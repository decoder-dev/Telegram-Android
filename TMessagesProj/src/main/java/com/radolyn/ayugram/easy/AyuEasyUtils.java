/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.easy;

import org.telegram.messenger.*;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.HashMap;

public class AyuEasyUtils {
    public static void loadDocumentsSync(int currentAccount, ArrayList<MessageObject> toBeDownloaded) {
        try (var waiter = new DummyFileLoadWaiter(currentAccount, toBeDownloaded)) {
            waiter.subscribe();

            var fileLoader = FileLoader.getInstance(currentAccount);

            for (var message : toBeDownloaded) {
                if (com.radolyn.ayugram.AyuUtils.getLocalMessageFile(message) != null) continue;
                var doc = message.getDocument();
                var photo = MessageObject.getPhoto(message.messageOwner);

                if (doc != null) {
                    fileLoader.loadFile(doc, message, FileLoader.PRIORITY_HIGH, 0);
                } else if (photo != null) {
                    var path = ImageLocation.getForPhoto(FileLoader.getClosestPhotoSizeWithSize(photo.sizes, AndroidUtilities.getPhotoSize()), photo);
                    fileLoader.loadFile(path, message, null, FileLoader.PRIORITY_HIGH, 0);
                }
            }

            waiter.await();
        }
    }

    public static void sendTextMessageSync(
            int currentAccount,
            String message,
            long peer,
            MessageObject replyToMsg,
            MessageObject replyToTopMsg,
            TLRPC.WebPage webPage,
            boolean searchLinks,
            ArrayList<TLRPC.MessageEntity> entities,
            boolean notify,
            int scheduleDate
    ) {
        try (var waiter = new DummyMessageWaiter(currentAccount)) {
            waiter.subscribe();

            var sendParams = SendMessagesHelper.SendMessageParams.of(
                    message,
                    peer,
                    replyToMsg,
                    replyToTopMsg,
                    webPage,
                    searchLinks,
                    entities,
                    null,
                    null,
                    notify,
                    scheduleDate,
                    0,
                    null,
                    false
            );
            postSend(currentAccount, sendParams, waiter);

            waiter.await();
        }
    }

    public static void sendDocumentMessageSync(
            int currentAccount,
            TLRPC.TL_document document,
            String path,
            long peer,
            MessageObject replyToMsg,
            MessageObject replyToTopMsg,
            String caption,
            ArrayList<TLRPC.MessageEntity> entities,
            boolean notify,
            int scheduleDate,
            Long groupId,
            boolean isFinalInGroup
    ) {
        sendDocumentMessageSync(currentAccount, document, path, peer, replyToMsg, replyToTopMsg, caption,
                entities, notify, scheduleDate, groupId, isFinalInGroup, null);
    }

    public static void sendDocumentMessageSync(
            int currentAccount,
            TLRPC.TL_document document,
            String path,
            long peer,
            MessageObject replyToMsg,
            MessageObject replyToTopMsg,
            String caption,
            ArrayList<TLRPC.MessageEntity> entities,
            boolean notify,
            int scheduleDate,
            Long groupId,
            boolean isFinalInGroup,
            DummyMessageWaiter batchWaiter
    ) {
        var waitToSend = groupId == 0 || isFinalInGroup;
        var sendWaiter = new DummyMessageWaiter(currentAccount, 1, batchWaiter == null && waitToSend, batchWaiter);
        var uploadWaiter = new DummyFileUploadWaiter(currentAccount, path);
        try (sendWaiter; uploadWaiter) {
            sendWaiter.subscribe();

            if (groupId == 0) uploadWaiter.subscribe();

            var params = createParams(groupId, isFinalInGroup);

            var sendParams = SendMessagesHelper.SendMessageParams.of(
                    document,
                    null,
                    path,
                    peer,
                    replyToMsg,
                    replyToTopMsg,
                    caption,
                    entities,
                    null,
                    params,
                    notify,
                    scheduleDate,
                    0,
                    0,
                    null,
                    null,
                    false
            );
            postSend(currentAccount, sendParams, sendWaiter);

            if (groupId == 0) {
                uploadWaiter.await();
            }

            sendWaiter.await();
            if (batchWaiter != null && isFinalInGroup) batchWaiter.await();
        }
    }

