package org.telegram.messenger.banana;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.radolyn.ayugram.AyuConfig;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;

import java.util.Date;

/**
 * Behavioral gates for BananaGram Extras. Call sites stay thin; preference reads stay here.
 */
public final class BananaHooks {
    private BananaHooks() {}

    private static String lastConfirmedUrl;
    private static long lastConfirmedAt;

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

    public static boolean compactMessagePreview(int account) {
        return BananaConfig.get(account, BananaConfig.Option.COMPACT_MESSAGE_PREVIEW);
    }

    /** Base DialogCell row height in dp (excluding separator / tags). */
    public static int dialogCellHeightDp(int account, boolean threeLines) {
        if (compactChatList(account)) {
            return threeLines ? 62 : 56;
        }
        return threeLines ? 76 : 70;
    }

    /** Forum DialogCell row height in dp (excluding separator / tags). */
    public static int forumDialogCellHeightDp(int account, boolean threeLines) {
        if (compactChatList(account)) {
            return threeLines ? 74 : 78;
        }
        return threeLines ? 86 : 91;
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

    public static boolean confirmVoiceSend(int account) {
        return BananaConfig.get(account, BananaConfig.Option.CONFIRM_VOICE_SEND);
    }

    public static boolean stopAfterVoice(int account) {
        return BananaConfig.get(account, BananaConfig.Option.STOP_AFTER_VOICE);
    }

    public static boolean stopAfterRoundVideo(int account) {
        return BananaConfig.get(account, BananaConfig.Option.STOP_AFTER_ROUND_VIDEO);
    }

    public static boolean hideAllChats(int account) {
        return BananaConfig.get(account, BananaConfig.Option.HIDE_ALL_CHATS);
    }

    public static boolean rememberLastFolder(int account) {
        return BananaConfig.get(account, BananaConfig.Option.REMEMBER_LAST_FOLDER);
    }

    public static boolean hideFolderUnreadCount(int account) {
        return BananaConfig.get(account, BananaConfig.Option.HIDE_FOLDER_UNREAD_COUNT);
    }

    public static boolean chatListHidesConnectionStatus(int account) {
        return BananaConfig.get(account, BananaConfig.Option.CHAT_LIST_HIDES_CONNECTION_STATUS);
    }

    public static boolean chatListHidesSearch(int account) {
        return BananaConfig.get(account, BananaConfig.Option.CHAT_LIST_HIDES_SEARCH);
    }

    public static boolean bottomChatFoldersEnabled(int account) {
        return BananaConfig.get(account, BananaConfig.Option.BOTTOM_CHAT_FOLDERS_ENABLED);
    }

    public static boolean ghostReadOnInteract(int account) {
        return BananaConfig.get(account, BananaConfig.Option.GHOST_READ_ON_INTERACT);
    }

    public static boolean dontReadMessages(int account) {
        return BananaConfig.get(account, BananaConfig.Option.DONT_READ_MESSAGES);
    }

    /** Master switch for Banana MessageFilters (Extras) plus FilterStore UI toggle. */
    public static boolean regexFiltersEnabled(int account) {
        return BananaConfig.get(account, BananaConfig.Option.REGEX_MESSAGE_FILTERS_ENABLED)
                || FilterStore.get(account).enabled();
    }

    public static boolean regexFiltersCaseInsensitive(int account) {
        return BananaConfig.get(account, BananaConfig.Option.REGEX_MESSAGE_FILTERS_CASE_INSENSITIVE);
    }

    public static boolean saveDeleted(int account) {
        return BananaConfig.get(account, BananaConfig.Option.SAVE_DELETED);
    }

    public static boolean saveEdits(int account) {
        return BananaConfig.get(account, BananaConfig.Option.SAVE_EDITS);
    }

    public static boolean saveMediaHistory(int account) {
        return BananaConfig.get(account, BananaConfig.Option.SAVE_MEDIA_HISTORY);
    }

    public static boolean proactiveSaveMedia(int account) {
        return BananaConfig.get(account, BananaConfig.Option.PROACTIVE_SAVE_MEDIA)
                && saveMediaHistory(account);
    }

    /**
     * When DONT_READ_MESSAGES is on and GHOST_READ_ON_INTERACT is on, allow one read
     * receipt for this message (tap / send interaction).
     */
    public static void maybeMarkReadOnInteract(int account, TLRPC.Message message) {
        if (message == null || message.id <= 0 || message.peer_id == null) {
            return;
        }
        if (!dontReadMessages(account) || !ghostReadOnInteract(account)) {
            return;
        }
        try {
            com.radolyn.ayugram.utils.AyuGhostUtils.markReadOnServer(
                    account,
                    message.id,
                    MessagesController.getInstance(account).getInputPeer(message.peer_id));
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    public static boolean hideOnline(int account) {
        return BananaConfig.get(account, BananaConfig.Option.HIDE_ONLINE);
    }

    /** Typing / upload progress suppression (ghost + dedicated toggles). */
    public static boolean dontSendTyping(int account) {
        return BananaConfig.get(account, BananaConfig.Option.GHOST_DONT_SEND_TYPING)
                || BananaConfig.get(account, BananaConfig.Option.HIDE_TYPING)
                || BananaConfig.get(account, BananaConfig.Option.HIDE_UPLOAD);
    }

    public static boolean hideTabBar(int account) {
        return BananaConfig.get(account, BananaConfig.Option.HIDE_TAB_BAR);
    }

    public static boolean chatListHidesEmojiStatus(int account) {
        return BananaConfig.get(account, BananaConfig.Option.CHAT_LIST_HIDES_EMOJI_STATUS);
    }

    public static boolean chatListCentersTitle(int account) {
        return BananaConfig.get(account, BananaConfig.Option.CHAT_LIST_CENTERS_TITLE);
    }

    public static boolean hideGiftButton(int account) {
        return BananaConfig.get(account, BananaConfig.Option.HIDE_GIFT_BUTTON);
    }

    public static boolean hideVerification(int account) {
        return BananaConfig.get(account, BananaConfig.Option.HIDE_VERIFICATION);
    }

    public static boolean hidePremiumStatuses(int account) {
        return BananaConfig.get(account, BananaConfig.Option.HIDE_PREMIUM_STATUSES);
    }

    public static boolean showMutualContact(int account) {
        return BananaConfig.get(account, BananaConfig.Option.SHOW_MUTUAL_CONTACT);
    }

    public static boolean showChatCreationDate(int account) {
        return BananaConfig.get(account, BananaConfig.Option.SHOW_CHAT_CREATION_DATE);
    }

    public static boolean showDisappearedGifts(int account) {
        return BananaConfig.get(account, BananaConfig.Option.SHOW_DISAPPEARED_GIFTS);
    }

    public static boolean visualPhoneEnabled(int account) {
        return BananaConfig.get(account, BananaConfig.Option.VISUAL_PHONE_ENABLED);
    }

    public static String visualPhoneNumber(int account) {
        return BananaConfig.visualPhoneNumber(account);
    }

    public static boolean autoPauseMediaInBackground(int account) {
        return BananaConfig.get(account, BananaConfig.Option.AUTO_PAUSE_MEDIA_IN_BACKGROUND);
    }

    public static boolean hidePaidReactions(int account) {
        return BananaConfig.get(account, BananaConfig.Option.HIDE_PAID_REACTIONS);
    }

    public static boolean hideChannelPostReactions(int account) {
        return BananaConfig.get(account, BananaConfig.Option.HIDE_CHANNEL_POST_REACTIONS);
    }

    public static boolean hideViaBot(int account) {
        return BananaConfig.get(account, BananaConfig.Option.HIDE_VIA_BOT);
    }

    public static boolean hideGreetingSticker(int account) {
        return BananaConfig.get(account, BananaConfig.Option.HIDE_GREETING_STICKER);
    }

    public static boolean hideStickerChecks(int account) {
        return BananaConfig.get(account, BananaConfig.Option.HIDE_STICKER_CHECKS);
    }

    public static boolean wideTabBar(int account) {
        return BananaConfig.get(account, BananaConfig.Option.WIDE_TAB_BAR);
    }

    public static boolean showContactsTab(int account) {
        return BananaConfig.get(account, BananaConfig.Option.SHOW_CONTACTS_TAB);
    }

    /** Folder-tab horizontal padding dp: wider when WIDE_TAB_BAR is on. */
    public static float filterTabPaddingDp(int account) {
        return wideTabBar(account) ? 36f : 24f;
    }

    /** Max side for outgoing photo compression (0→1280, 1→1920, 2→2560). */
    public static int outgoingPhotoMaxSide(int account) {
        return BananaConfig.outgoingPhotoMaxSide(account);
    }

    public static int doubleTapSeekSeconds(int account) {
        return BananaConfig.doubleTapSeekSeconds(account);
    }

    /** 0 = auto, 1 = highest, 2 = lowest. */
    public static int defaultVideoQuality(int account) {
        return BananaConfig.defaultVideoQuality(account);
    }

    public static boolean showMessageId(int account) {
        return BananaConfig.get(account, BananaConfig.Option.SHOW_MESSAGE_ID);
    }

    public static boolean showMediaInfo(int account) {
        return BananaConfig.get(account, BananaConfig.Option.SHOW_MEDIA_INFO);
    }

    public static boolean glassBubbles(int account) {
        return BananaConfig.get(account, BananaConfig.Option.GLASS_BUBBLES);
    }

    public static boolean cleanTrackingLinksEnabled(int account) {
        return BananaConfig.cleanTrackingLinks(account);
    }

    public static boolean shouldSendSilently(int account) {
        return BananaConfig.shouldSendSilently(account);
    }

    public static boolean ghostScheduleMessages(int account) {
        return BananaConfig.get(account, BananaConfig.Option.GHOST_SCHEDULE_MESSAGES)
                && BananaConfig.isGhostModeActive(account);
    }

    /** Sticker display size multiplier: 50–150 → 0.5–1.5. */
    public static float stickerSizeScale(int account) {
        return BananaConfig.stickerSizePercent(account) / 100f;
    }

    /**
     * Round radius in px for stickers. Shape 0 = square, 1 = soft (~12% side), 2 = pill (~25% side).
     */
    public static int stickerRoundRadiusPx(int account, int sidePx) {
        int shape = BananaConfig.stickerShape(account);
        if (shape <= 0 || sidePx <= 0) {
            return 0;
        }
        float fraction = shape == 2 ? 0.25f : 0.12f;
        return Math.max(1, Math.round(sidePx * fraction));
    }

    /** Soften bubble color when glass bubbles are enabled (tint percent → alpha). */
    public static int applyGlassBubbleColor(int account, int color) {
        if (!glassBubbles(account)) {
            return color;
        }
        int tint = BananaConfig.glassBubbleTint(account);
        int alpha = Math.max(40, Math.min(255, Math.round(255 * (100 - tint * 0.55f) / 100f)));
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    public static boolean isGlassBubbleColorKey(int key) {
        return key == org.telegram.ui.ActionBar.Theme.key_chat_inBubble
                || key == org.telegram.ui.ActionBar.Theme.key_chat_outBubble
                || key == org.telegram.ui.ActionBar.Theme.key_chat_inBubbleSelected
                || key == org.telegram.ui.ActionBar.Theme.key_chat_outBubbleSelected
                || key == org.telegram.ui.ActionBar.Theme.key_chat_inBubbleShadow
                || key == org.telegram.ui.ActionBar.Theme.key_chat_outBubbleShadow;
    }

    /** Strip tracking params from a URL when the Extra is on; otherwise return as-is. */
    public static String maybeCleanTrackingUrl(int account, String url) {
        if (TextUtils.isEmpty(url) || !cleanTrackingLinksEnabled(account)) {
            return url;
        }
        try {
            String cleaned = TrackingUrl.clean(url);
            return cleaned != null ? cleaned : url;
        } catch (Throwable ignore) {
            return url;
        }
    }

    public static String deletedMark(int account) {
        String mark = BananaConfig.deletedMessageMark(account);
        return mark != null ? mark : "";
    }

    public static String editedMark(int account) {
        String mark = BananaConfig.editedMessageMark(account);
        return mark != null ? mark : "";
    }

    /** Append " #id" when SHOW_MESSAGE_ID is enabled. */
    public static String appendMessageId(int account, String timeString, int messageId) {
        if (!showMessageId(account) || messageId <= 0 || timeString == null) {
            return timeString;
        }
        return timeString + " #" + messageId;
    }

    /** Compact media dimensions / size / duration for overlays. */
    public static String formatMediaInfo(int account, int width, int height, long sizeBytes, int durationSec) {
        if (!showMediaInfo(account)) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (width > 0 && height > 0) {
            sb.append(width).append('×').append(height);
        }
        if (sizeBytes > 0) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(AndroidUtilities.formatFileSize(sizeBytes));
        }
        if (durationSec > 0) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(LocaleController.formatShortDuration(durationSec));
        }
        return sb.length() > 0 ? sb.toString() : null;
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

    /** Share deep links: tg://msg_url, tg://msg, tg://share, t.me/share, t.me/msg. */
    public static boolean isShareLink(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }
        String lower = url.toLowerCase();
        if (lower.startsWith("tg:msg") || lower.startsWith("tg://msg")
                || lower.startsWith("tg:share") || lower.startsWith("tg://share")) {
            return true;
        }
        try {
            Uri uri = Uri.parse(url);
            String host = AndroidUtilities.getHostAuthority(uri);
            if (host == null) {
                return false;
            }
            host = host.toLowerCase();
            if (!"t.me".equals(host) && !"telegram.me".equals(host) && !"telegram.dog".equals(host)
                    && !host.endsWith(".t.me")) {
                return false;
            }
            String path = uri.getPath();
            if (path == null) {
                return false;
            }
            path = path.toLowerCase();
            return path.equals("/share") || path.startsWith("/share/")
                    || path.equals("/msg") || path.startsWith("/msg/");
        } catch (Exception ignore) {
            return false;
        }
    }

    /** Alias for share / msg_url style links. */
    public static boolean isShareOrMsgUrl(String url) {
        return isShareLink(url);
    }

    /** In-app Telegram links (t.me / tg://) excluding share deep links. */
    public static boolean isTelegramDeepLink(String url) {
        if (TextUtils.isEmpty(url) || isShareLink(url)) {
            return false;
        }
        return Browser.isInternalUrl(url, null);
    }

    /** String resource for a confirm dialog, or 0 if none needed. */
    public static int confirmationTitleResForUrl(int account, String url) {
        if (TextUtils.isEmpty(url)) {
            return 0;
        }
        if (isShareLink(url)) {
            if (confirmShareLinks(account)) {
                return R.string.BananaOpt_CONFIRM_SHARE_LINKS;
            }
            if (confirmTelegramLinks(account)) {
                return R.string.BananaOpt_CONFIRM_TELEGRAM_LINKS;
            }
            return 0;
        }
        if (confirmTelegramLinks(account) && isTelegramDeepLink(url)) {
            return R.string.BananaOpt_CONFIRM_TELEGRAM_LINKS;
        }
        return 0;
    }

    public static boolean shouldConfirmLink(int account, String url) {
        return confirmationTitleResForUrl(account, url) != 0;
    }

    /**
     * Shows a confirm dialog when Extras require it. Returns true if a dialog was shown
     * (proceed runs only after OK); false if the caller should continue immediately.
     */
    public static boolean interceptOpenUrl(Context context, int account, String url, Runnable proceed) {
        if (proceed == null) {
            return false;
        }
        if (url != null && url.equals(lastConfirmedUrl) && System.currentTimeMillis() - lastConfirmedAt < 4000) {
            return false;
        }
        int titleRes = confirmationTitleResForUrl(account, url);
        if (titleRes == 0 || context == null) {
            return false;
        }
        try {
            new AlertDialog.Builder(context)
                    .setTitle(LocaleController.getString(titleRes))
                    .setMessage(url)
                    .setPositiveButton(LocaleController.getString(R.string.Open), (d, w) -> {
                        lastConfirmedUrl = url;
                        lastConfirmedAt = System.currentTimeMillis();
                        proceed.run();
                    })
                    .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
                    .show();
            return true;
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }

    public static boolean interceptOpenUrl(Context context, String url, Runnable proceed) {
        return interceptOpenUrl(context, UserConfig.selectedAccount, url, proceed);
    }

    public static String maybeMaskPhone(int account, String phone) {
        if (phone == null || phone.length() == 0) {
            return phone;
        }
        if (streamerMode(account)) {
            return "•••••";
        }
        if (visualPhoneEnabled(account)) {
            String visual = visualPhoneNumber(account);
            if (!TextUtils.isEmpty(visual)) {
                return visual;
            }
        }
        return phone;
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
