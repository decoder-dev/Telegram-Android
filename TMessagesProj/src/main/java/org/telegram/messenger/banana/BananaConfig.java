package org.telegram.messenger.banana;

import android.content.SharedPreferences;

import org.telegram.messenger.MessagesController;

/** Account-scoped BananaGram Extras. Preference keys follow ForkExtrasSettings where possible. */
public final class BananaConfig {
    public enum Option {
        REMOVE_PINGS("bananaRemovePings"),
        SEND_TELEGRAM_STATISTICS("bananaSendTelegramStatistics"),
        BANANA_ISLAND_ENABLED("bananaIslandEnabled"),
        BANANA_ISLAND_SHOWS_NAME("bananaIslandShowsName"),
        BANANA_ISLAND_FOLLOWS_ICON("bananaIslandFollowsIcon"),
        WIDE_TAB_BAR("bananaWideTabBar"),
        INTEGRATED_TAB_SEARCH("bananaIntegratedTabSearch"),
        TAB_SEARCH_ON_LEFT("bananaTabSearchOnLeft"),
        BOTTOM_CHAT_FOLDERS_ENABLED("bananaBottomChatFoldersEnabled"),
        AVATAR_GLOW_ENABLED("bananaAvatarGlowEnabled"),
        SNOW_ENABLED("bananaSnowEnabled"),
        SHOW_CHANNEL_FORWARD_COUNT("bananaShowChannelForwardCount"),
        REACTION_GLOW_ENABLED("bananaReactionGlowEnabled"),
        SHOW_CONTACTS_TAB("bananaShowContactsTab"),
        DONT_READ_MESSAGES("bananaDontReadMessages"),
        DONT_READ_STORIES("bananaDontReadStories"),
        HIDE_ONLINE("bananaHideOnline"),
        GHOST_DONT_SEND_TYPING("bananaGhostDontSendTyping"),
        OFFLINE_AFTER_ACTIVITY("bananaOfflineAfterActivity"),
        GHOST_READ_ON_INTERACT("bananaGhostReadOnInteract"),
        WARN_BEFORE_STORIES("bananaWarnBeforeStories"),
        INSTANT_PASSCODE_LOCK("bananaInstantPasscodeLock"),
        HIDE_MENTION_NOTIFICATIONS("bananaHideMentionNotifications"),
        HIDE_PINNED_NOTIFICATIONS("bananaHidePinnedNotifications"),
        SESSION_KEYCHAIN_BACKUP("bananaSessionKeychainBackup"),
        COMPACT_CHAT_LIST("bananaCompactChatList"),
        COMPACT_MESSAGE_PREVIEW("bananaCompactMessagePreview"),
        COMPACT_FOLDER_NAMES("bananaCompactFolderNames"),
        HIDE_REACTIONS_BAR("bananaHideReactionsBar"),
        USE_RECENT_EMOJI_IN_REACTIONS("bananaUseRecentEmojiInReactions"),
        SHOW_DC("bananaShowDC"),
        SHOW_PROFILE_ID("bananaShowProfileId"),
        CONFIRM_BEFORE_CALL("bananaConfirmBeforeCall"),
        SEND_WITH_RETURN_KEY("bananaSendWithReturnKey"),
        FORCE_BUILT_IN_MIC("bananaForceBuiltInMic"),
        KEEP_ALIVE_IN_BACKGROUND("bananaKeepAliveInBackground"),
        SCROLL_TO_NEXT_CHAT_DISABLED("bananaScrollToNextChatDisabled"),
        SAVE_DELETED("bananaSaveDeleted"),
        SAVE_EDITS("bananaSaveEdits"),
        SAVE_BOT_HISTORY("bananaSaveBotHistory"),
        SAVE_MEDIA_HISTORY("bananaSaveMediaHistory"),
        PROACTIVE_SAVE_MEDIA("bananaProactiveSaveMedia"),
        AYU_FORWARD("bananaAyuForward"),
        BYPASS_DOWNLOAD_RESTRICTIONS("bananaBypassDownloadRestrictions"),
        LOCAL_PREMIUM("bananaLocalPremium"),
        HIDE_ADS("bananaHideAds"),
        HIDE_BLOCKED_MESSAGES("bananaHideBlockedMessages"),
        ALLOW_SECRET_SCREENSHOTS("bananaAllowSecretScreenshots"),
        EXPIRE_TTL_BUTTON("bananaExpireTtlButton"),
        KEEP_BANNED_CHATS("bananaKeepBannedChats"),
        REGEX_MESSAGE_FILTERS_ENABLED("bananaRegexMessageFiltersEnabled"),
        REGEX_MESSAGE_FILTERS_CASE_INSENSITIVE("bananaRegexMessageFiltersCaseInsensitive"),
        GHOST_SCHEDULE_MESSAGES("bananaGhostScheduleMessages"),
        HIDE_ALL_CHATS("bananaHideAllChats"),
        REMEMBER_LAST_FOLDER("bananaRememberLastFolder"),
        HIDE_TAB_BAR("bananaHideTabBar"),
        SHOW_MESSAGE_SECONDS("bananaShowMessageSeconds"),
        CLEAN_TRACKING_LINKS("bananaCleanTrackingLinks"),
        WIDE_CHANNEL_POSTS("bananaWideChannelPosts"),
        DOUBLE_TAP_TO_EDIT("bananaDoubleTapToEdit"),
        QUICK_TRANSLATE_BUTTON("bananaQuickTranslateButton"),
        SAVE_TO_CLOUD_MENU("bananaSaveToCloudMenu"),
        SELECT_FROM_AUTHOR("bananaSelectFromAuthor"),
        DOWNLOAD_SPEED_BOOST("bananaDownloadSpeedBoost"),
        STREAMER_MODE("bananaStreamerMode"),
        HIDE_STICKER_STATUS("bananaHideStickerStatus"),
        HIDE_STICKER_REPLY_COLORS("bananaHideStickerReplyColors"),
        CHOOSE_FOLDER_AFTER_JOIN("bananaChooseFolderAfterJoin"),
        RECENT_CHATS_MENU("bananaRecentChatsMenu"),
        MESSAGE_BOOKMARKS("bananaMessageBookmarks"),
        LOCAL_GROUP_ALIAS("bananaLocalGroupAlias"),
        DISABLE_STORY_CAMERA_SWIPE("bananaDisableStoryCameraSwipe"),
        JUMP_TO_START_IN_CHAT_MENU("bananaJumpToStartInChatMenu"),
        HIDE_SERVICE_MESSAGES("bananaHideServiceMessages"),
        CONFIRM_REACTIONS_IN_UNJOINED_CHATS("bananaConfirmReactionsInUnjoinedChats"),
        CONFIRM_TELEGRAM_LINKS("bananaConfirmTelegramLinks"),
        CONFIRM_SHARE_LINKS("bananaConfirmShareLinks"),
        CONFIRM_CHAT_PREVIEW_ENTRY("bananaConfirmChatPreviewEntry"),
        DISABLE_GIFT_STARS_REDIRECT("bananaDisableGiftStarsRedirect"),
        HIDE_STICKER_REPLY_EMOJI("bananaHideStickerReplyEmoji"),
        HIDE_STICKER_REPLY_BACKGROUND("bananaHideStickerReplyBackground"),
        HIDE_PREMIUM_REPLY_EMOJI("bananaHidePremiumReplyEmoji"),
        SHOW_CHAT_CREATION_DATE("bananaShowChatCreationDate"),
        PROFILE_ID_BOT_API_FORMAT("bananaProfileIdBotApiFormat"),
        SHOW_MUTUAL_CONTACT("bananaShowMutualContact"),
        HIDE_PREMIUM_STATUSES("bananaHidePremiumStatuses"),
        HIDE_VERIFICATION("bananaHideVerification"),
        HIDE_GIFT_BUTTON("bananaHideGiftButton"),
        VISUAL_PHONE_ENABLED("bananaVisualPhoneEnabled"),
        GHOST_SEND_WITHOUT_SOUND("bananaGhostSendWithoutSound"),
        GHOST_SUGGEST_FOR_STORIES("bananaGhostSuggestForStories"),
        HIDE_STORIES("bananaHideStories"),
        HIDE_ARCHIVE("bananaHideArchive"),
        ALLOW_PROTECTED_SCREENSHOTS("bananaAllowProtectedScreenshots"),
        ACCELERATE_UPLOAD("bananaAccelerateUpload"),
        INFINITE_RECENT_STICKERS("bananaInfiniteRecentStickers"),
        ONLY_ADDED_STICKERS("bananaOnlyAddedStickers"),
        AUTO_PAUSE_MEDIA_IN_BACKGROUND("bananaAutoPauseMediaInBackground"),
        SHOW_MESSAGE_ID("bananaShowMessageId"),
        AUTO_REVEAL_SPOILERS("bananaAutoRevealSpoilers"),
        REPEAT_MESSAGE_MENU("bananaRepeatMessageMenu"),
        STATIC_ROUND_VIDEO_ZOOM("bananaStaticRoundVideoZoom"),
        COMMA_AFTER_MENTION("bananaCommaAfterMention"),
        DISABLE_COLORED_REPLIES("bananaDisableColoredReplies"),
        HIDE_STICKER_CHECKS("bananaHideStickerChecks"),
        MENTION_AVATARS("bananaMentionAvatars"),
        RELATIVE_LAST_SEEN("bananaRelativeLastSeen"),
        DISABLE_CUSTOM_BACKGROUNDS("bananaDisableCustomBackgrounds"),
        CHANNEL_BOTTOM_DISCUSS("bananaChannelBottomDiscuss"),
        HIDE_CHANNEL_BOTTOM_BUTTON("bananaHideChannelBottomButton"),
        HIDE_SHARE_BUTTON("bananaHideShareButton"),
        GLASS_BUBBLES("bananaGlassBubbles"),
        SHOW_DISAPPEARED_GIFTS("bananaShowDisappearedGifts"),
        ROUND_VIDEO_STARTS_WITH_REAR("bananaRoundVideoStartsWithRear"),
        ROUND_VIDEO_REMEMBERS_CAMERA("bananaRoundVideoRemembersCamera"),
        CHAT_LIST_HIDES_CONNECTION_STATUS("bananaChatListHidesConnectionStatus"),
        CHAT_LIST_HIDES_EMOJI_STATUS("bananaChatListHidesEmojiStatus"),
        CHAT_LIST_CENTERS_TITLE("bananaChatListCentersTitle"),
        CHAT_LIST_HIDES_SEARCH("bananaChatListHidesSearch"),
        POLL_RESULTS_BEFORE_VOTING("bananaPollResultsBeforeVoting"),
        EXACT_LAST_SEEN("bananaExactLastSeen"),
        DOWNLOAD_TIK_TOK("bananaDownloadTikTok"),
        DOWNLOAD_YOU_TUBE_SHORTS("bananaDownloadYouTubeShorts"),
        SIGN_DOWNLOADED_MEDIA("bananaSignDownloadedMedia"),
        QUOTE_REPLIES_TO_DELETED("bananaQuoteRepliesToDeleted"),
        EXACT_COUNTS("bananaExactCounts"),
        CONFIRM_VOICE_SEND("bananaConfirmVoiceSend"),
        HIDE_TYPING("bananaHideTyping"),
        HIDE_RECORDING("bananaHideRecording"),
        HIDE_UPLOAD("bananaHideUpload"),
        MESSAGE_SHOT("bananaMessageShot"),
        POWER_SAVING("bananaPowerSaving"),
        SHOWS_OFFLINE_DOT("bananaShowsOfflineDot"),
        HIDE_PAID_REACTIONS("bananaHidePaidReactions"),
        HIDE_VIA_BOT("bananaHideViaBot"),
        REMOVE_LINK_PREVIEWS("bananaRemoveLinkPreviews"),
        HIDE_BIRTHDAY_NOTIFICATIONS("bananaHideBirthdayNotifications"),
        HIDE_BOT_AUTOMATION("bananaHideBotAutomation"),
        SHOW_PINNED_WITH_BOT("bananaShowPinnedWithBot"),
        STOP_AFTER_VOICE("bananaStopAfterVoice"),
        STOP_AFTER_ROUND_VIDEO("bananaStopAfterRoundVideo"),
        HIDE_GREETING_STICKER("bananaHideGreetingSticker"),
        HASHTAGS_PREFER_CURRENT_CHAT("bananaHashtagsPreferCurrentChat"),
        SHOW_ORIGINAL_FORWARD_DATE("bananaShowOriginalForwardDate"),
        SEND_BANNED_GIFS_AS_VIDEO("bananaSendBannedGifsAsVideo"),
        SHOW_SENSITIVE_CONTENT("bananaShowSensitiveContent"),
        DISABLE_STORY_CREATION("bananaDisableStoryCreation"),
        HIDE_CHANNEL_POST_REACTIONS("bananaHideChannelPostReactions"),
        AVATAR_LONG_PRESS_MENTION("bananaAvatarLongPressMention"),
        FIX_LINK_PREVIEWS("bananaFixLinkPreviews"),
        SAVE_STICKER_TO_PHOTOS("bananaSaveStickerToPhotos"),
        SHOW_MEDIA_INFO("bananaShowMediaInfo"),
        INVITE_LINKS_BULK_REVOKE("bananaInviteLinksBulkRevoke"),
        HIDE_FOLDER_UNREAD_COUNT("bananaHideFolderUnreadCount"),
        CONFIRM_UNPIN("bananaConfirmUnpin"),
        HIDE_PRIVATE_CHAT_ACTIVITIES("bananaHidePrivateChatActivities"),
        STAY_AT_LATEST_MESSAGE_AFTER_REFRESH("bananaStayAtLatestMessageAfterRefresh");

