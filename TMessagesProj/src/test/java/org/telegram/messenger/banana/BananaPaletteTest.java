package org.telegram.messenger.banana;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Every text and control colour of both BananaGram themes stays readable (WCAG 2 AA). */
public class BananaPaletteTest {
    private static final double TEXT = 4.5;
    private static final double CONTROL = 3.0;

    private static void readable(String what, int foreground, int background, double minimum) {
        double ratio = BananaPalette.contrast(foreground, background);
        assertTrue(what + " contrast " + ratio + " is below " + minimum, ratio >= minimum);
    }

    private static void check(String name, BananaPalette p) {
        readable(name + " text on surface", p.foreground, p.surface, TEXT);
        readable(name + " text on background", p.foreground, p.background, TEXT);
        readable(name + " incoming text", p.foreground, p.raised, TEXT);
        readable(name + " outgoing text", p.outgoingText, p.outgoing, TEXT);
        readable(name + " secondary text", p.secondary, p.surface, TEXT);
        readable(name + " section footer", p.secondary, p.background, TEXT);
        readable(name + " incoming time", p.secondary, p.raised, TEXT);
        readable(name + " outgoing time", p.outgoingSecondary, p.outgoing, TEXT);
        readable(name + " accent text", p.accent, p.surface, TEXT);
        readable(name + " incoming link", p.accent, p.raised, TEXT);
        readable(name + " outgoing link", p.outgoingAccent, p.outgoing, TEXT);
        readable(name + " button label", p.onAccent, p.accent, TEXT);
        readable(name + " text on a pressed row", p.foreground, p.highlighted, TEXT);
        readable(name + " text in search fields", p.foreground, p.search, TEXT);
        readable(name + " switch off on surface", p.disabled, p.surface, CONTROL);
        readable(name + " switch off on background", p.disabled, p.background, CONTROL);
        readable(name + " accent control on surface", p.accent, p.surface, CONTROL);
    }

    @Test
    public void creamIsReadable() {
        check("Cream", BananaPalette.CREAM);
    }

    @Test
    public void graphiteIsReadable() {
        check("Graphite", BananaPalette.GRAPHITE);
    }

    @Test
    public void lightAndDarkPalettesAreMarkedCorrectly() {
        assertTrue(!BananaPalette.CREAM.dark && BananaPalette.GRAPHITE.dark);
        assertTrue("Cream must be a light surface", BananaPalette.contrast(BananaPalette.CREAM.surface, 0xFF000000) > 10);
        assertTrue("Graphite must be a dark surface", BananaPalette.contrast(BananaPalette.GRAPHITE.surface, 0xFFFFFFFF) > 10);
    }
}
