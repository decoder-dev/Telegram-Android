package org.telegram.messenger.banana;

import org.junit.Test;
import static org.junit.Assert.*;

public class RegexProtocolTest {
    @Test public void binderPayloadHasCombinedNotJustPerItemLimit() {
        String half = new String(new char[RegexProtocol.MAX_INPUT / 2]);
        assertTrue(RegexProtocol.valid("x", new String[]{half, half}));
        assertFalse(RegexProtocol.valid("x", new String[]{half, half, "x"}));
        assertFalse(RegexProtocol.valid("x", new String[RegexProtocol.MAX_BATCH + 1]));
    }
    @Test public void nullAndOversizedRulesAreRejectedBeforeBinding() {
        assertFalse(RegexProtocol.valid(null, new String[]{"a"}));
        assertFalse(RegexProtocol.valid("x", null));
        assertFalse(RegexProtocol.valid("x", new String[]{null}));
        assertFalse(RegexProtocol.valid(new String(new char[RegexProtocol.MAX_PATTERN + 1]), new String[]{"a"}));
        assertTrue(RegexProtocol.valid("", new String[]{""}));
    }
}
