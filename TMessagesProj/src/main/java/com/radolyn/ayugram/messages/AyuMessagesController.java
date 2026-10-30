/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.messages;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import com.radolyn.ayugram.AyuConfig;
import com.radolyn.ayugram.AyuConstants;
import com.radolyn.ayugram.database.AyuData;
import com.radolyn.ayugram.database.dao.DeletedMessageDao;
import com.radolyn.ayugram.database.dao.EditedMessageDao;
import com.radolyn.ayugram.database.entities.DeletedMessage;
import com.radolyn.ayugram.database.entities.DeletedMessageFull;
import com.radolyn.ayugram.database.entities.DeletedMessageReaction;
import com.radolyn.ayugram.database.entities.EditedMessage;
import com.radolyn.ayugram.proprietary.AyuMessageUtils;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.NotificationCenter;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class AyuMessagesController {
    public static final String attachmentsSubfolder = "Saved Attachments";
    // App-private: saved media of deleted and edited messages, including secret-chat and
    // self-destructing media, used to sit in public Downloads where any app could read it and
    // where clearing the archive or logging out left it behind.
    public static final File attachmentsPath = new File(
            new File(org.telegram.messenger.ApplicationLoader.applicationContext.getNoBackupFilesDir(), "ayu-attachments"),
            attachmentsSubfolder
    );
    private static final File legacyAttachmentsPath = new File(
            new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), AyuConstants.APP_NAME),
            attachmentsSubfolder
    );
    private static AyuMessagesController instance;
    private final EditedMessageDao editedMessageDao;
    private final DeletedMessageDao deletedMessageDao;

    private AyuMessagesController() {
        initializeAttachmentsFolder();

        editedMessageDao = AyuData.getEditedMessageDao();
        deletedMessageDao = AyuData.getDeletedMessageDao();
    }

    private static void initializeAttachmentsFolder() {
        attachmentsPath.mkdirs();
        try {
            new File(attachmentsPath, ".nomedia").createNewFile();
        } catch (IOException e) {
            // ignored, I hate java
        }
        if (legacyAttachmentsPath.isDirectory()) {
            new Thread(AyuMessagesController::migrateLegacyAttachments, "ayu-attachments-migration").start();
        }
    }

    public static synchronized AyuMessagesController getInstance() {
        if (instance == null) {
            instance = new AyuMessagesController();
        }
        return instance;
    }

    public void onMessageEdited(AyuSavePreferences prefs, TLRPC.Message newMessage) {
        try {
            onMessageEditedInner(prefs, newMessage, false);
        } catch (Exception e) {
            Log.e("AyuGram", "error onMessageEdited", e);
            FileLog.e("onMessageEdited", e);
        }
    }

    public void onMessageEditedForce(AyuSavePreferences prefs) {
        try {
            onMessageEditedInner(prefs, prefs.getMessage(), true);
        } catch (Exception e) {
            Log.e("AyuGram", "error onMessageEditedForce", e);
            FileLog.e("onMessageEditedForce", e);
        }
    }

    private void onMessageEditedInner(AyuSavePreferences prefs, TLRPC.Message newMessage, boolean force) {
        if (!AyuConfig.saveEditedMessageFor(prefs.getAccountId(), prefs.getDialogId())) {
            return;
        }

        var oldMessage = prefs.getMessage();

        boolean sameMedia = oldMessage.media == newMessage.media ||
                (oldMessage.media != null && newMessage.media != null && oldMessage.media.getClass() == newMessage.media.getClass());
        if (oldMessage.media instanceof TLRPC.TL_messageMediaPhoto && newMessage.media instanceof TLRPC.TL_messageMediaPhoto && oldMessage.media.photo != null && newMessage.media.photo != null) {
            sameMedia = oldMessage.media.photo.id == newMessage.media.photo.id;
        } else if (oldMessage.media instanceof TLRPC.TL_messageMediaDocument && newMessage.media instanceof TLRPC.TL_messageMediaDocument && oldMessage.media.document != null && newMessage.media.document != null) {
            sameMedia = oldMessage.media.document.id == newMessage.media.document.id;
        }

        if (force) {
            sameMedia = false;
        }

        if (sameMedia && TextUtils.equals(oldMessage.message, newMessage.message)) {
            return;
        }

        var revision = new EditedMessage();
        AyuMessageUtils.map(prefs, revision);
        AyuMessageUtils.mapMedia(prefs, revision, !sameMedia);

        if (!sameMedia && !TextUtils.isEmpty(revision.mediaPath)) {
            var lastRevision = editedMessageDao.getLastRevision(prefs.getUserId(), prefs.getDialogId(), prefs.getMessageId());

            if (lastRevision != null && !TextUtils.equals(revision.mediaPath, lastRevision.mediaPath) && lastRevision.mediaPath != null && !lastRevision.mediaPath.contains(attachmentsSubfolder)) {
                // update previous revisions to reflect media change
                // like, there's no previous file, so replace it with one we copied before...
                editedMessageDao.updateAttachmentForRevisionsBetweenDates(prefs.getUserId(), prefs.getDialogId(), prefs.getMessageId(), lastRevision.mediaPath, revision.mediaPath);
            }
        }

        editedMessageDao.insert(revision);

        AndroidUtilities.runOnUIThread(() -> {
            NotificationCenter.getInstance(prefs.getAccountId()).postNotificationName(AyuConstants.MESSAGE_EDITED_NOTIFICATION, prefs.getDialogId(), prefs.getMessageId());
        });
    }

    public void onMessageDeleted(AyuSavePreferences prefs) {
        if (prefs.getMessage() == null) {
            Log.w("AyuGram", "null msg ?");
            return;
        }

        try {
            onMessageDeletedInner(prefs);
        } catch (Exception e) {
            Log.e("AyuGram", "error onMessageDeleted", e);
            FileLog.e("onMessageDeleted", e);
        }
    }

    private void onMessageDeletedInner(AyuSavePreferences prefs) {
        if (!AyuConfig.saveDeletedMessageFor(prefs.getAccountId(), prefs.getDialogId())) {
            return;
        }

        if (deletedMessageDao.exists(prefs.getUserId(), prefs.getDialogId(), prefs.getTopicId(), prefs.getMessageId())) {
            return;
        }

        var deletedMessage = new DeletedMessage();
        deletedMessage.userId = prefs.getUserId();
        deletedMessage.dialogId = prefs.getDialogId();
        deletedMessage.messageId = prefs.getMessageId();
        deletedMessage.entityCreateDate = prefs.getRequestCatchTime();

        var msg = prefs.getMessage();

        Log.d("AyuGram", "saving message " + prefs.getMessageId() + " for " + prefs.getDialogId() + " with topic " + prefs.getTopicId());

        AyuMessageUtils.map(prefs, deletedMessage);
        AyuMessageUtils.mapMedia(prefs, deletedMessage, true);

        var fakeMsgId = deletedMessageDao.insert(deletedMessage);

        if (msg != null && msg.reactions != null && AyuConfig.saveReactions) {
            processDeletedReactions(fakeMsgId, msg.reactions);
        }
    }

    private void processDeletedReactions(long fakeMessageId, TLRPC.TL_messageReactions reactions) {
        for (var reaction : reactions.results) {
            if (reaction.reaction instanceof TLRPC.TL_reactionEmpty) {
                continue;
            }

            var deletedReaction = new DeletedMessageReaction();
            deletedReaction.deletedMessageId = fakeMessageId;
            deletedReaction.count = reaction.count;
            deletedReaction.selfSelected = reaction.chosen;

            if (reaction.reaction instanceof TLRPC.TL_reactionEmoji) {
                deletedReaction.emoticon = ((TLRPC.TL_reactionEmoji) reaction.reaction).emoticon;
            } else if (reaction.reaction instanceof TLRPC.TL_reactionCustomEmoji) {
                deletedReaction.documentId = ((TLRPC.TL_reactionCustomEmoji) reaction.reaction).document_id;
                deletedReaction.isCustom = true;
            } else {
                Log.e("AyuGram", "fake news emoji");
                continue;
            }

            deletedMessageDao.insertReaction(deletedReaction);
        }
    }

    public boolean hasAnyRevisions(long userId, long dialogId, int messageId) {
        return editedMessageDao.hasAnyRevisions(userId, dialogId, messageId);
    }

    public List<EditedMessage> getRevisions(long userId, long dialogId, int messageId) {
        return editedMessageDao.getAllRevisions(userId, dialogId, messageId);
    }

    public DeletedMessageFull getMessage(long userId, long dialogId, int messageId) {
        return deletedMessageDao.getMessage(userId, dialogId, messageId);
    }

    public List<DeletedMessageFull> getMessages(long userId, long dialogId, long topicId, int startId, int endId, int limit) {
        return deletedMessageDao.getMessages(userId, dialogId, topicId, startId, endId, limit);
    }

    public List<DeletedMessageFull> getMessagesGrouped(long userId, long dialogId, long groupedId) {
        return deletedMessageDao.getMessagesGrouped(userId, dialogId, groupedId);
    }

    public void delete(long userId, long dialogId, int messageId) {
        var msg = getMessage(userId, dialogId, messageId);
        if (msg == null) {
            return;
        }

        deletedMessageDao.delete(userId, dialogId, messageId);

        if (!TextUtils.isEmpty(msg.message.mediaPath)) {
            var p = new File(msg.message.mediaPath);
            if (p.exists()) {
                try {
                    p.delete();
                } catch (Exception e) {
                    Log.e("AyuGram", "failed to delete file " + msg.message.mediaPath, e);
                }
            }
        }
    }

    /** Copies each public file into the private folder, repoints the archive rows, then deletes it. */
    private static synchronized void migrateLegacyAttachments() {
        File[] files = legacyAttachmentsPath.listFiles();
        if (files == null) {
            return;
        }
        try {
            androidx.sqlite.db.SupportSQLiteDatabase db = AyuData.getDatabase().getOpenHelper().getWritableDatabase();
            for (File file : files) {
                if (!file.isFile() || ".nomedia".equals(file.getName())) {
                    continue;
                }
                try {
                    File target = new File(attachmentsPath, file.getName());
                    if (!(target.isFile() && target.length() == file.length()) && !AndroidUtilities.copyFile(file, target)) {
                        continue;
                    }
                    Object[] paths = {target.getAbsolutePath(), file.getAbsolutePath()};
                    for (String table : new String[]{"DeletedMessage", "EditedMessage"}) {
                        db.execSQL("UPDATE `" + table + "` SET mediaPath = ? WHERE mediaPath = ?", paths);
                        db.execSQL("UPDATE `" + table + "` SET hqThumbPath = ? WHERE hqThumbPath = ?", paths);
                    }
                    file.delete();
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
            new File(legacyAttachmentsPath, ".nomedia").delete();
            legacyAttachmentsPath.delete();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private static void deleteAttachments(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && !".nomedia".equals(file.getName())) file.delete();
            }
        }
    }

    private static boolean isArchiveFile(String path) {
        return path != null && (path.startsWith(attachmentsPath.getAbsolutePath() + "/") || path.startsWith(legacyAttachmentsPath.getAbsolutePath() + "/"));
    }

    /**
     * Logging an account out removes what the archive kept for it: its deleted and edited message
     * rows, their reactions and every saved file no other account's rows still use.
     */
    public static void clearForUser(long userId) {
        if (userId <= 0) {
            return;
        }
        new Thread(() -> {
            try {
                androidx.sqlite.db.SupportSQLiteDatabase db = AyuData.getDatabase().getOpenHelper().getWritableDatabase();
                java.util.HashSet<String> paths = new java.util.HashSet<>();
                for (String table : new String[]{"DeletedMessage", "EditedMessage"}) {
                    try (android.database.Cursor cursor = db.query("SELECT mediaPath, hqThumbPath FROM `" + table + "` WHERE userId = ?", new Object[]{userId})) {
                        while (cursor.moveToNext()) {
                            if (!cursor.isNull(0)) paths.add(cursor.getString(0));
                            if (!cursor.isNull(1)) paths.add(cursor.getString(1));
                        }
                    }
                }
                db.beginTransaction();
                try {
                    db.execSQL("DELETE FROM `DeletedMessageReaction` WHERE deletedMessageId IN (SELECT fakeId FROM `DeletedMessage` WHERE userId = ?)", new Object[]{userId});
                    db.execSQL("DELETE FROM `DeletedMessage` WHERE userId = ?", new Object[]{userId});
                    db.execSQL("DELETE FROM `EditedMessage` WHERE userId = ?", new Object[]{userId});
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                for (String path : paths) {
                    if (!isArchiveFile(path)) continue;
                    boolean used = false;
                    for (String table : new String[]{"DeletedMessage", "EditedMessage"}) {
                        try (android.database.Cursor cursor = db.query("SELECT 1 FROM `" + table + "` WHERE mediaPath = ? OR hqThumbPath = ? LIMIT 1", new Object[]{path, path})) {
                            used |= cursor.moveToFirst();
                        }
                    }
                    if (!used) new File(path).delete();
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }, "ayu-archive-logout").start();
    }

    public void clean() {
        AyuData.clean();
        AyuData.create();
        // Clearing the archive removes its files too; they used to stay in Downloads.
        deleteAttachments(attachmentsPath);
        deleteAttachments(legacyAttachmentsPath);

        // force to recreate a database to avoid crash
        instance = null;
    }
}
