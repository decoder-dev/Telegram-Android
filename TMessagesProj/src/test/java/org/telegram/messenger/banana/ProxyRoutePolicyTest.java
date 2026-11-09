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

    @Test public void resetForgetsDrops() {
        ProxyRoutePolicy.Drops drops = new ProxyRoutePolicy.Drops(2, 60_000);
        assertFalse(drops.record(1_000));
        drops.reset();
        assertFalse(drops.record(2_000));
        assertTrue(drops.record(3_000));
    }
}
