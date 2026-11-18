package org.telegram.ui.Components.blur3.drawable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GlassRimTest {
    @Test
    public void highlightsOffDropsTheCrescentsButKeepsAnEvenOutline() {
        GlassRim rim = new GlassRim(30, 2, 0xffffffff, 1.5f, 0x80000000, 1f, 0x40ffffff, 1, 1, 1, 0);
        GlassRim plain = rim.withoutHighlights(0, 0);

        assertFalse(plain.hasTop());
        assertFalse(plain.hasBottom());
        assertFalse(plain.hasStroke());
        assertTrue(plain.hasRing());
        assertEquals(1f, plain.ringWidth, 0f);
        assertEquals(0x40ffffff, plain.ringColor);
        assertEquals(GlassRim.LIGHT_TOP, plain.lightAngle, 0f);
    }

    @Test
    public void aRingThatFollowsTheLightIsAHighlightToo() {
        GlassRim rim = new GlassRim(0, 0, 0, 0, 0, 2f, 0xffffffff, 2, 0.5f, 1, 2);
        assertFalse(rim.withoutHighlights(0, 0).hasRing());
    }

    @Test
    public void outlineModeAddsAnEvenStrokeInside() {
        GlassRim rim = new GlassRim(0, 2, 0xffffffff, 1, 0xff000000, 0, 0, 1, 1, 1, 0);
        GlassRim outline = rim.withoutHighlights(3f, 0xff112233);

        assertTrue(outline.hasStroke());
        assertEquals(3f, outline.strokeWidth, 0f);
        assertEquals(0xff112233, outline.strokeColor);
        assertFalse(outline.hasTop());
        assertFalse(outline.hasRing());
    }
}
