package org.telegram.messenger.banana;

import org.junit.Test;
import static org.junit.Assert.*;

public class BananaThemePolicyTest {
    @Test public void permitsOnlyTheFourIosPalettes() {
        for (String name : new String[]{"Classic White", "Classic Black", "Banana Cream", "Banana Graphite"}) {
            assertTrue(name, BananaThemePolicy.isAllowed(name));
            assertEquals(name, BananaThemePolicy.migrate(name, false));
            assertEquals(name, BananaThemePolicy.migrate(name, true));
        }
        for (String name : new String[]{null, "", "Blue", "Night", "Day", "Dark Blue", "Banana Glass", "Banana Glass Night", "remote123", "Classic White.attheme", "classic white"}) {
            assertFalse(name, BananaThemePolicy.isAllowed(name));
        }
    }

    @Test public void migratesRetiredGlassPalettesWithoutLosingDayOrNight() {
        assertEquals("Classic White", BananaThemePolicy.migrate("Banana Glass", false));
        assertEquals("Classic Black", BananaThemePolicy.migrate("Banana Glass Night", false));
        assertEquals(0, BananaThemePolicy.paletteOrdinal(4));
        assertEquals(3, BananaThemePolicy.paletteOrdinal(5));
        for (int n = 0; n < 4; n++) assertEquals(n, BananaThemePolicy.paletteOrdinal(n));
        assertEquals(0, BananaThemePolicy.paletteOrdinal(-1));
        assertEquals(0, BananaThemePolicy.paletteOrdinal(Integer.MAX_VALUE));
    }

    @Test public void freshAndUnsupportedThemesUseBrandedDefaults() {
        assertEquals("Banana Cream", BananaThemePolicy.migrate(null, false));
        assertEquals("Banana Graphite", BananaThemePolicy.migrate(null, true));
        assertEquals("Banana Cream", BananaThemePolicy.migrate("remote123", false));
        assertEquals("Banana Graphite", BananaThemePolicy.migrate("remote123", true));
    }
}
