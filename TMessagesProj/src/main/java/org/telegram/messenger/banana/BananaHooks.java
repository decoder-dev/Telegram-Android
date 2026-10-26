package org.telegram.messenger.banana;

import com.radolyn.ayugram.AyuConfig;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.util.Date;

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

    public static boolean showDC(int account) {
        return BananaConfig.get(account, BananaConfig.Option.SHOW_DC);
    }

    public static boolean showProfileId(int account) {
        return BananaConfig.get(account, BananaConfig.Option.SHOW_PROFILE_ID);
    }

    public static boolean profileIdBotApiFormat(int account) {
        return BananaConfig.get(account, BananaConfig.Option.PROFILE_ID_BOT_API_FORMAT);
    }

    public static boolean exactLastSeen(int account) {
        return BananaConfig.get(account, BananaConfig.Option.EXACT_LAST_SEEN);
    }

    public static boolean relativeLastSeen(int account) {
        return BananaConfig.get(account, BananaConfig.Option.RELATIVE_LAST_SEEN);
    }

    public static boolean showsOfflineDot(int account) {
        return BananaConfig.get(account, BananaConfig.Option.SHOWS_OFFLINE_DOT);
    }

    public static boolean snowEnabled(int account) {
        return BananaConfig.get(account, BananaConfig.Option.SNOW_ENABLED);
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

    public static int resolveDcId(int account, TLRPC.User user, TLRPC.Chat chat) {
        if (user != null && user.photo != null && user.photo.dc_id != 0) {
            return user.photo.dc_id;
        }
        if (chat != null && chat.photo != null && chat.photo.dc_id != 0) {
            return chat.photo.dc_id;
        }
        if (user != null && user.id == UserConfig.getInstance(account).getClientUserId()) {
            return ConnectionsManager.getInstance(account).getCurrentDatacenterId();
        }
        return 0;
    }

    public static String formatProfileId(int account, long userId, long chatId) {
        if (userId != 0) {
            return Long.toString(userId);
        }
        if (chatId == 0) {
            return "";
        }
        if (profileIdBotApiFormat(account)) {
            TLRPC.Chat chat = MessagesController.getInstance(account).getChat(chatId);
            if (chat != null && (chat.megagroup || chat.broadcast || ChatObjectIsChannel(chat))) {
                return "-100" + chatId;
            }
            return Long.toString(-chatId);
        }
        return Long.toString(chatId);
    }

    private static boolean ChatObjectIsChannel(TLRPC.Chat chat) {
        return chat instanceof TLRPC.TL_channel || chat instanceof TLRPC.TL_channelForbidden;
    }

    /**
     * Optional exact / relative last-seen formatting. Returns null to keep stock text.
     */
    public static String formatUserStatusOverride(int account, TLRPC.User user, boolean[] isOnline, boolean[] madeShorter) {
        if (user == null || user.status == null || UserObject.isDeleted(user) || user instanceof TLRPC.TL_userEmpty) {
            return null;
        }
        int currentTime = ConnectionsManager.getInstance(account).getCurrentTime();
        int expires = user.status.expires;
        if (expires <= 0) {
            if (MessagesController.getInstance(account).onlinePrivacy.containsKey(user.id)) {
                return null;
            }
            return null;
        }
        if (expires > currentTime) {
            return null;
        }
        if (expires <= 0 || expires == -1 || expires == -100 || expires == -1000 ||
                expires == -101 || expires == -1001 || expires == -102 || expires == -1002) {
            return null;
        }
        boolean exact = exactLastSeen(account);
        boolean relative = relativeLastSeen(account);
        if (!exact && !relative) {
            return null;
        }
        if (relative) {
            long diff = Math.max(0, currentTime - expires);
            String relativeText = LocaleController.formatRelativeDate(diff);
            if (exact) {
                String time = LocaleController.getInstance().getFormatterDay().format(new Date(expires * 1000L));
                return LocaleController.formatString(R.string.LastSeenFormatted, relativeText + ", " + time);
            }
            return LocaleController.formatString(R.string.LastSeenFormatted, relativeText);
        }
        // exact only: full date/time with seconds when available
        try {
            Date date = new Date(expires * 1000L);
            String day = LocaleController.getInstance().getFormatterDay().format(date);
            String dayMonth = LocaleController.getInstance().getFormatterDayMonth().format(date);
            String withSeconds = LocaleController.getInstance().getFormatterDayWithSeconds().format(date);
            return LocaleController.formatString(R.string.LastSeenDateFormatted,
                    LocaleController.formatString(R.string.formatDateAtTime, dayMonth, withSeconds != null ? withSeconds : day));
        } catch (Exception e) {
            return null;
        }
    }
}
