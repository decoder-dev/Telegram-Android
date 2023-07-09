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
import com.exteragram.messenger.ExteraConfig;
import com.google.android.exoplayer2.util.Log;
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
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class AyuMessagesController {
    public static final String attachmentsSubfolder = "Saved Attachments";
    public static final File attachmentsPath = new File(
            new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), AyuConstants.APP_NAME),
            attachmentsSubfolder
    );
    private static AyuMessagesController instance;
    private final EditedMessageDao editedMessageDao;
    private final DeletedMessageDao deletedMessageDao;

    private AyuMessagesController() {
        // recreate for testing if debug
        if (ExteraConfig.getLogging()) {
            ApplicationLoader.applicationContext.deleteDatabase(AyuConstants.AYU_DATABASE);
            if (attachmentsPath.exists()) {
                attachmentsPath.delete();
            }
        }

        if (!attachmentsPath.exists()) {
            attachmentsPath.mkdirs();
            try {
                new File(attachmentsPath, ".nomedia").createNewFile();
            } catch (IOException e) {
                // ignored, I hate java
            }
        }

        editedMessageDao = AyuData.getEditedMessageDao();
        deletedMessageDao = AyuData.getDeletedMessageDao();
    }

    public static AyuMessagesController getInstance() {
        if (instance == null) {
            instance = new AyuMessagesController();
        }
        return instance;
    }

    public void onMessageEdited(AyuSavePreferences prefs, TLRPC.Message newMessage) {
        if (!AyuConfig.saveMessagesHistory) {
            return;
        }

        try {
            onMessageEditedInner(prefs, newMessage, false);
        } catch (Exception e) {
            Log.e("AyuGram", "error onMessageEdited", e);
            FileLog.e("onMessageEdited", e);
        }
    }

    public void onMessageEditedForce(AyuSavePreferences prefs) {
        if (!AyuConfig.saveMessagesHistory) {
            return;
        }

        try {
            onMessageEditedInner(prefs, prefs.getMessage(), true);
        } catch (Exception e) {
            Log.e("AyuGram", "error onMessageEditedForce", e);
            FileLog.e("onMessageEditedForce", e);
        }
    }

    private void onMessageEditedInner(AyuSavePreferences prefs, TLRPC.Message newMessage, boolean force) {
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

        if (!sameMedia && !TextUtils.isEmpty(revision.mediaPath) && editedMessageDao.isFirstRevisionWithChangedMedia(prefs.getUserId(), prefs.getDialogId(), prefs.getMessageId())) {
            // update previous revisions to reflect media change
            // like, there's no previous file, so replace it with one we copied before...
            editedMessageDao.updateAttachmentForRevisionsBeforeDate(prefs.getUserId(), prefs.getDialogId(), prefs.getMessageId(), revision.mediaPath, prefs.getRequestCatchTime());
        }

        editedMessageDao.insert(revision);
    }

    public void onMessageDeleted(AyuSavePreferences prefs) {
        if (!AyuConfig.saveDeletedMessages) {
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

        if (msg != null) {
            Log.d("AyuGram", "saving message full");

            AyuMessageUtils.map(prefs, deletedMessage);
            AyuMessageUtils.mapMedia(prefs, deletedMessage, true);
        }

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

    public boolean hasAnyRevisions(long userId, long dialogId, int msgId) {
        return editedMessageDao.hasAnyRevisions(userId, dialogId, msgId);
    }

    public List<EditedMessage> getRevisions(long userId, long dialogId, int msgId) {
        return editedMessageDao.getAllRevisions(userId, dialogId, msgId);
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

    public void delete(long userId, long dialogId, int msgId) {
        var msg = getMessage(userId, dialogId, msgId);
        if (msg == null) {
            return;
        }

        deletedMessageDao.delete(userId, dialogId, msgId);

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

    public void clean() {
        AyuData.clean();
        AyuData.create();

        // force recreate a database to avoid crash
        instance = null;
    }
}