        public final String key;
        Option(String key) { this.key = key; }
    }

    private BananaConfig() {}

    public enum SilentSending { NORMAL, IN_GHOST_MODE, ALWAYS }

    private static final Option[] GHOST_OPTIONS = {
            Option.DONT_READ_MESSAGES, Option.DONT_READ_STORIES, Option.HIDE_ONLINE,
            Option.HIDE_TYPING, Option.HIDE_RECORDING, Option.HIDE_UPLOAD
    };

    public static boolean isGhostModeActive(int account) {
        for (Option option : GHOST_OPTIONS) {
            if (!get(account, option)) {
                return false;
            }
        }
        return true;
    }

    public static void setGhostMode(int account, boolean enabled) {
        SharedPreferences.Editor editor = MessagesController.getMainSettings(account).edit();
        for (Option option : GHOST_OPTIONS) {
            editor.putBoolean(option.key, enabled);
        }
        editor.putBoolean(Option.GHOST_DONT_SEND_TYPING.key, enabled);
        editor.putBoolean(Option.OFFLINE_AFTER_ACTIVITY.key, enabled);
        editor.apply();
        // Keep overlapping AyuGram ghost prefs aligned with Banana as source of truth.
        try {
            com.radolyn.ayugram.AyuConfig.sendReadPackets = !enabled;
            com.radolyn.ayugram.AyuConfig.sendOnlinePackets = !enabled;
            com.radolyn.ayugram.AyuConfig.sendUploadProgress = !enabled;
            com.radolyn.ayugram.AyuConfig.sendOfflinePacketAfterOnline = enabled;
            if (com.radolyn.ayugram.AyuConfig.editor != null) {
                com.radolyn.ayugram.AyuConfig.editor
                        .putBoolean("sendReadPackets", !enabled)
                        .putBoolean("sendOnlinePackets", !enabled)
                        .putBoolean("sendUploadProgress", !enabled)
                        .putBoolean("sendOfflinePacketAfterOnline", enabled)
                        .apply();
            }
        } catch (Throwable ignored) {
        }
    }

