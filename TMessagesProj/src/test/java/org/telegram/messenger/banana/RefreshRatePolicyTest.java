package org.telegram.messenger.banana;

import org.junit.Test;
import static org.junit.Assert.*;

public class RefreshRatePolicyTest {
    @Test public void preservesSystemAndRequestsActualMaximum() {
        assertEquals(0, RefreshRatePolicy.resolve(0, 120), 0);
        assertEquals(120, RefreshRatePolicy.resolve(-1, 120), 0);
        assertEquals(60, RefreshRatePolicy.resolve(60, 120), 0);
    }
    @Test public void handlesMovingToSlowerOrUnavailableDisplay() {
        assertEquals(60, RefreshRatePolicy.resolve(120, 60), 0);
        assertEquals(0, RefreshRatePolicy.resolve(120, 0), 0);
        assertEquals(0, RefreshRatePolicy.resolve(Float.NaN, 120), 0);
        assertEquals(0, RefreshRatePolicy.resolve(-1, Float.NaN), 0);
    }
}
