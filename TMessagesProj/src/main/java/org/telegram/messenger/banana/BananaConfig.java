package org.telegram.messenger.banana;

import android.content.SharedPreferences;

import org.telegram.messenger.MessagesController;

/** Account-scoped options use Telegram's existing in-memory preferences store. */
public final class BananaConfig {
    public enum Option {
        CLEAN_TRACKING_LINKS("bananaCleanTrackingLinks"),
        HIDE_ONLINE("bananaHideOnline"),
        HIDE_TYPING("bananaHideTyping"),
        HIDE_RECORDING("bananaHideRecording"),
        HIDE_UPLOAD("bananaHideUpload"),
        DONT_READ_MESSAGES("bananaDontReadMessages"),
        DONT_READ_STORIES("bananaDontReadStories"),
        OFFLINE_AFTER_ACTIVITY("bananaOfflineAfterActivity"),
        SAVE_DELETED("bananaSaveDeleted"),
        SAVE_EDITS("bananaSaveEdits"),
        SAVE_BOT_HISTORY("bananaSaveBotHistory"),
        SAVE_MEDIA_HISTORY("bananaSaveMediaHistory");

        private final String key;
        Option(String key) { this.key = key; }
    }

    private BananaConfig() {}

    public enum SilentSending { NORMAL, IN_GHOST_MODE, ALWAYS }

    private static final Option[] GHOST_OPTIONS = {Option.HIDE_ONLINE, Option.HIDE_TYPING,
            Option.HIDE_RECORDING, Option.HIDE_UPLOAD, Option.DONT_READ_MESSAGES, Option.DONT_READ_STORIES};

    public static boolean isGhostModeActive(int account) {
        for (Option option : GHOST_OPTIONS) if (!get(account, option)) return false;
        return true;
    }

    /** Apply the complete preset in one preference transaction. */
    public static void setGhostMode(int account, boolean enabled) {
        SharedPreferences.Editor editor = MessagesController.getMainSettings(account).edit();
        for (Option option : GHOST_OPTIONS) editor.putBoolean(option.key, enabled);
        editor.apply();
    }

    public static SilentSending silentSending(int account) {
        int value = MessagesController.getMainSettings(account).getInt("bananaSilentSending", 0);
        return value >= 0 && value < SilentSending.values().length ? SilentSending.values()[value] : SilentSending.NORMAL;
    }

    public static void setSilentSending(int account, SilentSending value) {
        MessagesController.getMainSettings(account).edit().putInt("bananaSilentSending", value.ordinal()).apply();
    }

    public static boolean shouldSendSilently(int account) {
        return GhostPolicy.sendSilently(silentSending(account).ordinal(), isGhostModeActive(account));
    }

    public static boolean cleanTrackingLinks(int account) {
        return get(account, Option.CLEAN_TRACKING_LINKS);
    }

    public static boolean get(int account, Option option) {
        return MessagesController.getMainSettings(account).getBoolean(option.key, false);
    }

    public static void set(int account, Option option, boolean enabled) {
        MessagesController.getMainSettings(account).edit().putBoolean(option.key, enabled).apply();
    }

    public static boolean suppressActivity(int account, int action) {
        return GhostPolicy.suppressActivity(action, get(account, Option.HIDE_TYPING),
                get(account, Option.HIDE_RECORDING), get(account, Option.HIDE_UPLOAD),
                get(account, Option.DONT_READ_MESSAGES));
    }
}
