package org.telegram.messenger.banana;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** The freeze watchdog's rolling-window counter. */
public class BananaPerfGuardTest {

    @Test
    public void burstCrossesTheLimitOnce() {
        BananaPerfGuard.RollingCounter counter = new BananaPerfGuard.RollingCounter(10_000, 8);
        for (int i = 0; i < 7; i++) assertFalse(counter.add(i * 1000, true));
        assertTrue(counter.add(7_000, true));
        // The burst is closed: the next heavy dispatch starts a fresh window.
        assertFalse(counter.add(7_500, true));
    }

    @Test
    public void spreadOutFreezesNeverTrigger() {
        BananaPerfGuard.RollingCounter counter = new BananaPerfGuard.RollingCounter(10_000, 8);
        for (int i = 0; i < 20; i++) assertFalse(counter.add(i * 5_000L, true));
    }

    @Test
    public void lightDispatchesDoNotCount() {
        BananaPerfGuard.RollingCounter counter = new BananaPerfGuard.RollingCounter(10_000, 8);
        for (int i = 0; i < 100; i++) assertFalse(counter.add(i * 100, false));
    }

    @Test
    public void aSecondBurstTriggersAgain() {
        BananaPerfGuard.RollingCounter counter = new BananaPerfGuard.RollingCounter(10_000, 8);
        for (int i = 0; i < 8; i++) counter.add(i * 1000, true);
        for (int i = 0; i < 7; i++) assertFalse(counter.add(20_000 + i * 1000, true));
        assertTrue(counter.add(27_000, true));
    }
}
