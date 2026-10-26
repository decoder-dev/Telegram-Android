package org.telegram.messenger.banana;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.SQLite.SQLiteDatabase;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.banana.BananaConfig.Option;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;

/** Capture previous text before cache writes/deletes, then archive on a separate IO queue. */
public final class MessageHistory {
    private static final class Worker {
        static final DispatchQueue QUEUE = new DispatchQueue("banana-history");
        // Only touched by QUEUE; bounded even when account slots are reused.
        static final LinkedHashMap<String, HistoryDatabase> DATABASES = new LinkedHashMap<>(8, .75f, true);
    }

    private MessageHistory() {}

    public static void captureEdits(SQLiteDatabase source, int account, List<TLRPC.Message> messages) {
        if (!BananaConfig.get(account, Option.SAVE_EDITS) || messages == null) return;
        for (TLRPC.Message updated : messages) {
            if (updated == null || updated.id <= 0 || updated.edit_date <= 0) continue;
            SQLiteCursor cursor = null;
            try {
                long dialog = org.telegram.messenger.MessageObject.getDialogId(updated);
                if (dialog == 0 || DialogObject.isEncryptedDialog(dialog)) continue;
                cursor = source.queryFinalized("SELECT data FROM messages_v2 WHERE uid=? AND mid=?", dialog, updated.id);
                if (!cursor.next()) continue;
                TLRPC.Message previous = decode(cursor.byteArrayValue(0));
                if (previous == null || java.util.Objects.equals(previous.message, updated.message) &&
                        Arrays.equals(formatting(previous), formatting(updated))) continue;
                capture(source, account, dialog, previous, false);
            } catch (Exception error) {
                FileLog.e(error);
            } finally {
                if (cursor != null) cursor.dispose();
            }
        }
    }

    /** Called while the old row still exists, including ordinary cloud deletes without file cleanup. */
    public static void captureDeletes(SQLiteDatabase source, int account, long dialog, List<Integer> messageIds) {
        if (!BananaConfig.get(account, Option.SAVE_DELETED) || messageIds == null ||
                DialogObject.isEncryptedDialog(dialog)) return;
        // Parameterized bounded batches; no assumption that Telegram message IDs are contiguous.
        for (int start = 0; start < messageIds.size(); start += 100) {
            int end = Math.min(start + 100, messageIds.size());
            StringBuilder placeholders = new StringBuilder();
            ArrayList<Object> args = new ArrayList<>();
            for (int i = start; i < end; i++) {
                Integer id = messageIds.get(i);
                if (id == null || id <= 0) continue;
                if (placeholders.length() > 0) placeholders.append(',');
                placeholders.append('?');
                args.add(id);
            }
            if (args.isEmpty()) continue;
            String scope = dialog == 0 ? "is_channel=0" : "uid=?";
            if (dialog != 0) args.add(dialog);
            SQLiteCursor cursor = null;
            try {
                cursor = source.queryFinalized("SELECT uid,data FROM messages_v2 WHERE mid IN(" + placeholders + ") AND " + scope,
                        args.toArray());
                while (cursor.next()) capture(source, account, cursor.longValue(0), decode(cursor.byteArrayValue(1)), true);
            } catch (Exception error) {
                FileLog.e(error);
            } finally {
                if (cursor != null) cursor.dispose();
            }
        }
    }

    private static TLRPC.Message decode(byte[] blob) {
        if (blob == null || blob.length > 1024 * 1024) return null;
        SerializedData data = new SerializedData(blob);
        try {
            return TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
        } finally {
            data.cleanup();
        }
    }

    private static byte[] formatting(TLRPC.Message message) {
        if (message.entities == null || message.entities.isEmpty()) return new byte[0];
        if (message.entities.size() > 1024) throw new IllegalArgumentException("Too many history entities");
        SerializedData data = new SerializedData();
        try {
            data.writeInt32(message.entities.size());
            for (TLRPC.MessageEntity entity : message.entities) entity.serializeToStream(data);
            byte[] bytes = data.toByteArray();
            if (bytes.length > HistoryDatabase.MAX_CONTENT_BYTES) throw new IllegalArgumentException("History entities exceed storage limit");
            return bytes;
        } finally {
            data.cleanup();
        }
    }

