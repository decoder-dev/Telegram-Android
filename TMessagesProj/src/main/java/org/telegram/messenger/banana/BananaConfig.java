package org.telegram.messenger.banana;

import android.content.SharedPreferences;

import com.radolyn.ayugram.AyuConfig;

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
        // Every request hook checks these per-account options itself; AyuGram's process-wide
        // ghost flags are left to AyuGram's own screen so one account never ghosts another.
    }

    private static final String GHOST_SCOPE_MIGRATED = "bananaGhostScopeMigrated";

    /**
     * Earlier builds mirrored Banana ghost and local Premium switches into AyuGram's process-wide
     * flags, so turning one on for one account also applied it to every other account. Once per
     * install, reset each Ayu
     * flag that a Banana option in some account explains back to Ayu's default; the Banana
     * option keeps covering the account it was turned on for.
     */
    public static void migrateGhostScope() {
        // Runs from Application.onCreate: read the account files directly instead of creating
        // a MessagesController for every account slot.
        SharedPreferences global = accountPreferences(0);
        if (global.getBoolean(GHOST_SCOPE_MIGRATED, false) || AyuConfig.editor == null) {
            return;
        }
        SharedPreferences.Editor ayu = AyuConfig.editor;
        if (!AyuConfig.sendReadPackets && anyAccount(Option.DONT_READ_MESSAGES)) {
            ayu.putBoolean("sendReadPackets", AyuConfig.sendReadPackets = true);
        }
        if (!AyuConfig.sendOnlinePackets && anyAccount(Option.HIDE_ONLINE)) {
            ayu.putBoolean("sendOnlinePackets", AyuConfig.sendOnlinePackets = true);
        }
        if (!AyuConfig.sendUploadProgress && (anyAccount(Option.GHOST_DONT_SEND_TYPING) || anyAccount(Option.HIDE_UPLOAD))) {
            ayu.putBoolean("sendUploadProgress", AyuConfig.sendUploadProgress = true);
        }
        if (AyuConfig.sendOfflinePacketAfterOnline && anyAccount(Option.OFFLINE_AFTER_ACTIVITY)) {
            ayu.putBoolean("sendOfflinePacketAfterOnline", AyuConfig.sendOfflinePacketAfterOnline = false);
        }
        if (AyuConfig.useScheduledMessages && anyAccount(Option.GHOST_SCHEDULE_MESSAGES)) {
            ayu.putBoolean("useScheduledMessages", AyuConfig.useScheduledMessages = false);
        }
        if (AyuConfig.localPremium && anyAccount(Option.LOCAL_PREMIUM)) {
            ayu.putBoolean("localPremium", AyuConfig.localPremium = false);
        }
        ayu.apply();
        global.edit().putBoolean(GHOST_SCOPE_MIGRATED, true).apply();
    }

    private static boolean anyAccount(Option option) {
        for (int account = 0; account < org.telegram.messenger.UserConfig.MAX_ACCOUNT_COUNT; account++) {
            if (accountPreferences(account).getBoolean(option.key, defaultFor(option))) {
                return true;
            }
        }
        return false;
    }

    /** Same files as MessagesController.getMainSettings(account). */
    private static SharedPreferences accountPreferences(int account) {
        return org.telegram.messenger.ApplicationLoader.applicationContext.getSharedPreferences(
                account == 0 ? "mainconfig" : "mainconfig" + account, android.content.Context.MODE_PRIVATE);
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

    public static int getThemePalette(int account) {
        SharedPreferences prefs = MessagesController.getMainSettings(account);
        int stored = prefs.getInt("bananaThemePalette", 1); // Banana Cream on a fresh install.
        int palette = BananaThemePolicy.paletteOrdinal(stored);
        if (stored != palette) {
            SharedPreferences.Editor editor = prefs.edit().putInt("bananaThemePalette", palette);
            if (stored == 4 || stored == 5) editor.putBoolean(Option.GLASS_BUBBLES.key, true);
            editor.apply();
        }
        return palette;
    }

    public static void setThemePalette(int account, int palette) {
        MessagesController.getMainSettings(account).edit().putInt("bananaThemePalette", BananaThemePolicy.paletteOrdinal(palette)).apply();
    }

    public static boolean get(int account, Option option) {
        return MessagesController.getMainSettings(account).getBoolean(option.key, defaultFor(option));
    }

    public static void set(int account, Option option, boolean enabled) {
        SharedPreferences.Editor editor = MessagesController.getMainSettings(account).edit().putBoolean(option.key, enabled);
        if (option == Option.DOUBLE_TAP_TO_EDIT) {
            // The switch is a shortcut for the "Edit" double-tap action; both read one value.
            int action = getInt(account, IntValue.DOUBLE_TAP_ACTION);
            editor.putInt(IntValue.DOUBLE_TAP_ACTION.key, enabled ? 6 : action == 6 ? 0 : action);
        }
        editor.apply();
        if (option == Option.KEEP_ALIVE_IN_BACKGROUND && account == org.telegram.messenger.UserConfig.selectedAccount) {
            org.telegram.messenger.AndroidUtilities.runOnUIThread(org.telegram.messenger.ApplicationLoader::startPushService);
        }
    }

    private static final String LAST_FOLDER_STABLE_ID = "bananaLastFolderStableId";

    /** Stable id of the last selected folder tab; -1 means unset / All chats. */
    public static int getLastFolderStableId(int account) {
        return MessagesController.getMainSettings(account).getInt(LAST_FOLDER_STABLE_ID, -1);
    }

    public static void setLastFolderStableId(int account, int stableId) {
        MessagesController.getMainSettings(account).edit().putInt(LAST_FOLDER_STABLE_ID, stableId).apply();
    }

    public static boolean suppressActivity(int account, int action) {
        boolean hideTyping = get(account, Option.HIDE_TYPING) || get(account, Option.GHOST_DONT_SEND_TYPING);
        boolean hideRecording = get(account, Option.HIDE_RECORDING) || get(account, Option.GHOST_DONT_SEND_TYPING);
        boolean hideUpload = get(account, Option.HIDE_UPLOAD) || get(account, Option.GHOST_DONT_SEND_TYPING);
        return GhostPolicy.suppressActivity(action, hideTyping, hideRecording, hideUpload,
                get(account, Option.DONT_READ_MESSAGES));
    }

    /** Non-boolean ForkExtrasSettings values (ints / strings / enum backends). */
    public enum IntValue {
        ACCENT_COLOR_SATURATION("bananaAccentColorSaturation", 100),
        STICKER_SIZE_PERCENT("bananaStickerSizePercent", 100),
        OUTGOING_PHOTO_QUALITY("bananaOutgoingPhotoQuality", 0),
        DOUBLE_TAP_SEEK_SECONDS("bananaDoubleTapSeekSeconds", 15),
        DOUBLE_TAP_ACTION("bananaDoubleTapAction", 0),
        STICKER_SHAPE("bananaStickerShape", 0),
        GLASS_BUBBLE_TINT("bananaGlassBubbleTint", 25),
        GLASS_BUBBLE_RIM_TOP("bananaGlassBubbleRimTop", 95),
        GLASS_BUBBLE_RIM_SIDES("bananaGlassBubbleRimSides", 20),
        GLASS_BUBBLE_RIM_BOTTOM("bananaGlassBubbleRimBottom", 55),
        DEFAULT_SEND_FORMATTING("bananaDefaultSendFormatting", 0),
        DEFAULT_VIDEO_QUALITY("bananaDefaultVideoQuality", 0),
        TRANSLATION_BACKEND("bananaTranslationBackend", 0),
        TRANSCRIPTION_BACKEND("bananaTranscriptionBackend", 0);

        public final String key;
        public final int defaultValue;
        IntValue(String key, int defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }
    }

    public enum StringValue {
        DELETED_MESSAGE_MARK("bananaDeletedMessageMark", "🧹"),
        EDITED_MESSAGE_MARK("bananaEditedMessageMark", ""),
        VISUAL_PHONE_NUMBER("bananaVisualPhoneNumber", "");

        public final String key;
        public final String defaultValue;
        StringValue(String key, String defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }
    }

    private static final int[] DOUBLE_TAP_SEEK_ALLOWED = {0, 5, 10, 15, 30};
    private static final int[] DOUBLE_TAP_ACTION_ALLOWED = {0, 1, 2, 3, 4, 6};

    public static int getInt(int account, IntValue value) {
        SharedPreferences prefs = MessagesController.getMainSettings(account);
        int fallback = value.defaultValue;
        if (value == IntValue.DOUBLE_TAP_ACTION && !prefs.contains(value.key) && get(account, Option.DOUBLE_TAP_TO_EDIT)) {
            fallback = 6;
        }
        return clampInt(value, prefs.getInt(value.key, fallback));
    }

    public static void setInt(int account, IntValue value, int raw) {
        MessagesController.getMainSettings(account).edit().putInt(value.key, clampInt(value, raw)).apply();
        if (value == IntValue.DOUBLE_TAP_ACTION) {
            set(account, Option.DOUBLE_TAP_TO_EDIT, clampInt(value, raw) == 6);
        }
    }

    public static String getString(int account, StringValue value) {
        String stored = MessagesController.getMainSettings(account).getString(value.key, value.defaultValue);
        return stored != null ? stored : value.defaultValue;
    }

    public static void setString(int account, StringValue value, String raw) {
        String next = raw != null ? raw : value.defaultValue;
        if ((value == StringValue.DELETED_MESSAGE_MARK || value == StringValue.EDITED_MESSAGE_MARK) && next.length() > 8) {
            // Never cut an emoji's surrogate pair in half.
            next = next.substring(0, Character.isHighSurrogate(next.charAt(7)) ? 7 : 8);
        }
        MessagesController.getMainSettings(account).edit().putString(value.key, next).apply();
        if (value == StringValue.DELETED_MESSAGE_MARK) {
            syncAyuMark("deletedMarkText", next);
        } else if (value == StringValue.EDITED_MESSAGE_MARK) {
            syncAyuMark("editedMarkText", next);
        }
    }

    public static int accentColorSaturation(int account) { return getInt(account, IntValue.ACCENT_COLOR_SATURATION); }
    public static void setAccentColorSaturation(int account, int value) { setInt(account, IntValue.ACCENT_COLOR_SATURATION, value); }

    public static int stickerSizePercent(int account) { return getInt(account, IntValue.STICKER_SIZE_PERCENT); }
    public static void setStickerSizePercent(int account, int value) { setInt(account, IntValue.STICKER_SIZE_PERCENT, value); }

    public static int outgoingPhotoQuality(int account) { return getInt(account, IntValue.OUTGOING_PHOTO_QUALITY); }
    public static void setOutgoingPhotoQuality(int account, int value) { setInt(account, IntValue.OUTGOING_PHOTO_QUALITY, value); }

    /** Max side length for outgoing photos: 0→1280, 1→1920, 2→2560. */
    public static int outgoingPhotoMaxSide(int account) {
        switch (outgoingPhotoQuality(account)) {
            case 1: return 1920;
            case 2: return 2560;
            default: return 1280;
        }
    }

    public static int doubleTapSeekSeconds(int account) { return getInt(account, IntValue.DOUBLE_TAP_SEEK_SECONDS); }
    public static void setDoubleTapSeekSeconds(int account, int value) { setInt(account, IntValue.DOUBLE_TAP_SEEK_SECONDS, value); }

    public static int doubleTapAction(int account) { return getInt(account, IntValue.DOUBLE_TAP_ACTION); }
    public static void setDoubleTapAction(int account, int value) { setInt(account, IntValue.DOUBLE_TAP_ACTION, value); }

    public static int stickerShape(int account) { return getInt(account, IntValue.STICKER_SHAPE); }
    public static void setStickerShape(int account, int value) { setInt(account, IntValue.STICKER_SHAPE, value); }

    public static int glassBubbleTint(int account) { return getInt(account, IntValue.GLASS_BUBBLE_TINT); }
    public static void setGlassBubbleTint(int account, int value) { setInt(account, IntValue.GLASS_BUBBLE_TINT, value); }

    public static int glassBubbleRimTop(int account) { return getInt(account, IntValue.GLASS_BUBBLE_RIM_TOP); }
    public static void setGlassBubbleRimTop(int account, int value) { setInt(account, IntValue.GLASS_BUBBLE_RIM_TOP, value); }

    public static int glassBubbleRimSides(int account) { return getInt(account, IntValue.GLASS_BUBBLE_RIM_SIDES); }
    public static void setGlassBubbleRimSides(int account, int value) { setInt(account, IntValue.GLASS_BUBBLE_RIM_SIDES, value); }

    public static int glassBubbleRimBottom(int account) { return getInt(account, IntValue.GLASS_BUBBLE_RIM_BOTTOM); }
    public static void setGlassBubbleRimBottom(int account, int value) { setInt(account, IntValue.GLASS_BUBBLE_RIM_BOTTOM, value); }

    public static int defaultSendFormatting(int account) { return getInt(account, IntValue.DEFAULT_SEND_FORMATTING); }
    public static void setDefaultSendFormatting(int account, int value) { setInt(account, IntValue.DEFAULT_SEND_FORMATTING, value); }

    public static int defaultVideoQuality(int account) { return getInt(account, IntValue.DEFAULT_VIDEO_QUALITY); }
    public static void setDefaultVideoQuality(int account, int value) { setInt(account, IntValue.DEFAULT_VIDEO_QUALITY, value); }

    /** 0 = Telegram default, 1 = system. */
    public static int translationBackend(int account) { return getInt(account, IntValue.TRANSLATION_BACKEND); }
    public static void setTranslationBackend(int account, int value) { setInt(account, IntValue.TRANSLATION_BACKEND, value); }

    /** 0 = Telegram default, 1 = on-device Russian model, 2 = on-device English model. */
    public static int transcriptionBackend(int account) { return getInt(account, IntValue.TRANSCRIPTION_BACKEND); }
    public static void setTranscriptionBackend(int account, int value) { setInt(account, IntValue.TRANSCRIPTION_BACKEND, value); }

    public static String deletedMessageMark(int account) { return getString(account, StringValue.DELETED_MESSAGE_MARK); }
    public static void setDeletedMessageMark(int account, String value) { setString(account, StringValue.DELETED_MESSAGE_MARK, value); }

    public static String editedMessageMark(int account) { return getString(account, StringValue.EDITED_MESSAGE_MARK); }
    public static void setEditedMessageMark(int account, String value) { setString(account, StringValue.EDITED_MESSAGE_MARK, value); }

    public static String visualPhoneNumber(int account) { return getString(account, StringValue.VISUAL_PHONE_NUMBER); }
    public static void setVisualPhoneNumber(int account, String value) { setString(account, StringValue.VISUAL_PHONE_NUMBER, value); }

    private static int clampInt(IntValue value, int raw) {
        switch (value) {
            case ACCENT_COLOR_SATURATION:
            case GLASS_BUBBLE_TINT:
            case GLASS_BUBBLE_RIM_TOP:
            case GLASS_BUBBLE_RIM_SIDES:
            case GLASS_BUBBLE_RIM_BOTTOM:
                return Math.max(0, Math.min(100, raw));
            case STICKER_SIZE_PERCENT:
                return Math.max(50, Math.min(150, raw));
            case OUTGOING_PHOTO_QUALITY:
            case STICKER_SHAPE:
            case DEFAULT_VIDEO_QUALITY:
                return Math.max(0, Math.min(2, raw));
            case TRANSCRIPTION_BACKEND:
                return Math.max(0, Math.min(2, raw));
            case TRANSLATION_BACKEND:
                return Math.max(0, Math.min(1, raw));
            case DEFAULT_SEND_FORMATTING:
                return Math.max(0, Math.min(5, raw));
            case DOUBLE_TAP_SEEK_SECONDS:
                return nearestAllowed(raw, DOUBLE_TAP_SEEK_ALLOWED, 15);
            case DOUBLE_TAP_ACTION:
                return nearestAllowed(raw, DOUBLE_TAP_ACTION_ALLOWED, 0);
            default:
                return raw;
        }
    }

    private static int nearestAllowed(int raw, int[] allowed, int fallback) {
        for (int value : allowed) {
            if (value == raw) {
                return raw;
            }
        }
        return fallback;
    }

    private static void syncAyuMark(String key, String value) {
        try {
            if (AyuConfig.editor != null) {
                AyuConfig.editor.putString(key, value).apply();
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean defaultFor(Option option) {
        switch (option) {
            case HIDE_ADS:
            case JUMP_TO_START_IN_CHAT_MENU:
            case CONFIRM_SHARE_LINKS:
            case MESSAGE_SHOT:
            case AYU_FORWARD:
            case SHOW_CONTACTS_TAB:
            case MENTION_AVATARS:
                return true;
            default:
                return false;
        }
    }
}
