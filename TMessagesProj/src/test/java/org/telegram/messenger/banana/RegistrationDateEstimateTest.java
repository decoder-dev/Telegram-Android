package org.telegram.messenger.banana;

import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.*;

public class RegistrationDateEstimateTest {
    private static final long NOW = RegistrationDateAnchors.DATA_END + 86400L * 30;

    @Test public void tableIsConsistent() {
        long[] ids = RegistrationDateAnchors.IDS;
        assertEquals(0L, ids[0]);
        for (int i = 1; i < ids.length; i++) assertTrue("ids sorted at " + i, ids[i] >= ids[i - 1]);
        assertEquals(ids.length, RegistrationDateAnchors.POINTS.length);
        assertEquals(ids.length, RegistrationDateAnchors.LOWS.length);
        assertEquals(ids.length, RegistrationDateAnchors.HIGHS.length);
        for (int i = 0; i < ids.length; i++) {
            assertTrue(RegistrationDateAnchors.LOWS[i] <= RegistrationDateAnchors.POINTS[i]);
            assertTrue(RegistrationDateAnchors.POINTS[i] <= RegistrationDateAnchors.HIGHS[i]);
        }
    }

    @Test public void serviceAccountsAndInvalidIdsHaveNoEstimate() {
        for (long id : new long[]{0, -5, 777000, 333000, 489000, 4244000}) {
            assertNull(Long.toString(id), RegistrationDateEstimate.estimate(id, NOW));
        }
    }

    @Test public void everyEstimateIsAnOrderedRangeInsideTelegramsLifetime() {
        long[] samples = {1, 25, 1_000_000, 100_000_000, 777_000_001L, 1_500_000_000L, 5_000_000_000L,
                RegistrationDateAnchors.IDS[RegistrationDateAnchors.IDS.length - 1] - 1,
                RegistrationDateAnchors.IDS[RegistrationDateAnchors.IDS.length - 1] + 50_000_000L};
        for (long id : samples) {
            RegistrationDateEstimate.Result result = RegistrationDateEstimate.estimate(id, NOW);
            assertNotNull(Long.toString(id), result);
            assertTrue(id + " from", result.from >= RegistrationDateAnchors.LAUNCH);
            assertTrue(id + " order", result.from <= result.point && result.point <= result.to);
            assertTrue(id + " future", result.to <= NOW);
        }
    }

    @Test public void largerIdsAreNeverOlder() {
        long previous = Long.MIN_VALUE;
        for (long id = 1; id < 9_500_000_000L; id += 37_000_000L) {
            RegistrationDateEstimate.Result result = RegistrationDateEstimate.estimate(id, NOW);
            if (result == null) continue;
            assertTrue("id " + id, result.point >= previous);
            previous = result.point;
        }
    }

    @Test public void anchorNodesGiveTheirOwnPoint() {
        int node = RegistrationDateAnchors.IDS.length / 2;
        long id = RegistrationDateAnchors.IDS[node];
        if (id > RegistrationDateAnchors.IDS[node - 1]) {
            RegistrationDateEstimate.Result result = RegistrationDateEstimate.rawEstimate(id, RegistrationDateAnchors.DATA_END);
            assertEquals(RegistrationDateAnchors.POINTS[node], result.point);
        }
    }

    @Test public void textShowsMonthOrYearInUtc() {
        RegistrationDateEstimate.Result narrow = new RegistrationDateEstimate.Result(1458000000L, 1457000000L, 1459000000L);
        assertTrue(narrow.isMonthPrecise());
        assertTrue(RegistrationDateEstimate.text(narrow, Locale.US).startsWith("≈ "));
        assertTrue(RegistrationDateEstimate.text(narrow, Locale.US).endsWith("2016"));
        RegistrationDateEstimate.Result wide = new RegistrationDateEstimate.Result(1458000000L, 1400000000L, 1500000000L);
        assertFalse(wide.isMonthPrecise());
        assertEquals("≈ 2016", RegistrationDateEstimate.text(wide, Locale.US));
    }
}
