package org.telegram.messenger.banana;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.regex.Pattern;
import org.junit.Test;

public class BoundedMatchTest {
    private static String repeat(char c, int count) {
        StringBuilder text = new StringBuilder(count);
        for (int i = 0; i < count; i++) text.append(c);
        return text.toString();
    }

    @Test
    public void ordinaryExpressionsStillMatch() {
        assertEquals(Boolean.TRUE, BoundedMatch.find(Pattern.compile("spam"), "buy spam now", 20_000_000L));
        assertEquals(Boolean.FALSE, BoundedMatch.find(Pattern.compile("spam"), "hello", 20_000_000L));
    }

    @Test
    public void catastrophicBacktrackingStopsWithinBudget() {
        Pattern runaway = Pattern.compile(".*a.*a.*a.*a.*a.*a.*a.*a.*a.*a.*a.*a.*b");
        long started = System.nanoTime();
        assertNull(BoundedMatch.find(runaway, repeat('a', 1500), 20_000_000L));
        assertTrue("took " + (System.nanoTime() - started) / 1_000_000 + " ms", System.nanoTime() - started < 2_000_000_000L);
    }

    @Test
    public void stackOverflowIsReportedAsRunaway() {
        Boolean result = BoundedMatch.find(Pattern.compile("^(a|aa)+$"), repeat('a', 200_000) + "b", 2_000_000_000L);
        assertNull(result);
    }
}
