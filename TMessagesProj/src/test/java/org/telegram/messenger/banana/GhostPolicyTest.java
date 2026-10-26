package org.telegram.messenger.banana;

import org.junit.Test;

import static org.junit.Assert.*;

public class GhostPolicyTest {
    @Test
    public void silentSendingRespectsModeAndGhostState() {
        assertFalse(GhostPolicy.sendSilently(0, false));
        assertFalse(GhostPolicy.sendSilently(0, true));
        assertFalse(GhostPolicy.sendSilently(1, false));
        assertTrue(GhostPolicy.sendSilently(1, true));
        assertTrue(GhostPolicy.sendSilently(2, false));
        assertTrue(GhostPolicy.sendSilently(2, true));
        assertFalse(GhostPolicy.sendSilently(-1, true));
        assertFalse(GhostPolicy.sendSilently(Integer.MAX_VALUE, true));
    }

    @Test
    public void offlineResetRequiresAnOnlineUpdateAndBothOptIns() {
        for (int mask = 0; mask < 8; mask++) {
            assertEquals(mask == 7, GhostPolicy.shouldReturnOffline((mask & 1) != 0,
                    (mask & 2) != 0, (mask & 4) != 0));
        }
    }
    @Test
    public void defaultOptionsDoNotChangeTelegramBehavior() {
        for (int action = 0; action <= 11; action++) {
            assertFalse(GhostPolicy.suppressActivity(action, false, false, false, false));
        }
    }

    @Test
    public void cancellationAlwaysReachesServerForEveryPrivacyCombination() {
        for (int mask = 0; mask < 16; mask++) {
            assertFalse(GhostPolicy.suppressActivity(2, (mask & 1) != 0, (mask & 2) != 0,
                    (mask & 4) != 0, (mask & 8) != 0));
        }
    }

    @Test
    public void hidingTypingDoesNotCancelCallsOrBotControlMessages() {
        for (int action : new int[]{-1, 12, 13, Integer.MAX_VALUE}) {
            assertFalse(GhostPolicy.suppressActivity(action, true, true, true, true));
        }
    }

    @Test
    public void allVoiceAndVideoCaptureAndUploadKindsAreCovered() {
        for (int action : new int[]{1, 7}) {
            assertTrue(GhostPolicy.suppressActivity(action, false, true, false, false));
            assertFalse(GhostPolicy.suppressActivity(action, true, false, true, true));
        }
        for (int action : new int[]{3, 4, 5, 8, 9}) {
            assertTrue(GhostPolicy.suppressActivity(action, false, false, true, false));
            assertFalse(GhostPolicy.suppressActivity(action, true, true, false, true));
        }
    }

    @Test
    public void seenEmojiAnimationsAreReadReceiptsEvenWithoutHideTyping() {
        assertTrue(GhostPolicy.suppressActivity(11, false, false, false, true));
        assertTrue(GhostPolicy.suppressActivity(11, true, false, false, false));
        assertFalse(GhostPolicy.suppressActivity(0, false, false, false, true));
    }
}
