package org.telegram.messenger.banana;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class ProxyRoutePolicyTest {
    @Test public void windowWrapsAroundThePool() {
        assertArrayEquals(new int[] {0, 1, 2, 3}, ProxyRoutePolicy.window(40, 0, 4));
        assertArrayEquals(new int[] {38, 39, 0, 1}, ProxyRoutePolicy.window(40, 38, 4));
        assertArrayEquals(new int[] {2, 0, 1}, ProxyRoutePolicy.window(3, 2, 4));
        assertArrayEquals(new int[] {1, 2, 3, 4}, ProxyRoutePolicy.window(10, -9, 4));
        assertEquals(0, ProxyRoutePolicy.window(0, 5, 4).length);
        assertEquals(0, ProxyRoutePolicy.window(10, 0, 0).length);
    }

    @Test public void consecutiveWindowsReachEveryCandidate() {
        // Three per pass: the active route holds the fourth slot.
        final int size = 10, step = 3;
        Set<Integer> seen = new HashSet<>();
        int offset = 7, passes = 0;
        for (int dialed = 0; dialed < size; dialed += step) {
            for (int index : ProxyRoutePolicy.window(size, offset, step)) {
                seen.add(index);
            }
            offset = ProxyRoutePolicy.nextOffset(size, offset, step);
            passes++;
        }
        assertEquals(4, passes);
        assertEquals(size, seen.size());
    }

    @Test public void offsetWrapsAndSurvivesAnEmptyPool() {
        assertEquals(0, ProxyRoutePolicy.nextOffset(0, 3, 4));
        assertEquals(2, ProxyRoutePolicy.nextOffset(10, 8, 4));
        assertEquals(1, ProxyRoutePolicy.nextOffset(3, 7, 0));
    }

    @Test public void dropsCountOnlyInsideTheSpan() {
        ProxyRoutePolicy.Drops drops = new ProxyRoutePolicy.Drops(3, 120_000);
        assertFalse(drops.record(0));
        assertFalse(drops.record(50_000));
        assertTrue(drops.record(100_000));
        // The count starts over after it fired.
        assertFalse(drops.record(101_000));
        assertFalse(drops.record(102_000));
        assertTrue(drops.record(103_000));
    }

    @Test public void oldDropsSlideOut() {
        ProxyRoutePolicy.Drops drops = new ProxyRoutePolicy.Drops(3, 120_000);
        assertFalse(drops.record(0));
        assertFalse(drops.record(100_000));
        assertFalse(drops.record(200_000));
        assertTrue(drops.record(210_000));
    }

    @Test public void troubleIsDueAfterConnectingTooLong() {
        ProxyRoutePolicy.Trouble trouble = new ProxyRoutePolicy.Trouble(15_000, 3, 120_000);
        assertEquals(-1, trouble.dueIn(0));
        trouble.connecting(1_000);
        trouble.connecting(5_000);
        assertEquals(6_000, trouble.dueIn(10_000));
        assertEquals(0, trouble.dueIn(16_000));
        trouble.connected();
        assertEquals(-1, trouble.dueIn(20_000));
    }

    @Test public void troubleIsDueWhenTheConnectionKeepsFallingOff() {
        ProxyRoutePolicy.Trouble trouble = new ProxyRoutePolicy.Trouble(15_000, 3, 120_000);
        trouble.connecting(0);
        // The first connection is not a drop.
        trouble.connected();
        for (long at : new long[] {10_000, 40_000}) {
            trouble.connecting(at);
            trouble.connected();
            assertEquals(-1, trouble.dueIn(at + 1_000));
        }
        trouble.connecting(70_000);
        trouble.connected();
        assertEquals(0, trouble.dueIn(71_000));
    }

    @Test public void troubleForgetsEverythingWithoutNetwork() {
        ProxyRoutePolicy.Trouble trouble = new ProxyRoutePolicy.Trouble(15_000, 2, 120_000);
        trouble.connected();
        trouble.connecting(1_000);
        trouble.reset();
        trouble.connected();
        trouble.connecting(2_000);
        assertEquals(15_000, trouble.dueIn(2_000));
        trouble.reset();
        assertEquals(-1, trouble.dueIn(2_000));
    }

    @Test public void resetForgetsDrops() {
        ProxyRoutePolicy.Drops drops = new ProxyRoutePolicy.Drops(2, 60_000);
        assertFalse(drops.record(1_000));
        drops.reset();
        assertFalse(drops.record(2_000));
        assertTrue(drops.record(3_000));
    }
}
