package org.telegram.messenger.banana;

import android.content.Context;
import android.content.SharedPreferences;
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
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.FilterCreateActivity;
import org.telegram.ui.LaunchActivity;

import java.util.ArrayList;
import java.util.Date;

/**
 * Behavioral gates for BananaGram Extras. Call sites stay thin; preference reads stay here.
 */
public final class BananaHooks {
    private BananaHooks() {}

    private static final ThreadLocal<String> confirmedUrlBypass = new ThreadLocal<>();

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

    /** Ayu OR Banana: enable deleted-message archive path. */
    public static boolean ayuSaveDeletedEnabled(int account) {
        return AyuConfig.saveDeletedMessages || saveDeleted(account);
    }

    /** Ayu OR Banana: enable edit-history archive path. */
    public static boolean ayuSaveEditsEnabled(int account) {
        return AyuConfig.saveMessagesHistory || saveEdits(account);
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

    /**
     * Typing / recording / upload suppression for every outgoing activity request, including ones
     * built outside MessagesController.sendTyping (emoji taps, calls, secret chats). Each kind follows
     * its own toggle; a cancellation is never dropped.
     */
    public static boolean suppressTypingRequest(int account, TLObject request) {
        int code;
        if (request instanceof TLRPC.TL_messages_setEncryptedTyping) {
            code = ((TLRPC.TL_messages_setEncryptedTyping) request).typing ? 0 : 2;
        } else if (request instanceof TLRPC.TL_messages_setTyping) {
            code = activityCode(((TLRPC.TL_messages_setTyping) request).action);
        } else {
            return false;
        }
        if (code == 2) {
            return false;
        }
        if (BananaConfig.get(account, BananaConfig.Option.GHOST_DONT_SEND_TYPING)) {
            return true;
        }
        return code >= 0 && BananaConfig.suppressActivity(account, code);
    }

    /** MessagesController.sendTyping action codes; -1 for kinds without a dedicated toggle. */
    private static int activityCode(TLRPC.SendMessageAction action) {
        if (action instanceof TLRPC.TL_sendMessageTypingAction || action instanceof TLRPC.TL_sendMessageEmojiInteraction) return 0;
        if (action instanceof TLRPC.TL_sendMessageRecordAudioAction) return 1;
        if (action instanceof TLRPC.TL_sendMessageCancelAction) return 2;
        if (action instanceof TLRPC.TL_sendMessageUploadDocumentAction) return 3;
        if (action instanceof TLRPC.TL_sendMessageUploadPhotoAction) return 4;
        if (action instanceof TLRPC.TL_sendMessageUploadVideoAction) return 5;
        if (action instanceof TLRPC.TL_sendMessageGamePlayAction) return 6;
        if (action instanceof TLRPC.TL_sendMessageRecordRoundAction) return 7;
        if (action instanceof TLRPC.TL_sendMessageUploadRoundAction) return 8;
        if (action instanceof TLRPC.TL_sendMessageUploadAudioAction) return 9;
        if (action instanceof TLRPC.TL_sendMessageChooseStickerAction) return 10;
        if (action instanceof TLRPC.TL_sendMessageEmojiInteractionSeen) return 11;
        return -1;
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

    public static boolean hideStickerStatus(int account) {
        return BananaConfig.get(account, BananaConfig.Option.HIDE_STICKER_STATUS);
    }

    public static boolean hideStickerReplyColors(int account) {
        return BananaConfig.get(account, BananaConfig.Option.HIDE_STICKER_REPLY_COLORS);
    }

    public static boolean hideStickerReplyEmoji(int account) {
        return BananaConfig.get(account, BananaConfig.Option.HIDE_STICKER_REPLY_EMOJI);
    }

    public static boolean hideStickerReplyBackground(int account) {
        return BananaConfig.get(account, BananaConfig.Option.HIDE_STICKER_REPLY_BACKGROUND);
    }

    public static boolean hidePremiumReplyEmoji(int account) {
        return BananaConfig.get(account, BananaConfig.Option.HIDE_PREMIUM_REPLY_EMOJI);
    }

    public static boolean infiniteRecentStickers(int account) {
        return BananaConfig.get(account, BananaConfig.Option.INFINITE_RECENT_STICKERS);
    }

    public static boolean onlyAddedStickers(int account) {
        return BananaConfig.get(account, BananaConfig.Option.ONLY_ADDED_STICKERS);
    }

    public static boolean saveStickerToPhotos(int account) {
        return BananaConfig.get(account, BananaConfig.Option.SAVE_STICKER_TO_PHOTOS);
    }

    public static boolean useRecentEmojiInReactions(int account) {
        return BananaConfig.get(account, BananaConfig.Option.USE_RECENT_EMOJI_IN_REACTIONS);
    }

    public static boolean disableColoredReplies(int account) {
        return BananaConfig.get(account, BananaConfig.Option.DISABLE_COLORED_REPLIES);
    }

    public static boolean wideChannelPosts(int account) {
        return BananaConfig.get(account, BananaConfig.Option.WIDE_CHANNEL_POSTS);
    }

    public static boolean staticRoundVideoZoom(int account) {
        return BananaConfig.get(account, BananaConfig.Option.STATIC_ROUND_VIDEO_ZOOM);
    }

    public static boolean roundVideoStartsWithRear(int account) {
        return BananaConfig.get(account, BananaConfig.Option.ROUND_VIDEO_STARTS_WITH_REAR);
    }

    public static boolean roundVideoRemembersCamera(int account) {
        return BananaConfig.get(account, BananaConfig.Option.ROUND_VIDEO_REMEMBERS_CAMERA);
    }

    public static boolean forceBuiltInMic(int account) {
        return BananaConfig.get(account, BananaConfig.Option.FORCE_BUILT_IN_MIC);
    }

    public static boolean downloadTikTok(int account) {
        return BananaConfig.get(account, BananaConfig.Option.DOWNLOAD_TIK_TOK);
    }

    public static boolean downloadYouTubeShorts(int account) {
        return BananaConfig.get(account, BananaConfig.Option.DOWNLOAD_YOU_TUBE_SHORTS);
    }

    public static boolean signDownloadedMedia(int account) {
        return BananaConfig.get(account, BananaConfig.Option.SIGN_DOWNLOADED_MEDIA);
    }

    public static boolean scrollToNextChatDisabled(int account) {
        return BananaConfig.get(account, BananaConfig.Option.SCROLL_TO_NEXT_CHAT_DISABLED);
    }

    public static boolean expireTtlButton(int account) {
        return BananaConfig.get(account, BananaConfig.Option.EXPIRE_TTL_BUTTON);
    }

    public static boolean keepBannedChats(int account) {
        return BananaConfig.get(account, BananaConfig.Option.KEEP_BANNED_CHATS);
    }

    public static boolean chooseFolderAfterJoin(int account) {
        return BananaConfig.get(account, BananaConfig.Option.CHOOSE_FOLDER_AFTER_JOIN);
    }

    public static boolean localGroupAlias(int account) {
        return BananaConfig.get(account, BananaConfig.Option.LOCAL_GROUP_ALIAS);
    }

    public static boolean jumpToStartInChatMenu(int account) {
        return BananaConfig.get(account, BananaConfig.Option.JUMP_TO_START_IN_CHAT_MENU);
    }

    public static boolean disableGiftStarsRedirect(int account) {
        return BananaConfig.get(account, BananaConfig.Option.DISABLE_GIFT_STARS_REDIRECT);
    }

    public static boolean inviteLinksBulkRevoke(int account) {
        return BananaConfig.get(account, BananaConfig.Option.INVITE_LINKS_BULK_REVOKE);
    }

    public static boolean hidePrivateChatActivities(int account) {
        return BananaConfig.get(account, BananaConfig.Option.HIDE_PRIVATE_CHAT_ACTIVITIES);
    }

    public static boolean stayAtLatestMessageAfterRefresh(int account) {
        return BananaConfig.get(account, BananaConfig.Option.STAY_AT_LATEST_MESSAGE_AFTER_REFRESH);
    }

    /**
     * Android has no iOS Keychain; this enables the app-private encrypted session snapshot instead.
     */
    public static boolean sessionKeychainBackup(int account) {
        return BananaConfig.get(account, BananaConfig.Option.SESSION_KEYCHAIN_BACKUP);
    }

    /** Schedules the app-private encrypted session snapshot (the Android counterpart of the iOS keychain backup). */
    public static void maybeSessionKeychainBackup(int account) {
        if (!sessionKeychainBackup(account)) {
            return;
        }
        BananaSessionBackup.schedule(account);
    }

    private static final String LOCAL_ALIAS_KEY_PREFIX = "bananaLocalGroupAlias_";

    public static String getLocalGroupAlias(int account, long dialogId) {
        if (dialogId >= 0) {
            return null;
        }
        return MessagesController.getMainSettings(account).getString(LOCAL_ALIAS_KEY_PREFIX + dialogId, null);
    }

    public static void setLocalGroupAlias(int account, long dialogId, String alias) {
        if (dialogId >= 0) {
            return;
        }
        SharedPreferences.Editor editor = MessagesController.getMainSettings(account).edit();
        if (TextUtils.isEmpty(alias)) {
            editor.remove(LOCAL_ALIAS_KEY_PREFIX + dialogId);
        } else {
            editor.putString(LOCAL_ALIAS_KEY_PREFIX + dialogId, alias.trim());
        }
        editor.apply();
    }

    /** Chat title with optional local group alias overlay. */
    public static String resolveChatTitle(int account, long dialogId, String fallback) {
        if (!localGroupAlias(account) || dialogId >= 0) {
            return fallback;
        }
        String alias = getLocalGroupAlias(account, dialogId);
        return TextUtils.isEmpty(alias) ? fallback : alias;
    }

    public static boolean isTikTokUrl(String url) {
        return ExternalVideoUrl.isTikTok(url);
    }

    public static boolean isYouTubeShortsUrl(String url) {
        return ExternalVideoUrl.isYouTubeShorts(url);
    }

    /**
     * When TikTok / YouTube Shorts download Extras are on, prefer system open so the user
     * can save via the external player/browser. Returns true when the URL was handed off.
     */
    public static boolean maybeOpenExternalVideoForDownload(Context context, int account, String url) {
        if (context == null || TextUtils.isEmpty(url)) {
            return false;
        }
        boolean tiktok = downloadTikTok(account) && isTikTokUrl(url);
        boolean shorts = downloadYouTubeShorts(account) && isYouTubeShortsUrl(url);
        if (!tiktok && !shorts) {
            return false;
        }
        if (!VideoDownloadPolicy.accepts(url)) return false;
        try {
            BananaVideoDownloadService.start(context, account, url);
            return true;
        } catch (RuntimeException startFailed) {
            android.widget.Toast.makeText(context, LocaleController.getString(R.string.BananaVideoFailed), android.widget.Toast.LENGTH_LONG).show();
            return true;
        }
    }

    /** Prefix saved gallery filenames when SIGN_DOWNLOADED_MEDIA is on. */
    public static String maybeSignDownloadedFileName(int account, String name) {
        if (!signDownloadedMedia(account) || TextUtils.isEmpty(name)) {
            return name;
        }
        if (name.startsWith("BananaGram_")) {
            return name;
        }
        return "BananaGram_" + name;
    }

    /**
     * After joining a chat/channel, optionally prompt to pick a folder.
     * Thin AlertDialog over dialog filters — keeps preference reads here.
     */
    public static void maybePromptFolderAfterJoin(int account, long chatId) {
        if (!chooseFolderAfterJoin(account) || chatId == 0) {
            return;
        }
        AndroidUtilities.runOnUIThread(() -> {
            try {
                BaseFragment fragment = LaunchActivity.getLastFragment();
                if (fragment == null || fragment.getParentActivity() == null) {
                    return;
                }
                MessagesController controller = MessagesController.getInstance(account);
                ArrayList<MessagesController.DialogFilter> filters = controller.getDialogFilters();
                if (filters == null || filters.isEmpty()) {
                    return;
                }
                ArrayList<CharSequence> names = new ArrayList<>();
                ArrayList<MessagesController.DialogFilter> choices = new ArrayList<>();
                for (int i = 0; i < filters.size(); i++) {
                    MessagesController.DialogFilter filter = filters.get(i);
                    if (filter == null || filter.isDefault()) {
                        continue;
                    }
                    names.add(filter.name != null ? filter.name : ("#" + filter.id));
                    choices.add(filter);
                }
                if (choices.isEmpty()) {
                    return;
                }
                final long dialogId = -chatId;
                AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity());
                builder.setTitle(LocaleController.getString(R.string.BananaChooseFolderAfterJoin));
                builder.setItems(names.toArray(new CharSequence[0]), (dialog, which) -> {
                    if (which < 0 || which >= choices.size()) {
                        return;
                    }
                    MessagesController.DialogFilter filter = choices.get(which);
                    if (!filter.alwaysShow.contains(dialogId)) {
                        filter.alwaysShow.add(dialogId);
                        filter.neverShow.remove(dialogId);
                        FilterCreateActivity.saveFilterToServer(
                                filter, filter.flags, filter.name, filter.entities, filter.title_noanimate,
                                filter.color, filter.alwaysShow, filter.neverShow, filter.pinnedDialogs,
                                false, false, true, true, false, fragment, null);
                    }
                });
                builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                fragment.showDialog(builder.create());
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }, 600);
    }

    /** Prompt to set/clear a local group alias (MainSettings-backed). */
    public static void promptLocalGroupAlias(BaseFragment fragment, int account, long dialogId, String currentTitle) {
        if (fragment == null || fragment.getParentActivity() == null || !localGroupAlias(account) || dialogId >= 0) {
            return;
        }
        final android.widget.EditText input = new android.widget.EditText(fragment.getParentActivity());
        input.setSingleLine(true);
        String existing = getLocalGroupAlias(account, dialogId);
        if (!TextUtils.isEmpty(existing)) {
            input.setText(existing);
            input.setSelection(existing.length());
        } else if (!TextUtils.isEmpty(currentTitle)) {
            input.setHint(currentTitle);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.BananaLocalGroupAlias));
        builder.setView(input);
        builder.setPositiveButton(LocaleController.getString(R.string.OK), (d, w) -> {
            setLocalGroupAlias(account, dialogId, input.getText() != null ? input.getText().toString() : null);
            if (fragment instanceof org.telegram.ui.ChatActivity) {
                ((org.telegram.ui.ChatActivity) fragment).updateTitle(true);
            }
            org.telegram.messenger.NotificationCenter.getInstance(account)
                    .postNotificationName(org.telegram.messenger.NotificationCenter.updateInterfaces,
                            MessagesController.UPDATE_MASK_NAME);
        });
        builder.setNeutralButton(LocaleController.getString(R.string.Delete), (d, w) -> {
            setLocalGroupAlias(account, dialogId, null);
            if (fragment instanceof org.telegram.ui.ChatActivity) {
                ((org.telegram.ui.ChatActivity) fragment).updateTitle(true);
            }
            org.telegram.messenger.NotificationCenter.getInstance(account)
                    .postNotificationName(org.telegram.messenger.NotificationCenter.updateInterfaces,
                            MessagesController.UPDATE_MASK_NAME);
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        fragment.showDialog(builder.create());
    }

    public static boolean showChannelForwardCount(int account) {
        return BananaConfig.get(account, BananaConfig.Option.SHOW_CHANNEL_FORWARD_COUNT);
    }

    public static boolean hideChannelBottomButton(int account) {
        return BananaConfig.get(account, BananaConfig.Option.HIDE_CHANNEL_BOTTOM_BUTTON);
    }

    public static boolean channelBottomDiscuss(int account) {
        return BananaConfig.get(account, BananaConfig.Option.CHANNEL_BOTTOM_DISCUSS);
    }

    public static boolean pollResultsBeforeVoting(int account) {
        return BananaConfig.get(account, BananaConfig.Option.POLL_RESULTS_BEFORE_VOTING);
    }

    public static boolean removeLinkPreviews(int account) {
        return BananaConfig.get(account, BananaConfig.Option.REMOVE_LINK_PREVIEWS);
    }

    public static boolean fixLinkPreviews(int account) {
        return BananaConfig.get(account, BananaConfig.Option.FIX_LINK_PREVIEWS);
    }

    public static boolean quoteRepliesToDeleted(int account) {
        return BananaConfig.get(account, BananaConfig.Option.QUOTE_REPLIES_TO_DELETED);
    }

    public static boolean commaAfterMention(int account) {
        return BananaConfig.get(account, BananaConfig.Option.COMMA_AFTER_MENTION);
    }

    public static boolean mentionAvatars(int account) {
        return BananaConfig.get(account, BananaConfig.Option.MENTION_AVATARS);
    }

    public static boolean avatarLongPressMention(int account) {
        return BananaConfig.get(account, BananaConfig.Option.AVATAR_LONG_PRESS_MENTION);
    }

    public static boolean hashtagsPreferCurrentChat(int account) {
        return BananaConfig.get(account, BananaConfig.Option.HASHTAGS_PREFER_CURRENT_CHAT);
    }

    public static boolean showOriginalForwardDate(int account) {
        return BananaConfig.get(account, BananaConfig.Option.SHOW_ORIGINAL_FORWARD_DATE);
    }

    public static boolean sendBannedGifsAsVideo(int account) {
        return BananaConfig.get(account, BananaConfig.Option.SEND_BANNED_GIFS_AS_VIDEO);
    }

    public static boolean hideBotAutomation(int account) {
        return BananaConfig.get(account, BananaConfig.Option.HIDE_BOT_AUTOMATION);
    }

    public static boolean showPinnedWithBot(int account) {
        return BananaConfig.get(account, BananaConfig.Option.SHOW_PINNED_WITH_BOT);
    }

    public static boolean hideBirthdayNotifications(int account) {
        return BananaConfig.get(account, BananaConfig.Option.HIDE_BIRTHDAY_NOTIFICATIONS);
    }

    public static boolean disableCustomBackgrounds(int account) {
        return BananaConfig.get(account, BananaConfig.Option.DISABLE_CUSTOM_BACKGROUNDS);
    }

    public static boolean avatarGlowEnabled(int account) {
        return BananaConfig.get(account, BananaConfig.Option.AVATAR_GLOW_ENABLED);
    }

    public static boolean reactionGlowEnabled(int account) {
        return BananaConfig.get(account, BananaConfig.Option.REACTION_GLOW_ENABLED);
    }

    /** Suffix after an inserted @mention: ", " when COMMA_AFTER_MENTION is on. */
    public static String mentionInsertSuffix(int account) {
        return commaAfterMention(account) ? ", " : " ";
    }

    /**
     * Whether webpage previews should be requested for an outgoing text send.
     * REMOVE_LINK_PREVIEWS forces off; FIX_LINK_PREVIEWS forces on.
     */
    public static boolean shouldSearchLinks(int account, boolean searchLinks) {
        if (removeLinkPreviews(account)) {
            return false;
        }
        if (fixLinkPreviews(account)) {
            return true;
        }
        return searchLinks;
    }

    /** Cap for recent stickers list; Integer.MAX_VALUE / 2 when unlimited. */
    public static int recentStickersMaxCount(int account, int serverMax) {
        if (infiniteRecentStickers(account)) {
            return Math.max(serverMax, 2000);
        }
        return serverMax;
    }

    private static final String ROUND_VIDEO_FRONT_KEY = "bananaRoundVideoFrontface";

    /** Initial front-facing preference for round-video camera. */
    public static boolean initialRoundVideoFrontface(int account) {
        if (roundVideoRemembersCamera(account)) {
            return MessagesController.getMainSettings(account).getBoolean(ROUND_VIDEO_FRONT_KEY, !roundVideoStartsWithRear(account));
        }
        return !roundVideoStartsWithRear(account);
    }

    public static void rememberRoundVideoFrontface(int account, boolean frontface) {
        if (!roundVideoRemembersCamera(account)) {
            return;
        }
        MessagesController.getMainSettings(account).edit().putBoolean(ROUND_VIDEO_FRONT_KEY, frontface).apply();
    }

    /**
     * Double-tap action: 0 off, 1 react, 2 reply, 3 repeat, 4 translate, 6 edit.
     * Legacy DOUBLE_TAP_TO_EDIT maps to 6 when the int pref is unset.
     */
    public static int doubleTapAction(int account) {
        return BananaConfig.doubleTapAction(account);
    }

    /** 0 off, 1 bold, 2 italic, 3 underline, 4 strike, 5 spoiler. */
    public static int defaultSendFormatting(int account) {
        return BananaConfig.defaultSendFormatting(account);
    }

    /** 0 Telegram default, 1 alternative/system. */
    public static int translationBackend(int account) {
        return BananaConfig.translationBackend(account);
    }

    public static boolean preferAlternativeTranslation(int account) {
        return translationBackend(account) == 1;
    }

    /** 0 Telegram default, 1 on-device (unlocks local transcription). */
    public static int transcriptionBackend(int account) {
        return BananaConfig.transcriptionBackend(account);
    }

    public static boolean preferOnDeviceTranscription(int account) {
        return BananaOfflineVoice.useFor(account);
    }

    public static int accentColorSaturation(int account) {
        return BananaConfig.accentColorSaturation(account);
    }

    /** Scale HSV saturation of an accent color by ACCENT_COLOR_SATURATION (100 = unchanged). */
    public static int applyAccentSaturation(int account, int color) {
        int sat = accentColorSaturation(account);
        if (sat == 100) {
            return color;
        }
        float[] hsv = new float[3];
        android.graphics.Color.colorToHSV(color, hsv);
        hsv[1] = Math.max(0f, Math.min(1f, hsv[1] * (sat / 100f)));
        return (color & 0xFF000000) | (android.graphics.Color.HSVToColor(hsv) & 0x00FFFFFF);
    }

    public static boolean isAccentColorKey(int key) {
        return key == org.telegram.ui.ActionBar.Theme.key_chat_messagePanelSend
                || key == org.telegram.ui.ActionBar.Theme.key_featuredStickers_addButton
                || key == org.telegram.ui.ActionBar.Theme.key_checkbox
                || key == org.telegram.ui.ActionBar.Theme.key_switchTrackChecked
                || key == org.telegram.ui.ActionBar.Theme.key_actionBarDefault
                || key == org.telegram.ui.ActionBar.Theme.key_chat_outBubble
                || key == org.telegram.ui.ActionBar.Theme.key_windowBackgroundWhiteBlueHeader;
    }

    /**
     * Soft glow paint for avatar / reaction accents when the matching Extra is on.
     * Returns null when glow is disabled.
     */
    public static android.graphics.Paint glowPaint(int account, boolean avatar, int color, android.graphics.Paint paint) {
        if (avatar ? !avatarGlowEnabled(account) : !reactionGlowEnabled(account)) {
            return null;
        }
        int alpha = avatar ? 0x66 : 0x55;
        int glowColor = (color & 0x00FFFFFF) | (alpha << 24);
        float strokeWidth = AndroidUtilities.dp(3);
        boolean update = paint == null || paint.getColor() != glowColor || paint.getStrokeWidth() != strokeWidth;
        if (paint == null) {
            paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        }
        if (update) {
            paint.setStyle(android.graphics.Paint.Style.STROKE);
            paint.setStrokeWidth(strokeWidth);
            paint.setColor(glowColor);
            paint.setShadowLayer(AndroidUtilities.dp(6), 0, 0, (color & 0x00FFFFFF) | 0x88000000);
        }
        return paint;
    }

    /**
     * Ensure default send formatting wraps plain text when no style entities exist.
     * Mutates and returns the same list (creating one if null).
     */
    public static java.util.ArrayList<TLRPC.MessageEntity> applyDefaultSendFormatting(
            int account, CharSequence text, java.util.ArrayList<TLRPC.MessageEntity> entities) {
        int format = defaultSendFormatting(account);
        if (format <= 0 || text == null || text.length() == 0) {
            return entities;
        }
        if (entities != null) {
            for (int i = 0; i < entities.size(); i++) {
                TLRPC.MessageEntity e = entities.get(i);
                if (e instanceof TLRPC.TL_messageEntityBold
                        || e instanceof TLRPC.TL_messageEntityItalic
                        || e instanceof TLRPC.TL_messageEntityUnderline
                        || e instanceof TLRPC.TL_messageEntityStrike
                        || e instanceof TLRPC.TL_messageEntitySpoiler
                        || e instanceof TLRPC.TL_messageEntityCode
                        || e instanceof TLRPC.TL_messageEntityPre) {
                    return entities;
                }
            }
        }
        TLRPC.MessageEntity entity;
        switch (format) {
            case 1:
                entity = new TLRPC.TL_messageEntityBold();
                break;
            case 2:
                entity = new TLRPC.TL_messageEntityItalic();
                break;
            case 3:
                entity = new TLRPC.TL_messageEntityUnderline();
                break;
            case 4:
                entity = new TLRPC.TL_messageEntityStrike();
                break;
            case 5:
                entity = new TLRPC.TL_messageEntitySpoiler();
                break;
            default:
                return entities;
        }
        entity.offset = 0;
        entity.length = text.length();
        if (entities == null) {
            entities = new java.util.ArrayList<>();
        }
        entities.add(0, entity);
        return entities;
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

    /**
     * Soften bubble color when glass bubbles are enabled.
     * Tint controls the fill; MessageDrawable renders the three edge strengths independently.
     */
    public static int applyGlassBubbleColor(int account, int color) {
        if (!glassBubbles(account)) {
            return color;
        }
        return BananaGlassStyle.fillColor(color, BananaConfig.glassBubbleTint(account));
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
        String lower = url.toLowerCase(java.util.Locale.ROOT);
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
            host = host.toLowerCase(java.util.Locale.ROOT);
            if (!"t.me".equals(host) && !"telegram.me".equals(host) && !"telegram.dog".equals(host)
                    && !host.endsWith(".t.me")) {
                return false;
            }
            String path = uri.getPath();
            if (path == null) {
                return false;
            }
            path = path.toLowerCase(java.util.Locale.ROOT);
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
        if (url != null && url.equals(confirmedUrlBypass.get())) {
            confirmedUrlBypass.remove();
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
                        confirmedUrlBypass.set(url);
                        try {
                            proceed.run();
                        } finally {
                            confirmedUrlBypass.remove();
                        }
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
        // Hidden (recently / week / month) and still-online statuses keep the stock text.
        if (expires <= 0 || expires > currentTime) {
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

    public static boolean ayuForward(int account) {
        return BananaConfig.get(account, BananaConfig.Option.AYU_FORWARD);
    }

    public static boolean messageShot(int account) {
        return BananaConfig.get(account, BananaConfig.Option.MESSAGE_SHOT);
    }

    public static boolean sendWithReturnKey(int account) {
        return BananaConfig.get(account, BananaConfig.Option.SEND_WITH_RETURN_KEY);
    }

    public static boolean downloadSpeedBoost(int account) {
        return BananaConfig.get(account, BananaConfig.Option.DOWNLOAD_SPEED_BOOST);
    }

    public static boolean accelerateUpload(int account) {
        return BananaConfig.get(account, BananaConfig.Option.ACCELERATE_UPLOAD);
    }

    public static boolean allowSecretScreenshots(int account) {
        return BananaConfig.get(account, BananaConfig.Option.ALLOW_SECRET_SCREENSHOTS);
    }

    public static boolean allowProtectedScreenshots(int account) {
        return BananaConfig.get(account, BananaConfig.Option.ALLOW_PROTECTED_SCREENSHOTS);
    }

    /** When either screenshot Extra is on, FLAG_SECURE should not block captures. */
    public static boolean allowScreenshotsOverride(int account) {
        return allowSecretScreenshots(account) || allowProtectedScreenshots(account);
    }

    public static boolean instantPasscodeLock(int account) {
        return BananaConfig.get(account, BananaConfig.Option.INSTANT_PASSCODE_LOCK);
    }

    public static boolean showSensitiveContent(int account) {
        return BananaConfig.get(account, BananaConfig.Option.SHOW_SENSITIVE_CONTENT);
    }

    public static boolean quickTranslateButton(int account) {
        return BananaConfig.get(account, BananaConfig.Option.QUICK_TRANSLATE_BUTTON);
    }

    public static boolean repeatMessageMenu(int account) {
        return BananaConfig.get(account, BananaConfig.Option.REPEAT_MESSAGE_MENU);
    }

    public static boolean saveToCloudMenu(int account) {
        return BananaConfig.get(account, BananaConfig.Option.SAVE_TO_CLOUD_MENU);
    }

    public static boolean selectFromAuthor(int account) {
        return BananaConfig.get(account, BananaConfig.Option.SELECT_FROM_AUTHOR);
    }

    public static boolean keepAliveInBackground(int account) {
        return BananaConfig.get(account, BananaConfig.Option.KEEP_ALIVE_IN_BACKGROUND);
    }

    public static boolean powerSaving(int account) {
        return BananaConfig.get(account, BananaConfig.Option.POWER_SAVING);
    }

    public static boolean disableStoryCreation(int account) {
        return BananaConfig.get(account, BananaConfig.Option.DISABLE_STORY_CREATION);
    }

    public static boolean disableStoryCameraSwipe(int account) {
        return BananaConfig.get(account, BananaConfig.Option.DISABLE_STORY_CAMERA_SWIPE);
    }

    public static boolean confirmUnpin(int account) {
        return BananaConfig.get(account, BananaConfig.Option.CONFIRM_UNPIN);
    }

    public static boolean confirmReactionsInUnjoinedChats(int account) {
        return BananaConfig.get(account, BananaConfig.Option.CONFIRM_REACTIONS_IN_UNJOINED_CHATS);
    }

    public static boolean confirmChatPreviewEntry(int account) {
        return BananaConfig.get(account, BananaConfig.Option.CONFIRM_CHAT_PREVIEW_ENTRY);
    }

    public static boolean removePings(int account) {
        return BananaConfig.get(account, BananaConfig.Option.REMOVE_PINGS);
    }

    /** When off (default), suppress analytics TL requests. */
    public static boolean sendTelegramStatistics(int account) {
        return BananaConfig.get(account, BananaConfig.Option.SEND_TELEGRAM_STATISTICS);
    }

    public static boolean integratedTabSearch(int account) {
        return BananaConfig.get(account, BananaConfig.Option.INTEGRATED_TAB_SEARCH);
    }

    public static boolean tabSearchOnLeft(int account) {
        return BananaConfig.get(account, BananaConfig.Option.TAB_SEARCH_ON_LEFT);
    }

    public static boolean recentChatsMenu(int account) {
        return BananaConfig.get(account, BananaConfig.Option.RECENT_CHATS_MENU);
    }

    public static boolean messageBookmarks(int account) {
        return BananaConfig.get(account, BananaConfig.Option.MESSAGE_BOOKMARKS);
    }

    public static boolean compactFolderNames(int account) {
        return BananaConfig.get(account, BananaConfig.Option.COMPACT_FOLDER_NAMES);
    }

    public static boolean ghostSuggestForStories(int account) {
        return BananaConfig.get(account, BananaConfig.Option.GHOST_SUGGEST_FOR_STORIES);
    }

    public static boolean shouldSecureForSecret(int account, boolean isSecret) {
        return isSecret && !allowSecretScreenshots(account);
    }

    public static boolean shouldSecureForProtected(int account, boolean isProtected) {
        return isProtected && !allowProtectedScreenshots(account);
    }

    /** Truncate folder tab labels when COMPACT_FOLDER_NAMES is on. */
    public static String compactFolderLabel(int account, String name) {
        if (!compactFolderNames(account) || TextUtils.isEmpty(name)) {
            return name;
        }
        // Keep leading characters: custom emoji entities are offsets into the original name.
        int end = name.length();
        while (end > 0 && Character.isWhitespace(name.charAt(end - 1))) {
            end--;
        }
        if (end <= 10) {
            return name.substring(0, end);
        }
        int cut = Character.isHighSurrogate(name.charAt(8)) ? 8 : 9;
        return name.substring(0, cut) + "…";
    }

    private static final String BOOKMARKS_KEY = "bananaMessageBookmarks";

    public static java.util.ArrayList<String> getBookmarks(int account) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        if (!messageBookmarks(account)) {
            return out;
        }
        String raw = MessagesController.getMainSettings(account).getString(BOOKMARKS_KEY, "");
        if (TextUtils.isEmpty(raw)) {
            return out;
        }
        for (String part : raw.split(",")) {
            if (!TextUtils.isEmpty(part)) {
                out.add(part);
            }
        }
        return out;
    }

    public static void addBookmark(int account, long dialogId, int messageId) {
        if (!messageBookmarks(account) || messageId <= 0) {
            return;
        }
        String entry = dialogId + ":" + messageId;
        java.util.ArrayList<String> list = getBookmarks(account);
        list.remove(entry);
        list.add(0, entry);
        while (list.size() > 50) {
            list.remove(list.size() - 1);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(list.get(i));
        }
        MessagesController.getMainSettings(account).edit().putString(BOOKMARKS_KEY, sb.toString()).apply();
    }

    public static boolean isAnalyticsRequest(TLObject object) {
        return object instanceof TLRPC.TL_help_saveAppLog;
    }

    public static boolean shouldDropAnalyticsRequest(int account, TLObject object) {
        return !sendTelegramStatistics(account) && isAnalyticsRequest(object);
    }

    public static boolean bananaIslandEnabled(int account) {
        return BananaConfig.get(account, BananaConfig.Option.BANANA_ISLAND_ENABLED);
    }

    public static boolean bananaIslandShowsName(int account) {
        return BananaConfig.get(account, BananaConfig.Option.BANANA_ISLAND_SHOWS_NAME);
    }

    public static boolean bananaIslandFollowsIcon(int account) {
        return BananaConfig.get(account, BananaConfig.Option.BANANA_ISLAND_FOLLOWS_ICON);
    }

    /**
     * iOS sheds optional fork work in Low Power Mode. System power-save and the
     * Banana power-saving extra do the same: boosts stay inside the stock queue.
     */
    private static boolean boostsAllowed(int account) {
        if (powerSaving(account)) {
            return false;
        }
        try {
            android.os.PowerManager pm = (android.os.PowerManager) org.telegram.messenger.ApplicationLoader.applicationContext.getSystemService(android.content.Context.POWER_SERVICE);
            return pm == null || !pm.isPowerSaveMode();
        } catch (Throwable ignore) {
            return true;
        }
    }

    /**
     * Concurrent downloads when the boost Extra is on.
     * Ceiling stays 8. The queue grows by at most one step so it cannot stack
     * on top of the per-file cap of 8 (iOS {@code MultipartFetch.parallelParts}).
     */
    public static int downloadQueueMaxActive(int account, int base) {
        int safe = Math.max(1, base);
        if (!downloadSpeedBoost(account) || !boostsAllowed(account)) {
            return safe;
        }
        return Math.min(8, safe + 1);
    }

    /** Larger download chunks, still bounded by the per-file request cap. */
    public static boolean downloadUseBoostedChunks(int account) {
        return downloadSpeedBoost(account) && boostsAllowed(account);
    }

    /** Concurrent uploads when accelerate Extra is on. Hard cap is 3 files, matching the previous gate. */
    public static int uploadMaxConcurrent(int account) {
        if (!accelerateUpload(account) || !boostsAllowed(account)) {
            return 1;
        }
        return 3;
    }

    /**
     * In-flight upload window in KiB.
     * iOS {@code accelerateUpload} keeps {@code parallelParts} at 8. With a 256 KiB
     * minimum part that is a 2048 KiB window, not an open-ended one.
     */
    public static int uploadMaxKBytes(int account, boolean slowNetwork, int base) {
        if (slowNetwork || !accelerateUpload(account) || !boostsAllowed(account)) {
            return Math.max(1, base);
        }
        return Math.max(base, 256 * 8);
    }

    /** Minimum upload part size in KiB when accelerate Extra is on. */
    public static int uploadMinChunkKb(int account, boolean slowNetwork, int base) {
        if (slowNetwork || !accelerateUpload(account) || !boostsAllowed(account)) {
            return Math.max(1, base);
        }
        return Math.max(base, 256);
    }

    /**
     * Renders a message cell to a PNG and opens a share sheet.
     * Used by MESSAGE_SHOT (defaults on).
     */
    public static void shareMessageShot(android.app.Activity activity, android.view.View cell) {
        if (activity == null || cell == null || cell.getWidth() <= 0 || cell.getHeight() <= 0) {
            return;
        }
        try {
            android.graphics.Bitmap bitmap = AndroidUtilities.snapshotView(cell);
            if (bitmap == null) {
                return;
            }
            java.io.File dir = new java.io.File(org.telegram.messenger.ApplicationLoader.applicationContext.getCacheDir(), "message_shots");
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
            // Earlier shots were already handed to their share targets; do not let the cache grow forever.
            java.io.File[] old = dir.listFiles();
            if (old != null) {
                long cutoff = System.currentTimeMillis() - 60 * 60 * 1000L;
                for (java.io.File file : old) {
                    if (file.lastModified() < cutoff) {
                        //noinspection ResultOfMethodCallIgnored
                        file.delete();
                    }
                }
            }
            java.io.File out = new java.io.File(dir, "msg_" + System.currentTimeMillis() + ".png");
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(out)) {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
            }
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            intent.setType("image/png");
            android.net.Uri uri;
            if (android.os.Build.VERSION.SDK_INT >= 24) {
                uri = androidx.core.content.FileProvider.getUriForFile(
                        activity,
                        org.telegram.messenger.ApplicationLoader.getApplicationId() + ".provider",
                        out);
                intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                uri = android.net.Uri.fromFile(out);
            }
            intent.putExtra(android.content.Intent.EXTRA_STREAM, uri);
            activity.startActivity(android.content.Intent.createChooser(
                    intent, LocaleController.getString(R.string.BananaMessageShot)));
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }
}
