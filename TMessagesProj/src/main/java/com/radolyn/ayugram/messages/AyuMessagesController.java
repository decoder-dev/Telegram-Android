package com.radolyn.ayugram.messages;

import android.os.Environment;
import android.text.TextUtils;

import androidx.annotation.OptIn;
import androidx.room.ExperimentalRoomApi;
import androidx.room.Room;

import android.util.Log;
import com.radolyn.ayugram.AyuConfig;
import com.radolyn.ayugram.AyuConstants;
import com.radolyn.ayugram.database.AyuDatabase;
import com.radolyn.ayugram.database.dao.DeletedMessageDao;
import com.radolyn.ayugram.database.dao.EditedMessageDao;
import com.radolyn.ayugram.database.entities.DeletedMessage;
import com.radolyn.ayugram.database.entities.EditedMessage;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AyuMessagesController {
    public static final String attachmentsSubfolder = "Saved Attachments";
    private static final File attachmentsPath = new File(
            new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), AyuConstants.APP_NAME),
            attachmentsSubfolder
    );
    private static AyuMessagesController instance;
    private final AyuDatabase database;
    private final EditedMessageDao editedMessageDao;
    private final DeletedMessageDao deletedMessageDao;

    private @OptIn(markerClass = ExperimentalRoomApi.class) AyuMessagesController() {
        // recreate for testing if debug
        if (ExteraConfig.getLogging()) {
            ApplicationLoader.applicationContext.deleteDatabase("ayu-data");
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

        database = Room.databaseBuilder(ApplicationLoader.applicationContext, AyuDatabase.class, "ayu-data")
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .setAutoCloseTimeout(10, TimeUnit.MINUTES)
                .build();

        editedMessageDao = database.editedMessageDao();
        deletedMessageDao = database.deletedMessageDao();
    }

    public static AyuMessagesController getInstance() {
        if (instance == null) {
            instance = new AyuMessagesController();
        }
        return instance;
    }

    public void onMessageEdited(TLRPC.Message oldMessage, TLRPC.Message newMessage, long userId, int accountId, int currentTime) {
        if (!AyuConfig.keepMessagesHistory) {
            return;
        }

        onMessageEditedInner(oldMessage, newMessage, userId, accountId, currentTime);
    }

    private void onMessageEditedInner(TLRPC.Message oldMessage, TLRPC.Message newMessage, long userId, int accountId, int currentTime) {
        boolean sameMedia = false;
        boolean isDocument = false;
        if (oldMessage.media instanceof TLRPC.TL_messageMediaPhoto && newMessage.media instanceof TLRPC.TL_messageMediaPhoto && oldMessage.media.photo != null && newMessage.media.photo != null) {
            sameMedia = oldMessage.media.photo.id == newMessage.media.photo.id;
        } else if (oldMessage.media instanceof TLRPC.TL_messageMediaDocument && newMessage.media instanceof TLRPC.TL_messageMediaDocument && oldMessage.media.document != null && newMessage.media.document != null) {
            sameMedia = oldMessage.media.document.id == newMessage.media.document.id;
            isDocument = true;
        } else if (oldMessage.media instanceof TLRPC.TL_messageMediaWebPage && newMessage.media instanceof TLRPC.TL_messageMediaWebPage) {
            sameMedia = true;
        }

        if (sameMedia && TextUtils.equals(oldMessage.message, newMessage.message)) {
            return;
        }

        // reactions fix
        if (!sameMedia && newMessage.edit_hide) {
            return;
        }

        var revision = new EditedMessage();

        var attachPathFile = FileLoader.getInstance(accountId).getPathToMessage(oldMessage);

        if (!sameMedia && attachPathFile.exists()) {
            var filename = attachPathFile.getName();
            var dest = new File(attachmentsPath, filename);

            // copy file, because it's likely to be deleted by Telegram in a few seconds
            boolean success;
            try {
                success = AndroidUtilities.copyFile(attachPathFile, dest);
            } catch (IOException e) {
                Log.d("AyuGram", e.toString());
                success = false;
            }

            if (success) {
                attachPathFile = new File(dest.getAbsolutePath());
            } else {
                attachPathFile = new File("/");
            }
        }

        var attachPath = attachPathFile.getAbsolutePath();

        revision.path = attachPath.equals("/") ? null : attachPath;
        revision.isDocument = isDocument;

        var dialogId = MessageObject.getDialogId(oldMessage);
        var messageId = newMessage.id;

        if (!sameMedia && !TextUtils.isEmpty(revision.path) && editedMessageDao.isFirstRevisionWithChangedMedia(userId, dialogId, messageId)) {
            // update previous revisions to reflect media change
            // like, there's no previous file, so replace it with one we copied before...
            editedMessageDao.updateAttachmentForRevisionsBeforeDate(userId, dialogId, messageId, attachPath, currentTime);
        }

        revision.userId = userId;
        revision.dialogId = dialogId;
        revision.messageId = messageId;
        revision.text = oldMessage.message;
        revision.date = currentTime;

        editedMessageDao.insert(revision);
    }

    public void onMessageDeleted(long userId, long dialogId, int msgId, int currentTime) {
        if (!AyuConfig.keepDeletedMessages) {
            return;
        }

        onMessageDeletedInner(userId, dialogId, msgId, currentTime);
    }

    private void onMessageDeletedInner(long userId, long dialogId, int msgId, int currentTime) {
        var msg = new DeletedMessage();
        msg.userId = userId;
        msg.dialogId = dialogId;
        msg.messageId = msgId;
        msg.date = currentTime;

        deletedMessageDao.insert(msg);
    }

    public boolean hasAnyRevisions(long userId, long dialogId, int msgId) {
        return editedMessageDao.hasAnyRevisions(userId, dialogId, msgId);
    }

    public List<EditedMessage> getRevisions(long userId, long dialogId, int msgId) {
        return editedMessageDao.getAllRevisions(userId, dialogId, msgId);
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
        editedMessageDao.cleanTable();
        deletedMessageDao.cleanTable();

        database.close();

        ApplicationLoader.applicationContext.deleteDatabase("ayu-data");

        // force recreate database to avoid crash
        instance = null;
    }
}