    private static void capture(SQLiteDatabase source, int account, long dialog, TLRPC.Message message, boolean deleted) throws Exception {
        long owner = UserConfig.getInstance(account).getClientUserId();
        if (owner <= 0 || message == null || message.id <= 0 || dialog == 0 || DialogObject.isEncryptedDialog(dialog) ||
                message.action != null || message.ttl > 0 || message.ttl_period > 0 ||
                message.media != null && message.media.ttl_seconds > 0 ||
                message.message == null || message.message.isEmpty() || message.message.length() > 65536) return;
        long author = message.from_id == null ? 0 : DialogObject.getPeerDialogId(message.from_id);
        if (!BananaConfig.get(account, Option.SAVE_BOT_HISTORY)) {
            for (long userId : new long[]{author, dialog}) {
                if (userId > 0 && userId != owner && !isKnownHuman(source, account, userId)) return;
            }
        }

        long topic = message.reply_to != null && message.reply_to.forum_topic ?
                (message.reply_to.reply_to_top_id != 0 ? message.reply_to.reply_to_top_id : message.reply_to.reply_to_msg_id) : 0;
        HistoryDatabase.Record record = new HistoryDatabase.Record(dialog, message.id, author, topic, message.date,
                deleted, message.message, formatting(message));
        boolean test = ConnectionsManager.getInstance(account).isTestBackend();
        Worker.QUEUE.postRunnable(() -> {
            try { database(owner, test).append(record); }
            catch (Exception error) { FileLog.e(error); }
        });
    }

    private static boolean isKnownHuman(SQLiteDatabase source, int account, long userId) throws Exception {
        TLRPC.User user = MessagesController.getInstance(account).getUser(userId);
        if (user == null) {
            SQLiteCursor cursor = source.queryFinalized("SELECT data FROM users WHERE uid=?", userId);
            try {
                if (cursor.next()) {
                    byte[] blob = cursor.byteArrayValue(0);
                    if (blob != null) {
                        SerializedData data = new SerializedData(blob);
                        try { user = TLRPC.User.TLdeserialize(data, data.readInt32(false), false); }
                        finally { data.cleanup(); }
                    }
                }
            } finally { cursor.dispose(); }
        }
        // Do not accidentally archive an unknown bot, or our outgoing messages to one.
        return user != null && !user.bot;
    }

    private static HistoryDatabase database(long owner, boolean test) {
        String key = owner + ":" + test;
        HistoryDatabase db = Worker.DATABASES.get(key);
        if (db == null) {
            db = new HistoryDatabase(ApplicationLoader.applicationContext,
                    new File(ApplicationLoader.applicationContext.getNoBackupFilesDir(), "banana-history"), owner, test);
            Worker.DATABASES.put(key, db);
            if (Worker.DATABASES.size() > 4) {
                String oldest = Worker.DATABASES.keySet().iterator().next();
                Worker.DATABASES.remove(oldest).close();
            }
        }
        return db;
    }

    /** Null denotes an IO failure; an empty result is a successful query with no records. */
    public static void load(int account, String search, long dialog, Consumer<List<HistoryDatabase.Record>> callback) {
        long owner = UserConfig.getInstance(account).getClientUserId();
        boolean test = ConnectionsManager.getInstance(account).isTestBackend();
        Worker.QUEUE.postRunnable(() -> {
            List<HistoryDatabase.Record> result;
            try { result = owner <= 0 ? new ArrayList<>() : database(owner, test).recent(search, dialog, 100); }
            catch (Exception error) { FileLog.e(error); result = null; }
            List<HistoryDatabase.Record> answer = result;
            AndroidUtilities.runOnUIThread(() -> {
                if (owner == UserConfig.getInstance(account).getClientUserId() &&
                        test == ConnectionsManager.getInstance(account).isTestBackend()) callback.accept(answer);
            });
        });
    }

    public static void clear(int account, Consumer<Boolean> callback) {
        long owner = UserConfig.getInstance(account).getClientUserId();
        boolean test = ConnectionsManager.getInstance(account).isTestBackend();
        Worker.QUEUE.postRunnable(() -> {
            boolean success;
            try { if (owner > 0) database(owner, test).clearArchive(); success = true; }
            catch (Exception error) { FileLog.e(error); success = false; }
            boolean answer = success;
            AndroidUtilities.runOnUIThread(() -> {
                if (owner == UserConfig.getInstance(account).getClientUserId() &&
                        test == ConnectionsManager.getInstance(account).isTestBackend()) callback.accept(answer);
            });
        });
    }
}
