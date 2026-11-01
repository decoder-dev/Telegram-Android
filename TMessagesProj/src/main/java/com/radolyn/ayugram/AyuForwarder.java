/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram;

import android.text.TextUtils;
import android.util.LongSparseArray;
import com.exteragram.messenger.utils.ChatUtils;
import android.util.Log;
import com.radolyn.ayugram.easy.AyuEasyUtils;
import com.radolyn.ayugram.easy.DummyMessageWaiter;
import org.telegram.messenger.*;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.util.ArrayList;

// music for coding on jaBBa
// https://open.spotify.com/track/2qpOzuQGFqTKNn56w7qShx
public class AyuForwarder {
    public static boolean isFullAyuForwardsNeeded(int currentAccount, ArrayList<MessageObject> messages) {
        if (messages == null || messages.isEmpty()) {
            return false;
        }
        MessageObject first = messages.get(0);
        if (first == null || first.messageOwner == null) return false;
        var dialogId = first.getDialogId();
        if (dialogId >= 0) return false;
        var chat = MessagesController.getInstance(currentAccount).getChat(-dialogId);

        return chat != null && chat.ayuNoforwards;
    }

    public static boolean isAyuForwardNeeded(ArrayList<MessageObject> messages) {
        if (messages == null || messages.isEmpty()) {
            return false;
        }
        for (var message : messages) {
            if (isAyuForwardNeeded(message)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isAyuForwardNeeded(MessageObject message) {
        return message != null && message.messageOwner != null && (message.messageOwner.ayuDeleted || message.messageOwner.ayuNoforwards);
    }

    public static void intelligentForward(int currentAccount, ArrayList<MessageObject> messages, long peer, boolean forwardFromMyName, boolean hideCaption, boolean notify, int scheduleDate, MessageObject replyToTopMsg) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        var batches = new ArrayList<ForwardBatch>();

        var currentArray = new ArrayList<MessageObject>();
        var currentBatch = new ForwardBatch(isAyuForwardNeeded(messages.get(0)), currentArray);

        for (var message : messages) {
            if (isAyuForwardNeeded(message) != currentBatch.isAyuForwardNeeded) {
                batches.add(currentBatch);

                currentArray = new ArrayList<>();
                currentBatch = new ForwardBatch(isAyuForwardNeeded(message), currentArray);
            }

            currentArray.add(message);
        }

        batches.add(currentBatch);

        for (var batch : batches) {
            if (batch.isAyuForwardNeeded) {
                forwardMessages(currentAccount, batch.messages, peer, forwardFromMyName, hideCaption, notify, scheduleDate, replyToTopMsg);
            } else {
                // use default forward, but wait for it
                AyuEasyUtils.forwardMessagesSync(currentAccount, batch.messages, peer, forwardFromMyName, hideCaption, notify, scheduleDate, replyToTopMsg);
            }
        }
    }

    public static void forwardMessages(int currentAccount, ArrayList<MessageObject> messages, long peer, boolean forwardFromMyName, boolean hideCaption, boolean notify, int scheduleDate, MessageObject replyToTopMsg) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        var toBeDownloaded = new ArrayList<MessageObject>();

        var groups = new LongSparseArray<Long>();
        var groupIds = new LongSparseArray<Long>();

        // todo: replies

        var fullNoforwards = isFullAyuForwardsNeeded(currentAccount, messages);

        for (var message : messages) {
            if (message == null || message.messageOwner == null ||
                    (!MessageObject.isMediaEmpty(message.messageOwner) || message.getGroupId() != 0) &&
                            !AyuUtils.isMediaDownloadable(message, false)) {
                throw new IllegalArgumentException("Unsupported forward media");
            }
            if (fullNoforwards || message.messageOwner.ayuNoforwards || message.messageOwner.ayuDeleted) {
                if (AyuUtils.isMediaDownloadable(message, false)) {
                    toBeDownloaded.add(message);
                }
            }

            if (message.getGroupId() != 0) {
                var currentCount = groups.get(message.getGroupId());
                groups.put(message.getGroupId(), currentCount == null ? 1 : currentCount + 1);

                var currentId = groupIds.get(message.getGroupId());
                if (currentId == null) {
                    groupIds.put(message.getGroupId(), Utilities.random.nextLong());
                }
            }
        }

        if (!toBeDownloaded.isEmpty()) {
            AyuEasyUtils.loadDocumentsSync(currentAccount, toBeDownloaded);
        }

        // Validate the entire batch before uploading its first item. A failed/expired download
        // must not create an empty document, crash photo decoding or leave an unfinished album.
        var mediaPaths = new java.util.IdentityHashMap<MessageObject, File>();
        var photos = new java.util.IdentityHashMap<MessageObject, TLRPC.TL_photo>();
        for (MessageObject message : messages) {
            if (AyuUtils.isMediaDownloadable(message, false)) {
                File path = AyuUtils.getLocalMessageFile(message);
                if (path == null) throw new IllegalStateException("Forward media is not available locally");
                mediaPaths.put(message, path);
                if (message.isPhoto()) photos.put(message, mapPhoto(currentAccount, MessageObject.getPhoto(message.messageOwner), path));
            }
        }

        var albumWaiters = new LongSparseArray<DummyMessageWaiter>();
        try {
            for (int i = 0; i < groups.size(); i++) {
                DummyMessageWaiter waiter = new DummyMessageWaiter(currentAccount, groups.valueAt(i).intValue());
                albumWaiters.put(groups.keyAt(i), waiter);
                waiter.subscribe();
            }
            for (var message : messages) {
                var textExtracted = ChatUtils.getMessageText(message, null);
                var text = textExtracted == null ? "" : textExtracted.toString();
                var mediaDownloadable = AyuUtils.isMediaDownloadable(message, false);
                var groupId = message.getGroupId();
                var newGroupId = groupIds.get(groupId, 0L);

                var isFinalInGroup = false;
                if (groupId != 0) {
                    groups.put(groupId, groups.get(groupId) - 1);
                    isFinalInGroup = groups.get(groupId) == 0;
                }

                var messagePath = mediaPaths.get(message);

                if (TextUtils.isEmpty(text) && !mediaDownloadable) {
                    continue;
                }

                if (!mediaDownloadable) {
                    AyuEasyUtils.sendTextMessageSync(
                            currentAccount,
                            text,
                            peer,
                            null,
                            replyToTopMsg,
                            null, // todo
                            false,
                            message.messageOwner.entities,
                            notify,
                            scheduleDate
                    );
                } else if (message.getDocument() != null) {
                    var doc = message.getDocument();
                    var newDoc = mapDocument(currentAccount, doc, messagePath);
                    var caption = hideCaption ? "" : text;
                    var entities = hideCaption ? null : message.messageOwner.entities;

                    AyuEasyUtils.sendDocumentMessageSync(
                            currentAccount,
                            newDoc,
                            messagePath.getAbsolutePath(),
                            peer,
                            null,
                            replyToTopMsg,
                            caption,
                            entities,
                            notify,
                            scheduleDate,
                            newGroupId,
                            isFinalInGroup,
                            albumWaiters.get(groupId)
                    );
                } else if (message.isPhoto()) {
                    var photo = MessageObject.getPhoto(message.messageOwner);
                    var newPhoto = photos.get(message);

                    var caption = hideCaption ? "" : (TextUtils.isEmpty(photo.caption) ? text : photo.caption);
                    var entities = hideCaption ? null : message.messageOwner.entities;

                    AyuEasyUtils.sendPhotoMessageSync(
                            currentAccount,
                            newPhoto,
                            messagePath.getAbsolutePath(),
                            peer,
                            null,
                            replyToTopMsg,
                            caption,
                            entities,
                            notify,
                            scheduleDate,
                            newGroupId,
                            isFinalInGroup,
                            albumWaiters.get(groupId)
                    );
                } else {
                    Log.w("AyuGram", "Unsupported message type: " + message.messageOwner);
                }

                Log.w("AyuGram", "Message forwarded");
            }

        } finally {
            for (int i = 0; i < albumWaiters.size(); i++) albumWaiters.valueAt(i).close();
        }
        Log.w("AyuGram", "All messages forwarded");
    }

    private static TLRPC.TL_document mapDocument(int currentAccount, TLRPC.Document doc, File messagePath) {
        var newDoc = new TLRPC.TL_document();
        newDoc.file_reference = new byte[0];
        newDoc.dc_id = Integer.MIN_VALUE;
        newDoc.user_id = doc.user_id;
        newDoc.mime_type = doc.mime_type;
        newDoc.file_name = doc.file_name;
        newDoc.file_name_fixed = doc.file_name_fixed;
        newDoc.date = AccountInstance.getInstance(currentAccount).getConnectionsManager().getCurrentTime();
        newDoc.size = messagePath.length();

        newDoc.attributes = new ArrayList<>(doc.attributes);
        newDoc.localPath = messagePath.getAbsolutePath();

        return newDoc;
    }

    private static TLRPC.TL_photo mapPhoto(int currentAccount, TLRPC.Photo photo, File messagePath) {
        var helper = SendMessagesHelper.getInstance(currentAccount);

        var newPhoto = helper.generatePhotoSizes(messagePath.getAbsolutePath(), null); // todo: mapPhoto
        if (newPhoto == null) throw new IllegalStateException("Forward photo could not be decoded");
        newPhoto.flags = photo.flags;
        newPhoto.has_stickers = photo.has_stickers;
        newPhoto.date = AccountInstance.getInstance(currentAccount).getConnectionsManager().getCurrentTime();
        newPhoto.geo = photo.geo;
        newPhoto.caption = photo.caption;

        return newPhoto;
    }

    private static class ForwardBatch {
        public final boolean isAyuForwardNeeded;
        public final ArrayList<MessageObject> messages;

        public ForwardBatch(boolean isAyuForwardNeeded, ArrayList<MessageObject> messages) {
            this.isAyuForwardNeeded = isAyuForwardNeeded;
            this.messages = messages;
        }
    }
}
