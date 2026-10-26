package org.telegram.messenger.banana;

import com.radolyn.ayugram.AyuConfig;

import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.TLRPC;

/**
 * Behavioral gates for BananaGram Extras. Call sites stay thin; preference reads stay here.
 */
public final class BananaHooks {
    private BananaHooks() {}

    public static boolean hideAds(int account) {
        return BananaConfig.get(account, BananaConfig.Option.HIDE_ADS) || AyuConfig.disableAds;
    }

    public static boolean hideBlockedMessages(int account) {
        return BananaConfig.get(account, BananaConfig.Option.HIDE_BLOCKED_MESSAGES);
    }

    public static boolean showMessageSeconds(int account) {
        return BananaConfig.get(account, BananaConfig.Option.SHOW_MESSAGE_SECONDS);
    }

    public static boolean confirmBeforeCall(int account) {
        return BananaConfig.get(account, BananaConfig.Option.CONFIRM_BEFORE_CALL);
    }

    public static boolean hideMentionNotifications(int account) {
        return BananaConfig.get(account, BananaConfig.Option.HIDE_MENTION_NOTIFICATIONS);
    }

    public static boolean hidePinnedNotifications(int account) {
        return BananaConfig.get(account, BananaConfig.Option.HIDE_PINNED_NOTIFICATIONS);
    }

    public static boolean hideStories(int account) {
        return BananaConfig.get(account, BananaConfig.Option.HIDE_STORIES);
    }

    public static boolean hideArchive(int account) {
        return BananaConfig.get(account, BananaConfig.Option.HIDE_ARCHIVE);
    }

    public static boolean compactChatList(int account) {
        return BananaConfig.get(account, BananaConfig.Option.COMPACT_CHAT_LIST);
    }

    public static boolean exactCounts(int account) {
        return BananaConfig.get(account, BananaConfig.Option.EXACT_COUNTS);
    }

    public static boolean confirmShareLinks(int account) {
        return BananaConfig.get(account, BananaConfig.Option.CONFIRM_SHARE_LINKS);
    }

    public static boolean confirmTelegramLinks(int account) {
        return BananaConfig.get(account, BananaConfig.Option.CONFIRM_TELEGRAM_LINKS);
    }

    public static boolean hideServiceMessages(int account) {
        return BananaConfig.get(account, BananaConfig.Option.HIDE_SERVICE_MESSAGES);
    }

    public static boolean autoRevealSpoilers(int account) {
        return BananaConfig.get(account, BananaConfig.Option.AUTO_REVEAL_SPOILERS);
    }

    public static boolean hideReactionsBar(int account) {
        return BananaConfig.get(account, BananaConfig.Option.HIDE_REACTIONS_BAR);
    }

    public static boolean hideShareButton(int account) {
        return BananaConfig.get(account, BananaConfig.Option.HIDE_SHARE_BUTTON);
    }

    public static boolean streamerMode(int account) {
        return BananaConfig.get(account, BananaConfig.Option.STREAMER_MODE);
    }

    public static boolean localPremium(int account) {
        return BananaConfig.get(account, BananaConfig.Option.LOCAL_PREMIUM) || AyuConfig.localPremium;
    }

    public static boolean bypassDownloadRestrictions(int account) {
        return BananaConfig.get(account, BananaConfig.Option.BYPASS_DOWNLOAD_RESTRICTIONS);
    }

    public static boolean hideAllChats(int account) {
        return BananaConfig.get(account, BananaConfig.Option.HIDE_ALL_CHATS);
    }

    public static boolean rememberLastFolder(int account) {
        return BananaConfig.get(account, BananaConfig.Option.REMEMBER_LAST_FOLDER);
    }

    public static boolean ghostReadOnInteract(int account) {
        return BananaConfig.get(account, BananaConfig.Option.GHOST_READ_ON_INTERACT);
    }

    public static boolean dontReadMessages(int account) {
        return BananaConfig.get(account, BananaConfig.Option.DONT_READ_MESSAGES);
    }

    public static boolean shouldHideServiceMessage(int account, TLRPC.Message message) {
        if (!hideServiceMessages(account) || message == null || message.action == null) {
            return false;
        }
        return message.action instanceof TLRPC.TL_messageActionChatAddUser
                || message.action instanceof TLRPC.TL_messageActionChatDeleteUser
                || message.action instanceof TLRPC.TL_messageActionChatJoinedByLink
                || message.action instanceof TLRPC.TL_messageActionChatJoinedByRequest
                || message.action instanceof TLRPC.TL_messageActionChatEditTitle
                || message.action instanceof TLRPC.TL_messageActionChatEditPhoto
                || message.action instanceof TLRPC.TL_messageActionChatDeletePhoto
                || message.action instanceof TLRPC.TL_messageActionPinMessage
                || message.action instanceof TLRPC.TL_messageActionHistoryClear;
    }

    public static boolean shouldHideBlockedSender(int account, long fromUserId) {
        if (!hideBlockedMessages(account) || fromUserId <= 0) {
            return false;
        }
        return MessagesController.getInstance(account).blockePeers.indexOfKey(fromUserId) >= 0;
    }

    public static String maybeMaskPhone(int account, String phone) {
        if (!streamerMode(account) || phone == null || phone.length() == 0) {
            return phone;
        }
        return "•••••";
    }

    public static String maybeMaskUsername(int account, String username) {
        if (!streamerMode(account) || username == null || username.length() == 0) {
            return username;
        }
        return "hidden";
    }
}
