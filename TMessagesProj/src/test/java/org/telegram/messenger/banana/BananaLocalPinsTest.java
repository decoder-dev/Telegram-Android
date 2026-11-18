package org.telegram.messenger.banana;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class BananaLocalPinsTest {
    @Test public void idsRoundTripWithoutDuplicatesOrJunk() {
        assertEquals(Arrays.asList(1L, -100L, 5L), BananaLocalPins.parse("1,-100,x,,5,1"));
        assertEquals("1,-100,5", BananaLocalPins.format(Arrays.asList(1L, -100L, 5L)));
        assertTrue(BananaLocalPins.parse(null).isEmpty());
    }

    @Test public void onlyPinsOverTheLimitStayLocal() {
        assertFalse(BananaLocalPins.shouldPinLocally(4, 5));
        assertTrue(BananaLocalPins.shouldPinLocally(5, 5));
        assertTrue(BananaLocalPins.shouldPinLocally(9, 5));
        assertFalse(BananaLocalPins.shouldPinLocally(9, 0));
    }
}
