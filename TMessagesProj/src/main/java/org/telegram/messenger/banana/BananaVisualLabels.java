package org.telegram.messenger.banana;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;

/**
 * Display-only name badge for the signed-in account.
 * Matches iOS {@code BananaVisualLabel} / Fork Extras item 1981:
 * None, SCAM, FAKE, SUPPORT, BOT. Server peer flags are never changed.
 */
public final class BananaVisualLabels {
    public static final int NONE = 0;
    public static final int SCAM = 1;
    public static final int FAKE = 2;
    public static final int SUPPORT = 3;
    public static final int BOT = 4;

    private BananaVisualLabels() {}

    public static int get(int account) {
        long selfId = selfId(account);
        if (selfId == 0) {
            return NONE;
        }
        int value = MessagesController.getMainSettings(account).getInt(key(selfId), NONE);
        return value >= NONE && value <= BOT ? value : NONE;
    }

    public static void set(int account, int value) {
        long selfId = selfId(account);
        if (selfId == 0) {
            return;
        }
        int stored = value >= NONE && value <= BOT ? value : NONE;
        MessagesController.getMainSettings(account).edit().putInt(key(selfId), stored).apply();
    }

    /** Badge letters drawn next to the account name, or null when the label is off. */
    public static String badgeText(int account) {
        switch (get(account)) {
            case SCAM:
                return "SCAM";
            case FAKE:
                return "FAKE";
            case SUPPORT:
                return "SUPPORT";
            case BOT:
                return "BOT";
            default:
                return null;
        }
    }

    public static String labelName(int account) {
        switch (get(account)) {
            case SCAM:
                return LocaleController.getString(R.string.BananaVisualLabelScam);
            case FAKE:
                return LocaleController.getString(R.string.BananaVisualLabelFake);
            case SUPPORT:
                return LocaleController.getString(R.string.BananaVisualLabelSupport);
            case BOT:
                return LocaleController.getString(R.string.BananaVisualLabelBot);
            default:
                return LocaleController.getString(R.string.BananaVisualLabelNone);
        }
    }

    private static long selfId(int account) {
        try {
            return UserConfig.getInstance(account).getClientUserId();
        } catch (Throwable ignore) {
            return 0;
        }
    }

    private static String key(long selfId) {
        return "bananaVisualLabel_" + selfId;
    }
}
