package org.telegram.messenger.banana;

import org.junit.Test;
import org.telegram.messenger.UserConfig;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class AccountSlotsTest {
    // Fixed ids of video encoding, music/import, live location, story upload, calls, migration and the keep-alive service.
    private static final List<Integer> SERVICE_IDS = Arrays.asList(4, 5, 6, 33, 201, 202, 301, 9999);

    @Test public void tenSlotsForEveryone() {
        assertEquals(10, UserConfig.MAX_ACCOUNT_COUNT);
        assertEquals(UserConfig.MAX_ACCOUNT_COUNT, UserConfig.MAX_ACCOUNT_DEFAULT_COUNT);
    }

    @Test public void firstThreeAccountsKeepTheirSummaryIds() {
        assertEquals(1, AccountSlots.summaryNotificationId(0));
        assertEquals(2, AccountSlots.summaryNotificationId(1));
        assertEquals(3, AccountSlots.summaryNotificationId(2));
    }

    @Test public void summaryIdsAreUniqueAndSkipServiceNotifications() {
        Set<Integer> ids = new HashSet<>();
        for (int account = 0; account < UserConfig.MAX_ACCOUNT_COUNT; account++) {
            int id = AccountSlots.summaryNotificationId(account);
            assertTrue("Duplicate summary id " + id, ids.add(id));
            assertFalse("Summary id " + id + " replaces a service notification", SERVICE_IDS.contains(id));
        }
    }
}
