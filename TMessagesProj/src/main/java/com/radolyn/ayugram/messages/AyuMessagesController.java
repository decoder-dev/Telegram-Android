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
import com.radolyn.ayugram.AyuUtils;
import com.radolyn.ayugram.database.AyuData;
import com.radolyn.ayugram.database.dao.DeletedMessageDao;
import com.radolyn.ayugram.database.dao.EditedMessageDao;
import com.radolyn.ayugram.database.entities.DeletedMessage;
import com.radolyn.ayugram.database.entities.DeletedMessageFull;
import com.radolyn.ayugram.database.entities.DeletedMessageReaction;
import com.radolyn.ayugram.database.entities.EditedMessage;
import com.radolyn.ayugram.proprietary.AyuMessageUtils;
import org.telegram.messenger.*;
import org.telegram.messenger.secretmedia.EncryptedFileInputStream;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AyuMessagesController {
    public static final String attachmentsSubfolder = "Saved Attachments";
    private static final File attachmentsPath = new File(
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

    public void onMessageEdited(TLRPC.Message oldMessage, TLRPC.Message newMessage, long userId, int accountId, int currentTime) {
        if (!AyuConfig.saveMessagesHistory) {
            return;
        }

        onMessageEditedInner(oldMessage, newMessage, userId, accountId, currentTime, false);
    }

    public void onMessageEditedForce(TLRPC.Message message, long userId, int accountId, int currentTime) {
        if (!AyuConfig.saveMessagesHistory) {
            return;
        }

        onMessageEditedInner(message, message, userId, accountId, currentTime, true);
    }

    private void onMessageEditedInner(TLRPC.Message oldMessage, TLRPC.Message newMessage, long userId, int accountId, int currentTime, boolean force) {
        boolean sameMedia = oldMessage.media == newMessage.media ||
                (oldMessage.media != null && newMessage.media != null && oldMessage.media.getClass() == newMessage.media.getClass());
        if (oldMessage.media instanceof TLRPC.TL_messageMediaPhoto && newMessage.media instanceof TLRPC.TL_messageMediaPhoto && oldMessage.media.photo != null && newMessage.media.photo != null) {
            sameMedia = oldMessage.media.photo.id == newMessage.media.photo.id;
        } else if (oldMessage.media instanceof TLRPC.TL_messageMediaDocument && newMessage.media instanceof TLRPC.TL_messageMediaDocument && oldMessage.media.document != null && newMessage.media.document != null) {
            sameMedia = oldMessage.media.document.id == newMessage.media.document.id;
        }

        var documentType = AyuConstants.DOCUMENT_TYPE_FILE;
        if (oldMessage.media instanceof TLRPC.TL_messageMediaPhoto && oldMessage.media.photo != null) {
            documentType = AyuConstants.DOCUMENT_TYPE_PHOTO;
        }

        if (force) {
            sameMedia = false;
        }

        if (sameMedia && TextUtils.equals(oldMessage.message, newMessage.message)) {
            return;
        }

        var dialogId = MessageObject.getDialogId(oldMessage);

        var attachPathFile = FileLoader.getInstance(accountId).getPathToMessage(oldMessage);

        if (!sameMedia && shouldSaveMedia(accountId, dialogId, oldMessage)) {
            try {
                attachPathFile = processAttachment(accountId, oldMessage);
            } catch (Exception e) {
                attachPathFile = new File("/");
                Log.e("AyuGram", "failed to save media");
            }
        }

        var attachPath = attachPathFile.getAbsolutePath();

        var revision = new EditedMessage();
        revision.mediaPath = attachPath.equals("/") ? null : attachPath;
        revision.documentType = documentType;

        var messageId = newMessage.id;

        if (!sameMedia && !TextUtils.isEmpty(revision.mediaPath) && editedMessageDao.isFirstRevisionWithChangedMedia(userId, dialogId, messageId)) {
            // update previous revisions to reflect media change
            // like, there's no previous file, so replace it with one we copied before...
            editedMessageDao.updateAttachmentForRevisionsBeforeDate(userId, dialogId, messageId, attachPath, currentTime);
        }

        revision.userId = userId;
        revision.dialogId = dialogId;
        revision.messageId = messageId;
        revision.text = oldMessage.message;
        revision.textEntities = AyuMessageUtils.serialize(oldMessage.entities);
        revision.editedDate = currentTime;

        editedMessageDao.insert(revision);
    }

    public void onMessageDeleted(TLRPC.Message msg, long userId, long dialogId, long topicId, int msgId, int accountId, int currentTime) {
        if (!AyuConfig.saveDeletedMessages) {
            return;
        }

        onMessageDeletedInner(msg, userId, dialogId, topicId, msgId, accountId, currentTime);
    }

    private void onMessageDeletedInner(TLRPC.Message msg, long userId, long dialogId, long topicId, int msgId, int accountId, int currentTime) {
        if (deletedMessageDao.exists(userId, dialogId, topicId, msgId)) {
            return;
        }

        var deletedMessage = new DeletedMessage();
        deletedMessage.userId = userId;
        deletedMessage.dialogId = dialogId;
        deletedMessage.messageId = msgId;
        deletedMessage.deletedDate = currentTime;

        Log.d("AyuGram", "saving message " + msgId + " for " + dialogId + " with topic " + topicId);

        if (msg != null) {
            Log.d("AyuGram", "saving message full");

            deletedMessage.text = msg.message;
            deletedMessage.textEntities = AyuMessageUtils.serialize(msg.entities);
            deletedMessage.date = msg.date;
            deletedMessage.flags = msg.flags;

            deletedMessage.peerId = MessageObject.getPeerId(msg.peer_id);
            deletedMessage.fromId = MessageObject.getPeerId(msg.from_id);
            deletedMessage.groupedId = msg.grouped_id;
            deletedMessage.topicId = topicId;

            deletedMessage.out = msg.out;
            deletedMessage.editDate = msg.edit_date;
            deletedMessage.editHide = msg.edit_hide;

            // --- media work

            if (shouldSaveMedia(accountId, dialogId, msg)) {
                if (msg.media == null) {
                    deletedMessage.documentType = AyuConstants.DOCUMENT_TYPE_NONE;
                } else if (msg.media instanceof TLRPC.TL_messageMediaPhoto && msg.media.photo != null) {
                    deletedMessage.documentType = AyuConstants.DOCUMENT_TYPE_PHOTO;
                } else if (msg.media instanceof TLRPC.TL_messageMediaDocument && msg.media.document != null && (MessageObject.isStickerMessage(msg) || msg.media.document.mime_type.equals("application/x-tgsticker"))) {
                    deletedMessage.documentType = AyuConstants.DOCUMENT_TYPE_STICKER;

                    try {
                        // телеграм полная хуйня, поэтому приходится сериализовать стикер вручную
                        NativeByteBuffer buffer = new NativeByteBuffer(msg.media.getObjectSize());
                        msg.media.serializeToStream(buffer);
                        buffer.reuse();
                        buffer.buffer.rewind();
                        byte[] arr = new byte[buffer.buffer.remaining()];
                        buffer.buffer.get(arr);

                        deletedMessage.documentSerialized = arr;
                    } catch (Exception e) {
                        Log.e("AyuGram", "fake news sticker", e);
                    }
                } else {
                    deletedMessage.documentType = AyuConstants.DOCUMENT_TYPE_FILE;
                }

                if (deletedMessage.documentType == AyuConstants.DOCUMENT_TYPE_PHOTO || deletedMessage.documentType == AyuConstants.DOCUMENT_TYPE_FILE) {
                    var attachPathFile = new File("/");

                    try {
                        attachPathFile = processAttachment(accountId, msg);
                    } catch (Exception e) {
                        Log.e("AyuGram", "failed to save media");
                    }
                    var attachPath = attachPathFile.getAbsolutePath();

                    deletedMessage.mediaPath = attachPath.equals("/") ? null : attachPath;
                }
            }
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

    private File processAttachment(int accountId, TLRPC.Message msg) {
        var attachPathFile = FileLoader.getInstance(accountId).getPathToMessage(msg);

        var f = AyuUtils.getFilename(msg, attachPathFile);
        var dest = new File(attachmentsPath, f);

        if (attachPathFile.exists()) {
            var success = AyuUtils.moveFile(attachPathFile, dest);

            if (success) {
                attachPathFile = new File(dest.getAbsolutePath());
            } else {
                attachPathFile = new File("/");
            }
        } else {
            var possibleEncrypted = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE), attachPathFile.getName() + ".enc");
            if (possibleEncrypted.exists()) {
                var keyFile = new File(FileLoader.getInternalCacheDir(), possibleEncrypted.getName() + ".key");

                Log.d("AyuGram", "key file " + keyFile.getAbsolutePath() + " exists " + keyFile.exists());

                if (keyFile.exists()) {
                    try {
                        try (var stream = new EncryptedFileInputStream(possibleEncrypted, keyFile)) {
                            try (var outStream = new FileOutputStream(dest)) {
                                var buffer = new byte[1024];
                                int read;
                                while ((read = stream.read(buffer)) != -1) {
                                    outStream.write(buffer, 0, read);
                                }

                                Log.d("AyuGram", "encrypted media copy success");

                                return dest;
                            }
                        }
                    } catch (Exception e) {
                        Log.e("AyuGram", "encrypted media copy failed", e);
                        return new File("/");
                    }
                }
            }

            attachPathFile = new File("/");
        }

        return attachPathFile;
    }

    private boolean shouldSaveMedia(int accountId, long dialogId, TLRPC.Message message) {
        if (!AyuConfig.saveMedia) {
            return false;
        }

        if (message.media == null) {
            return false;
        }

        if (DialogObject.isUserDialog(dialogId)) {
            return AyuConfig.saveMediaInPrivateChats;
        }

        var messagesController = MessagesController.getInstance(accountId);
        var chat = messagesController.getChat(dialogId);
        if (chat == null) {
            Log.e("AyuGram", "chat is null so saving media just in case");
            return true;
        }
        var isPublic = ChatObject.isPublic(chat);

        if (ChatObject.isChannel(chat)) {
            if (isPublic && AyuConfig.saveMediaInPublicChannels) {
                return true;
            } else return !isPublic && AyuConfig.saveMediaInPrivateChannels;
        }

        // then it's a group
        if (isPublic && AyuConfig.saveMediaInPublicGroups) {
            return true;
        } else return !isPublic && AyuConfig.saveMediaInPrivateGroups;
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

    public boolean isDeleted(long userId, long dialogId, int msgId) {
        return deletedMessageDao.isDeleted(userId, dialogId, msgId);
    }

    public boolean isDeleted(long userId, ArrayList<MessageObject> messageObjects) {
        var ids = messageObjects.stream().map(MessageObject::getId).collect(Collectors.toList());
        var dialogId = messageObjects.get(0).messageOwner.dialog_id;

        return deletedMessageDao.isDeleted(userId, dialogId, ids);
    }

    public void clean() {
        AyuData.clean();

        // force recreate a database to avoid crash
        instance = null;
    }
}
