package org.telegram.messenger.banana;

import org.junit.Test;
import static org.junit.Assert.*;

public class BananaGlassStyleTest {
    @Test public void tintPreservesRgbAndOriginalTransparency() {
        assertEquals(0xff123456, BananaGlassStyle.fillColor(0xff123456, 100));
        assertEquals(0xa0123456, BananaGlassStyle.fillColor(0xff123456, 0));
        assertEquals(0x80123456, BananaGlassStyle.fillColor(0x80123456, 100));
        assertEquals(0x00123456, BananaGlassStyle.fillColor(0x00123456, 25));
    }

    @Test public void edgeStrengthIsIndependentFromTintAndOtherEdges() {
        assertEquals(0x00ffffff, BananaGlassStyle.highlightColor(0));
        assertEquals(0xb4ffffff, BananaGlassStyle.highlightColor(100));
        assertTrue((BananaGlassStyle.highlightColor(95) >>> 24) > (BananaGlassStyle.highlightColor(20) >>> 24));
        assertEquals(BananaGlassStyle.highlightColor(0), BananaGlassStyle.highlightColor(-100));
        assertEquals(BananaGlassStyle.highlightColor(100), BananaGlassStyle.highlightColor(200));
        assertEquals(BananaGlassStyle.fillColor(0xff123456, 0), BananaGlassStyle.fillColor(0xff123456, -1));
        assertEquals(BananaGlassStyle.fillColor(0xff123456, 100), BananaGlassStyle.fillColor(0xff123456, 101));
    }
}
