package org.telegram.messenger.banana;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.SQLite.SQLiteDatabase;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.banana.BananaConfig.Option;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

/** Capture previous text before cache writes/deletes, then archive on a separate IO queue. */
public final class MessageHistory {
    private static final class Worker {
        static final DispatchQueue QUEUE = new DispatchQueue("banana-history");
        // Only touched by QUEUE; bounded even when account slots are reused.
        static final LinkedHashMap<String, HistoryDatabase> DATABASES = new LinkedHashMap<>(8, .75f, true);
    }
    private static final Semaphore pendingCopies = new Semaphore(64);

    private MessageHistory() {}

    /**
     * When PROACTIVE_SAVE_MEDIA + SAVE_MEDIA_HISTORY are on, start low-priority downloads
     * so attachments exist locally before a later edit/delete capture.
     */
    public static void proactiveDownloads(int account, List<TLRPC.Message> messages) {
        if (!BananaHooks.proactiveSaveMedia(account) || messages == null || messages.isEmpty()) return;
        long owner = UserConfig.getInstance(account).getClientUserId();
        if (owner <= 0) return;
        boolean test = ConnectionsManager.getInstance(account).isTestBackend();
        long generation = generation(account);
        int started = 0;
        for (TLRPC.Message message : messages) {
            long dialog = message == null ? 0 : org.telegram.messenger.MessageObject.getDialogId(message);
            if (message == null || message.id <= 0 || dialog == 0 || DialogObject.isEncryptedDialog(dialog) ||
                    message.media == null || message.ttl > 0 || message.ttl_period > 0 || message.media.ttl_seconds > 0) continue;
            HistoryDatabase.Attachment media = describeMedia(message);
            if (media == null) continue;
            Worker.QUEUE.postRunnable(() -> {
                if (generation == generation(account)) deferAttachment(account, owner, test, 0, message, media);
            });
            if (++started >= 8) return;
        }
    }

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
                        Arrays.equals(formatting(previous), formatting(updated)) &&
                        (!BananaConfig.get(account, Option.SAVE_MEDIA_HISTORY) ||
                                java.util.Objects.equals(mediaKey(previous), mediaKey(updated)))) continue;
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
                // Per row: these rows are deleted right after this returns, so one message that fails
                // to capture must not take the rest of the batch with it.
                while (cursor.next()) {
                    try {
                        capture(source, account, cursor.longValue(0), decode(cursor.byteArrayValue(1)), true);
                    } catch (Exception error) {
                        FileLog.e(error);
                    }
                }
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

    /** Formatting to archive; text whose entities exceed the limits is kept as plain text rather than lost. */
    private static byte[] storableFormatting(TLRPC.Message message) {
        try {
            return formatting(message);
        } catch (IllegalArgumentException tooLarge) {
            FileLog.e(tooLarge);
            return new byte[0];
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
                message.message != null && message.message.length() > 65536) return;
        boolean saveMedia = BananaConfig.get(account, Option.SAVE_MEDIA_HISTORY);
        HistoryDatabase.Attachment media = saveMedia ? describeMedia(message) : null;
        if ((message.message == null || message.message.isEmpty()) && media == null) return;
        long author = message.from_id == null ? 0 : DialogObject.getPeerDialogId(message.from_id);
        if (!BananaConfig.get(account, Option.SAVE_BOT_HISTORY)) {
            for (long userId : new long[]{author, dialog}) {
                if (userId > 0 && userId != owner && !isKnownHuman(source, account, userId)) return;
            }
        }

        long topic = message.reply_to != null && message.reply_to.forum_topic ?
                (message.reply_to.reply_to_top_id != 0 ? message.reply_to.reply_to_top_id : message.reply_to.reply_to_msg_id) : 0;
        HistoryDatabase.Record record = new HistoryDatabase.Record(dialog, message.id, author, topic, message.date,
                deleted, message.message, storableFormatting(message), media);
        boolean test = ConnectionsManager.getInstance(account).isTestBackend();
        AtomicLong rowId = new AtomicLong();
        Worker.QUEUE.postRunnable(() -> {
            try {
                HistoryDatabase db = database(owner, test);
                rowId.set(Math.abs(db.appendWithId(record)));
                File directory = attachmentDirectory(owner, test);
                if (rowId.get() != 0 && media != null && directory.isDirectory()) {
                    HistoryDatabase.Attachment retained = db.storedMedia(media.mediaKey);
                    if (retained != null) {
                        File existing = new SavedAttachments(directory).resolve(retained.storedName);
                        if (existing != null && existing.length() == retained.bytes) {
                            db.linkAttachment(rowId.get(), retained.storedName, retained.bytes);
                        }
                    }
                }
                if (directory.isDirectory()) new SavedAttachments(directory).prune(db.referencedAttachments(), System.currentTimeMillis() - 3600000);
            }
            catch (Exception error) { FileLog.e(error); }
        });
        if (media == null) return;
        if (!pendingCopies.tryAcquire()) return;
        // FileLoader.deleteFiles uses this same queue. Open before its later unlink;
        // copy on our IO queue so large files do not stall Telegram's download queue.
        FileLoader loader = FileLoader.getInstance(account);
        loader.getFileLoaderQueue().postRunnable(() -> {
            FileInputStream opened;
            long size;
            try {
                File file = loader.getPathToMessage(message);
                size = file.length();
                if (!file.isFile() || size == 0) {
                    pendingCopies.release();
                    if (BananaHooks.proactiveSaveMedia(account)) {
                        Worker.QUEUE.postRunnable(() -> deferAttachment(account, owner, test, rowId.get(), message, media));
                    }
                    return;
                }
                if (size > SavedAttachments.MAX_FILE_BYTES) { pendingCopies.release(); return; }
                opened = new FileInputStream(file);
            } catch (Exception error) { pendingCopies.release(); FileLog.e(error); return; }
            final long expectedSize = size;
            Worker.QUEUE.postRunnable(() -> {
                try (InputStream input = opened) {
                    if (rowId.get() == 0 || UserConfig.getInstance(account).getClientUserId() != owner ||
                            !BananaConfig.get(account, Option.SAVE_MEDIA_HISTORY) ||
                            !database(owner, test).canLinkAttachment(rowId.get())) return;
                    SavedAttachments attachments = new SavedAttachments(attachmentDirectory(owner, test));
                    String extension = media.displayName.contains(".") ? media.displayName.substring(media.displayName.lastIndexOf('.') + 1) : "bin";
                    File saved = attachments.save(input, extension, expectedSize);
                    if (!database(owner, test).linkAttachment(rowId.get(), saved.getName(), saved.length())) attachments.delete(saved.getName());
                } catch (Exception error) { FileLog.e(error); }
                finally { pendingCopies.release(); }
            });
        });
    }

    private static final java.util.concurrent.atomic.AtomicLongArray generations = new java.util.concurrent.atomic.AtomicLongArray(UserConfig.MAX_ACCOUNT_COUNT);
    private static long generation(int account) { return generations.get(account); }
    private static final java.util.Map<String, java.util.ArrayList<PendingAttachment>> pendingDownloads = new java.util.HashMap<>();
    private static final class PendingAttachment {
        final int account; final long owner, row, generation; final boolean test; final HistoryDatabase.Attachment media;
        PendingAttachment(int account, long owner, boolean test, long row, HistoryDatabase.Attachment media) {
            this.account = account; this.owner = owner; this.test = test; this.row = row; this.media = media; this.generation = generation(account);
        }
    }

    /** Worker queue: subscribe before starting, then recheck the local file to close the completion race. */
    private static void deferAttachment(int account, long owner, boolean test, long row, TLRPC.Message message,
                                        HistoryDatabase.Attachment media) {
        try {
            if (!BananaHooks.proactiveSaveMedia(account) ||
                    UserConfig.getInstance(account).getClientUserId() != owner ||
                    ConnectionsManager.getInstance(account).isTestBackend() != test ||
                    row != 0 && !database(owner, test).canLinkAttachment(row)) return;
            if (row == 0 && database(owner, test).storedMedia(media.mediaKey) != null) return;
            TLRPC.Document document = message.media != null ? message.media.document : null;
            TLRPC.Photo photo = message.media != null ? message.media.photo : null;
            TLRPC.PhotoSize size = photo == null ? null : FileLoader.getClosestPhotoSizeWithSize(photo.sizes, AndroidUtilities.getPhotoSize());
            if (document == null && size == null || document != null && document.size > SavedAttachments.MAX_FILE_BYTES ||
                    size != null && size.size > SavedAttachments.MAX_FILE_BYTES) return;
            String name = FileLoader.getAttachFileName(document != null ? document : size);
            String key = account + ":" + name;
            PendingAttachment pending = new PendingAttachment(account, owner, test, row, media);
            synchronized (pendingDownloads) {
                int count = 0;
                for (java.util.List<PendingAttachment> entries : pendingDownloads.values()) count += entries.size();
                if (count >= 64) return;
                java.util.ArrayList<PendingAttachment> entries = pendingDownloads.computeIfAbsent(key, ignored -> new java.util.ArrayList<>());
                for (PendingAttachment old : entries) if (old.owner == owner && old.row == row && old.test == test && old.generation == pending.generation) return;
                entries.add(pending);
            }
            Worker.QUEUE.postRunnable(() -> {
                synchronized (pendingDownloads) {
                    java.util.List<PendingAttachment> entries = pendingDownloads.get(key);
                    if (entries != null) { entries.remove(pending); if (entries.isEmpty()) pendingDownloads.remove(key); }
                }
            }, 10 * 60 * 1000);
            FileLoader loader = FileLoader.getInstance(account);
            File local = loader.getPathToMessage(message);
            if (local != null && local.isFile() && local.length() > 0) { onFileLoaded(account, name, local); return; }
            if (document != null) loader.loadFile(document, message, FileLoader.PRIORITY_LOW, 0);
            else loader.loadFile(org.telegram.messenger.ImageLocation.getForPhoto(size, photo), message, "jpg", FileLoader.PRIORITY_LOW, 0);
        } catch (Exception failure) { FileLog.e(failure); }
    }

    /** FileLoader completion path: open while the completed cache file still exists. */
    public static void onFileLoaded(int account, String name, File file) {
        java.util.List<PendingAttachment> entries;
        synchronized (pendingDownloads) { entries = pendingDownloads.remove(account + ":" + name); }
        if (entries == null) return;
        for (PendingAttachment pending : entries) {
            if (!pendingCopies.tryAcquire()) continue;
            try {
                long bytes = file.length();
                if (bytes <= 0 || bytes > SavedAttachments.MAX_FILE_BYTES) { pendingCopies.release(); continue; }
                InputStream input = new FileInputStream(file);
                Worker.QUEUE.postRunnable(() -> {
                    try (InputStream opened = input) {
                        if (UserConfig.getInstance(pending.account).getClientUserId() != pending.owner ||
                                ConnectionsManager.getInstance(pending.account).isTestBackend() != pending.test ||
                                !BananaHooks.proactiveSaveMedia(pending.account) || pending.generation != generation(pending.account) ||
                                pending.row != 0 && !database(pending.owner, pending.test).canLinkAttachment(pending.row)) return;
                        SavedAttachments store = new SavedAttachments(attachmentDirectory(pending.owner, pending.test));
                        HistoryDatabase.Attachment reused = database(pending.owner, pending.test).storedMedia(pending.media.mediaKey);
                        if (reused != null) {
                            File retained = store.resolve(reused.storedName);
                            if (retained != null && retained.isFile() && retained.length() == reused.bytes) {
                                if (pending.row != 0) database(pending.owner, pending.test).linkAttachment(pending.row, reused.storedName, reused.bytes);
                                return;
                            }
                        }
                        String display = pending.media.displayName;
                        String extension = display.contains(".") ? display.substring(display.lastIndexOf('.') + 1) : "bin";
                        File saved = store.save(opened, extension, bytes);
                        boolean linked = pending.row == 0 ? database(pending.owner, pending.test).rememberMedia(pending.media, saved.getName(), saved.length())
                                : database(pending.owner, pending.test).linkAttachment(pending.row, saved.getName(), saved.length());
                        if (!linked) store.delete(saved.getName());
                    } catch (Exception failure) { FileLog.e(failure); }
                    finally { pendingCopies.release(); }
                });
            } catch (Exception failure) { pendingCopies.release(); FileLog.e(failure); }
        }
    }

    public static void onFileFailed(int account, String name) {
        synchronized (pendingDownloads) { pendingDownloads.remove(account + ":" + name); }
    }

    private static String mediaKey(TLRPC.Message message) {
        HistoryDatabase.Attachment media = describeMedia(message);
        return media == null ? null : media.mediaKey;
    }

    private static HistoryDatabase.Attachment describeMedia(TLRPC.Message message) {
        if (message.media instanceof TLRPC.TL_messageMediaPhoto && message.media.photo != null) {
            return new HistoryDatabase.Attachment("photo:" + message.media.photo.id, 1, "image/jpeg", "photo.jpg");
        }
        if (message.media instanceof TLRPC.TL_messageMediaDocument && message.media.document != null) {
            TLRPC.Document document = message.media.document;
            int kind = MessageObject.isVideoDocument(document) ? 2 :
                    MessageObject.isVoiceDocument(document) || MessageObject.isMusicDocument(document) ? 3 : 4;
            return new HistoryDatabase.Attachment("document:" + document.id, kind, document.mime_type, FileLoader.getDocumentFileName(document));
        }
        return null;
    }

    private static File attachmentDirectory(long owner, boolean test) {
        return new File(new File(ApplicationLoader.applicationContext.getNoBackupFilesDir(), "banana-history/attachments"),
                owner + (test ? "-test" : "-production"));
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
        generations.incrementAndGet(account);
        synchronized (pendingDownloads) { pendingDownloads.entrySet().removeIf(e -> e.getKey().startsWith(account + ":")); }

        long owner = UserConfig.getInstance(account).getClientUserId();
        boolean test = ConnectionsManager.getInstance(account).isTestBackend();
        Worker.QUEUE.postRunnable(() -> {
            boolean success;
            try {
                if (owner > 0) {
                    database(owner, test).clearArchive();
                    File directory = attachmentDirectory(owner, test);
                    if (directory.isDirectory()) new SavedAttachments(directory).prune(java.util.Collections.emptySet(), Long.MAX_VALUE);
                }
                success = true;
            }
            catch (Exception error) { FileLog.e(error); success = false; }
            boolean answer = success;
            AndroidUtilities.runOnUIThread(() -> {
                if (owner == UserConfig.getInstance(account).getClientUserId() &&
                        test == ConnectionsManager.getInstance(account).isTestBackend()) callback.accept(answer);
            });
        });
    }

    public static void attachment(int account, HistoryDatabase.Record record, Consumer<File> callback) {
        long owner = UserConfig.getInstance(account).getClientUserId();
        boolean test = ConnectionsManager.getInstance(account).isTestBackend();
        Worker.QUEUE.postRunnable(() -> {
            File result = null;
            try {
                if (owner > 0 && record.attachment != null && record.attachment.storedName != null) {
                    result = new SavedAttachments(attachmentDirectory(owner, test)).resolve(record.attachment.storedName);
                }
            } catch (Exception error) { FileLog.e(error); }
            File answer = result;
            AndroidUtilities.runOnUIThread(() -> {
                if (owner == UserConfig.getInstance(account).getClientUserId() &&
                        test == ConnectionsManager.getInstance(account).isTestBackend()) callback.accept(answer);
            });
        });
    }
}