    public static SilentSending silentSending(int account) {
        int value = MessagesController.getMainSettings(account).getInt("bananaSilentSending", 0);
        return value >= 0 && value < SilentSending.values().length ? SilentSending.values()[value] : SilentSending.NORMAL;
    }

    public static void setSilentSending(int account, SilentSending value) {
        MessagesController.getMainSettings(account).edit().putInt("bananaSilentSending", value.ordinal()).apply();
    }

    public static boolean shouldSendSilently(int account) {
        return GhostPolicy.sendSilently(silentSending(account).ordinal(), isGhostModeActive(account))
                || (get(account, Option.GHOST_SEND_WITHOUT_SOUND) && isGhostModeActive(account));
    }

    public static boolean cleanTrackingLinks(int account) {
        return get(account, Option.CLEAN_TRACKING_LINKS);
    }

    public static boolean get(int account, Option option) {
        return MessagesController.getMainSettings(account).getBoolean(option.key, defaultFor(option));
    }

    public static void set(int account, Option option, boolean enabled) {
        MessagesController.getMainSettings(account).edit().putBoolean(option.key, enabled).apply();
    }

    public static boolean suppressActivity(int account, int action) {
        boolean hideTyping = get(account, Option.HIDE_TYPING) || get(account, Option.GHOST_DONT_SEND_TYPING);
        boolean hideRecording = get(account, Option.HIDE_RECORDING) || get(account, Option.GHOST_DONT_SEND_TYPING);
        boolean hideUpload = get(account, Option.HIDE_UPLOAD) || get(account, Option.GHOST_DONT_SEND_TYPING);
        return GhostPolicy.suppressActivity(action, hideTyping, hideRecording, hideUpload,
                get(account, Option.DONT_READ_MESSAGES));
    }

    private static boolean defaultFor(Option option) {
        switch (option) {
            case HIDE_ADS:
            case JUMP_TO_START_IN_CHAT_MENU:
            case CONFIRM_SHARE_LINKS:
            case MESSAGE_SHOT:
            case AYU_FORWARD:
                return true;
            default:
                return false;
        }
    }
}
