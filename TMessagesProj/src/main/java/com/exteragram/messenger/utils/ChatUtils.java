package com.exteragram.messenger.utils;

import androidx.core.util.Consumer;

import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

/**
 * Stubbed exteragram helpers used by Banana/Ayu hooks.
 * Prefer real exteragram sources when available.
 */
public final class ChatUtils {
    private ChatUtils() {}

    public static MessagesController getMessagesController() {
        return MessagesController.getInstance(UserConfig.selectedAccount);
    }

    public static boolean hasArchivedChats() {
        MessagesController controller = getMessagesController();
        return controller != null && !controller.getDialogs(1).isEmpty();
    }

    public static CharSequence getMessageText(MessageObject message, MessageObject.GroupedMessages group) {
        if (message == null) {
            return "";
        }
        if (message.messageText != null) {
            return message.messageText;
        }
        if (message.messageOwner != null && message.messageOwner.message != null) {
            return message.messageOwner.message;
        }
        return "";
    }

    public static String getDCName(int dc) {
        switch (dc) {
            case 1:
            case 3:
                return "Miami";
            case 2:
            case 4:
                return "Amsterdam";
            case 5:
                return "Singapore";
            default:
                return "Unknown";
        }
    }

    public static String getOwnerIds(long stickerSetId) {
        return Long.toString(stickerSetId >> 32);
    }

    public static void searchById(long userId, Consumer<TLRPC.User> callback) {
        if (callback == null) {
            return;
        }
        TLRPC.User user = getMessagesController().getUser(userId);
        callback.accept(user);
    }
}
