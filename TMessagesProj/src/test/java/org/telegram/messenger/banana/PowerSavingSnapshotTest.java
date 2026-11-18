package org.telegram.messenger.banana;

import org.junit.Test;
import org.telegram.messenger.LiteMode;

import static org.junit.Assert.*;

public class PowerSavingSnapshotTest {
    @Test public void roundTripRestoresTheMaskedFlags() {
        int before = LiteMode.FLAG_AUTOPLAY_VIDEOS | LiteMode.FLAG_CHAT_BLUR | 1 << 20;
        int saving = PowerSavingSnapshot.liteWhenEnabled(before);
        assertEquals(0, saving & PowerSavingSnapshot.MASK);
        assertEquals(1 << 20, saving & (1 << 20));
        assertEquals(before, PowerSavingSnapshot.liteWhenDisabled(saving, before));
    }

    @Test public void changesOutsideTheMaskSurviveAndNoSnapshotChangesNothing() {
        int before = LiteMode.FLAG_AUTOPLAY_GIFS;
        int saving = PowerSavingSnapshot.liteWhenEnabled(before) | 1 << 21;
        int restored = PowerSavingSnapshot.liteWhenDisabled(saving, before);
        assertEquals(LiteMode.FLAG_AUTOPLAY_GIFS | 1 << 21, restored);
        assertEquals(12345, PowerSavingSnapshot.liteWhenDisabled(12345, PowerSavingSnapshot.NONE));
    }

    @Test public void effectsPackIntoBits() {
        int packed = PowerSavingSnapshot.packEffects(true, false, true);
        assertTrue((packed & PowerSavingSnapshot.SNOW) != 0);
        assertEquals(0, packed & PowerSavingSnapshot.AVATAR_GLOW);
        assertTrue((packed & PowerSavingSnapshot.REACTION_GLOW) != 0);
        assertEquals(0, PowerSavingSnapshot.packEffects(false, false, false));
    }
}
