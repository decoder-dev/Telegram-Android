package org.telegram.messenger.banana;

/**
 * Ten account slots for everyone (UserConfig.MAX_ACCOUNT_COUNT). Things upstream sized for
 * three to five accounts live here.
 */
public final class AccountSlots {
    // Upstream accounts kept their summaries at 1..3, below the services' fixed ids.
    private static final int LEGACY_SUMMARY_SLOTS = 3;
    private static final int SUMMARY_ID_BASE = 1000;

    private AccountSlots() {}

    /**
     * Id of the per-account summary notification. Upstream uses account + 1, which from the fourth
     * account on replaces the video encoding (4), music and import (5) and live location (6)
     * notifications. The first three accounts keep their ids, so summaries already shown still update.
     */
    public static int summaryNotificationId(int account) {
        return account < LEGACY_SUMMARY_SLOTS ? account + 1 : SUMMARY_ID_BASE + account;
    }
}