    public static void sendPhotoMessageSync(
            int currentAccount,
            TLRPC.TL_photo photo,
            String path,
            long peer,
            MessageObject replyToMsg,
            MessageObject replyToTopMsg,
            String caption,
            ArrayList<TLRPC.MessageEntity> entities,
            boolean notify,
            int scheduleDate,
            Long groupId,
            boolean isFinalInGroup
    ) {
        sendPhotoMessageSync(currentAccount, photo, path, peer, replyToMsg, replyToTopMsg, caption,
                entities, notify, scheduleDate, groupId, isFinalInGroup, null);
    }

    public static void sendPhotoMessageSync(
            int currentAccount,
            TLRPC.TL_photo photo,
            String path,
            long peer,
            MessageObject replyToMsg,
            MessageObject replyToTopMsg,
            String caption,
            ArrayList<TLRPC.MessageEntity> entities,
            boolean notify,
            int scheduleDate,
            Long groupId,
            boolean isFinalInGroup,
            DummyMessageWaiter batchWaiter
    ) {
        var bigPhoto = FileLoader.getClosestPhotoSizeWithSize(photo.sizes, AndroidUtilities.getPhotoSize());
        var filename = FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE) + "/" + bigPhoto.location.volume_id + "_" + bigPhoto.location.local_id + ".jpg";

        var waitToSend = groupId == 0 || isFinalInGroup;
        var sendWaiter = new DummyMessageWaiter(currentAccount, 1, batchWaiter == null && waitToSend, batchWaiter);
        var uploadWaiter = new DummyFileUploadWaiter(currentAccount, filename);
        try (sendWaiter; uploadWaiter) {
            sendWaiter.subscribe();

            if (groupId == 0) uploadWaiter.subscribe();

            var params = createParams(groupId, isFinalInGroup);

            var sendParams = SendMessagesHelper.SendMessageParams.of(
                    photo,
                    path,
                    peer,
                    replyToMsg,
                    replyToTopMsg,
                    caption,
                    entities,
                    null,
                    params,
                    notify,
                    scheduleDate,
                    0,
                    0,
                    null,
                    false,
                    false
            );
            postSend(currentAccount, sendParams, sendWaiter);

            if (groupId == 0) {
                uploadWaiter.await();
            }

            sendWaiter.await();
            if (batchWaiter != null && isFinalInGroup) batchWaiter.await();
        }
    }

    public static void forwardMessagesSync(
            int currentAccount,
            ArrayList<MessageObject> messages,
            final long peer,
            boolean forwardFromMyName,
            boolean hideCaption,
            boolean notify,
            int scheduleDate,
            MessageObject replyToTopMsg
    ) {
        try (var waiter = new DummyMessageWaiter(currentAccount, messages.size())) {
            waiter.subscribe();

            postTransfer(waiter, () -> {
                int error = SendMessagesHelper.getInstance(currentAccount).sendMessage(messages, peer, forwardFromMyName,
                        hideCaption, notify, scheduleDate, 0, replyToTopMsg, -1, 0, 0, null, waiter);
                if (error != 0) waiter.onError();
            });

            waiter.await();
        }
    }


    private static void postSend(int account, SendMessagesHelper.SendMessageParams params, DummyMessageWaiter waiter) {
        params.bananaSendObserver = waiter;
        postTransfer(waiter, () -> SendMessagesHelper.getInstance(account).sendMessage(params));
    }

    private static void postTransfer(DummyMessageWaiter waiter, Runnable send) {
        AndroidUtilities.runOnUIThread(() -> {
            if (!waiter.isActive()) return;
            try { send.run(); }
            catch (RuntimeException failure) {
                waiter.onError();
                FileLog.e("Copy send failed: " + failure.getClass().getSimpleName());
            }
        });
    }

    private static HashMap<String, String> createParams(long groupId, boolean isFinalInGroup) {
        if (groupId == 0) {
            return null;
        }

        var params = new HashMap<String, String>();

        params.put("groupId", String.valueOf(groupId));

        if (isFinalInGroup) {
            params.put("final", "1");
        }

        return params;
    }
}
