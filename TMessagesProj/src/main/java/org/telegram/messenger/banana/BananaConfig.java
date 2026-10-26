package org.telegram.messenger.banana;

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
        DONT_READ_STORIES("bananaDontReadStories");

        private final String key;
        Option(String key) { this.key = key; }
    }

    private BananaConfig() {}

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
