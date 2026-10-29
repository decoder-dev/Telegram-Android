package org.telegram.messenger.banana;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.telegram.messenger.R;
import org.telegram.messenger.banana.BananaConfig.Option;

/**
 * Hub sections for BananaGram Extras, in the iOS ForkExtrasController order. Every Option sits in
 * exactly one group; groups give long sections their own headers.
 */
public final class BananaExtrasCatalog {
    public static final String[] HUB_ORDER = {
            "NINJA", "PRIVACY", "APPEARANCE", "CHAT_LIST", "TAB_BAR", "MESSAGES", "MEDIA", "PROFILE", "NETWORK"
    };

    /** A titled run of switches inside a section. title is 0 for a section with a single group. */
    public static final class Group {
        public final int title;
        public final Option[] options;

        Group(int title, Option... options) {
            this.title = title;
            this.options = options;
        }
    }

    private static final Map<String, Group[]> SECTIONS;
    static {
        Map<String, Group[]> map = new LinkedHashMap<>();
        // Ninja and Ghost are one section: ghost activity first, then the archive and filters.
        map.put("NINJA", new Group[] {
                new Group(R.string.BananaGroup_Activity, Option.DONT_READ_MESSAGES, Option.HIDE_ONLINE, Option.OFFLINE_AFTER_ACTIVITY, Option.GHOST_READ_ON_INTERACT, Option.GHOST_DONT_SEND_TYPING, Option.HIDE_TYPING, Option.HIDE_RECORDING, Option.HIDE_UPLOAD),
                new Group(R.string.BananaGroup_Stories, Option.DONT_READ_STORIES, Option.WARN_BEFORE_STORIES, Option.GHOST_SUGGEST_FOR_STORIES),
                new Group(R.string.BananaGroup_Sending, Option.GHOST_SCHEDULE_MESSAGES, Option.GHOST_SEND_WITHOUT_SOUND),
                new Group(R.string.BananaGroup_Archive, Option.SAVE_DELETED, Option.SAVE_EDITS, Option.SAVE_BOT_HISTORY, Option.SAVE_MEDIA_HISTORY, Option.PROACTIVE_SAVE_MEDIA, Option.QUOTE_REPLIES_TO_DELETED),
                new Group(R.string.BananaGroup_Content, Option.AYU_FORWARD, Option.BYPASS_DOWNLOAD_RESTRICTIONS, Option.SHOW_SENSITIVE_CONTENT, Option.HIDE_ADS, Option.HIDE_BLOCKED_MESSAGES),
                new Group(R.string.BananaGroup_Filters, Option.REGEX_MESSAGE_FILTERS_ENABLED, Option.REGEX_MESSAGE_FILTERS_CASE_INSENSITIVE)
        });
        map.put("PRIVACY", new Group[] {
                new Group(R.string.BananaGroup_LockScreen, Option.INSTANT_PASSCODE_LOCK, Option.STREAMER_MODE, Option.ALLOW_SECRET_SCREENSHOTS, Option.ALLOW_PROTECTED_SCREENSHOTS, Option.EXPIRE_TTL_BUTTON),
                new Group(R.string.BananaGroup_Notifications, Option.HIDE_MENTION_NOTIFICATIONS, Option.HIDE_PINNED_NOTIFICATIONS),
                new Group(R.string.BananaGroup_Confirmations, Option.CONFIRM_SHARE_LINKS, Option.CONFIRM_TELEGRAM_LINKS, Option.CONFIRM_CHAT_PREVIEW_ENTRY, Option.CONFIRM_REACTIONS_IN_UNJOINED_CHATS),
                new Group(R.string.BananaGroup_Data, Option.SESSION_KEYCHAIN_BACKUP, Option.KEEP_BANNED_CHATS, Option.REMOVE_PINGS, Option.SEND_TELEGRAM_STATISTICS)
        });
        map.put("APPEARANCE", new Group[] {
                new Group(R.string.BananaGroup_Island, Option.BANANA_ISLAND_ENABLED, Option.BANANA_ISLAND_SHOWS_NAME, Option.BANANA_ISLAND_FOLLOWS_ICON),
                new Group(R.string.BananaGroup_Effects, Option.GLASS_BUBBLES, Option.AVATAR_GLOW_ENABLED, Option.REACTION_GLOW_ENABLED, Option.SNOW_ENABLED, Option.POWER_SAVING),
                new Group(R.string.BananaGroup_ChatLook, Option.DISABLE_CUSTOM_BACKGROUNDS, Option.DISABLE_COLORED_REPLIES, Option.WIDE_CHANNEL_POSTS)
        });
        map.put("CHAT_LIST", new Group[] {
                new Group(R.string.BananaGroup_ListLayout, Option.COMPACT_CHAT_LIST, Option.COMPACT_MESSAGE_PREVIEW, Option.CHAT_LIST_CENTERS_TITLE, Option.CHAT_LIST_HIDES_SEARCH, Option.CHAT_LIST_HIDES_CONNECTION_STATUS, Option.CHAT_LIST_HIDES_EMOJI_STATUS, Option.HIDE_STORIES, Option.HIDE_ARCHIVE, Option.HIDE_PRIVATE_CHAT_ACTIVITIES, Option.HIDE_BIRTHDAY_NOTIFICATIONS, Option.STAY_AT_LATEST_MESSAGE_AFTER_REFRESH, Option.RECENT_CHATS_MENU),
                new Group(R.string.BananaGroup_Folders, Option.COMPACT_FOLDER_NAMES, Option.HIDE_ALL_CHATS, Option.REMEMBER_LAST_FOLDER, Option.HIDE_FOLDER_UNREAD_COUNT, Option.CHOOSE_FOLDER_AFTER_JOIN)
        });
        map.put("TAB_BAR", new Group[] {
                new Group(0, Option.WIDE_TAB_BAR, Option.INTEGRATED_TAB_SEARCH, Option.TAB_SEARCH_ON_LEFT, Option.BOTTOM_CHAT_FOLDERS_ENABLED, Option.SHOW_CONTACTS_TAB, Option.HIDE_TAB_BAR)
        });
        map.put("MESSAGES", new Group[] {
                new Group(R.string.BananaGroup_Display, Option.SHOW_MESSAGE_SECONDS, Option.SHOW_MESSAGE_ID, Option.SHOW_ORIGINAL_FORWARD_DATE, Option.EXACT_COUNTS, Option.SHOW_CHANNEL_FORWARD_COUNT, Option.AUTO_REVEAL_SPOILERS, Option.POLL_RESULTS_BEFORE_VOTING, Option.HIDE_SERVICE_MESSAGES, Option.HIDE_GREETING_STICKER, Option.HIDE_VIA_BOT, Option.HIDE_BOT_AUTOMATION, Option.SHOW_PINNED_WITH_BOT, Option.MENTION_AVATARS),
                new Group(R.string.BananaGroup_Composer, Option.CLEAN_TRACKING_LINKS, Option.REMOVE_LINK_PREVIEWS, Option.FIX_LINK_PREVIEWS, Option.COMMA_AFTER_MENTION, Option.AVATAR_LONG_PRESS_MENTION, Option.HASHTAGS_PREFER_CURRENT_CHAT, Option.SEND_BANNED_GIFS_AS_VIDEO, Option.DOUBLE_TAP_TO_EDIT, Option.QUICK_TRANSLATE_BUTTON),
                new Group(R.string.BananaGroup_MessageMenu, Option.SAVE_TO_CLOUD_MENU, Option.SELECT_FROM_AUTHOR, Option.REPEAT_MESSAGE_MENU, Option.JUMP_TO_START_IN_CHAT_MENU, Option.MESSAGE_BOOKMARKS, Option.MESSAGE_SHOT, Option.LOCAL_GROUP_ALIAS, Option.CONFIRM_UNPIN, Option.INVITE_LINKS_BULK_REVOKE),
                new Group(R.string.BananaGroup_Reactions, Option.HIDE_REACTIONS_BAR, Option.USE_RECENT_EMOJI_IN_REACTIONS, Option.HIDE_PAID_REACTIONS, Option.HIDE_CHANNEL_POST_REACTIONS, Option.HIDE_STICKER_STATUS, Option.HIDE_STICKER_REPLY_COLORS, Option.HIDE_STICKER_REPLY_EMOJI, Option.HIDE_STICKER_REPLY_BACKGROUND, Option.HIDE_PREMIUM_REPLY_EMOJI, Option.HIDE_STICKER_CHECKS, Option.INFINITE_RECENT_STICKERS, Option.ONLY_ADDED_STICKERS, Option.SAVE_STICKER_TO_PHOTOS),
                new Group(R.string.BananaGroup_Channels, Option.CHANNEL_BOTTOM_DISCUSS, Option.HIDE_CHANNEL_BOTTOM_BUTTON, Option.HIDE_SHARE_BUTTON, Option.SCROLL_TO_NEXT_CHAT_DISABLED, Option.DISABLE_GIFT_STARS_REDIRECT, Option.HIDE_GIFT_BUTTON)
        });
        map.put("MEDIA", new Group[] {
                new Group(R.string.BananaGroup_CallsInput, Option.CONFIRM_BEFORE_CALL, Option.FORCE_BUILT_IN_MIC, Option.SEND_WITH_RETURN_KEY, Option.CONFIRM_VOICE_SEND),
                new Group(R.string.BananaGroup_Playback, Option.AUTO_PAUSE_MEDIA_IN_BACKGROUND, Option.STOP_AFTER_VOICE, Option.STOP_AFTER_ROUND_VIDEO, Option.STATIC_ROUND_VIDEO_ZOOM, Option.ROUND_VIDEO_STARTS_WITH_REAR, Option.ROUND_VIDEO_REMEMBERS_CAMERA),
                new Group(R.string.BananaGroup_Downloads, Option.DOWNLOAD_TIK_TOK, Option.DOWNLOAD_YOU_TUBE_SHORTS, Option.SIGN_DOWNLOADED_MEDIA, Option.SHOW_MEDIA_INFO),
                new Group(R.string.BananaGroup_Stories, Option.DISABLE_STORY_CAMERA_SWIPE, Option.DISABLE_STORY_CREATION)
        });
        map.put("PROFILE", new Group[] {
                new Group(R.string.BananaGroup_ProfileInfo, Option.SHOW_DC, Option.SHOW_PROFILE_ID, Option.PROFILE_ID_BOT_API_FORMAT, Option.SHOW_CHAT_CREATION_DATE, Option.SHOW_MUTUAL_CONTACT, Option.SHOW_DISAPPEARED_GIFTS),
                new Group(R.string.BananaGroup_Status, Option.EXACT_LAST_SEEN, Option.RELATIVE_LAST_SEEN, Option.SHOWS_OFFLINE_DOT, Option.HIDE_PREMIUM_STATUSES, Option.HIDE_VERIFICATION),
                new Group(R.string.BananaGroup_Local, Option.VISUAL_PHONE_ENABLED, Option.LOCAL_PREMIUM)
        });
        map.put("NETWORK", new Group[] {
                new Group(0, Option.KEEP_ALIVE_IN_BACKGROUND, Option.DOWNLOAD_SPEED_BOOST, Option.ACCELERATE_UPLOAD)
        });
        SECTIONS = Collections.unmodifiableMap(map);
    }

    private BananaExtrasCatalog() {}

    public static Group[] groupsFor(String section) {
        Group[] groups = SECTIONS.get(section);
        return groups != null ? groups : new Group[0];
    }

    public static Option[] optionsFor(String section) {
        ArrayList<Option> options = new ArrayList<>();
        for (Group group : groupsFor(section)) Collections.addAll(options, group.options);
        return options.toArray(new Option[0]);
    }
}
