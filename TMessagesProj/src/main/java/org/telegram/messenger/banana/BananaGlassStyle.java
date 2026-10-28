package org.telegram.messenger.banana;

/** Shared bounded controls for the glass tint and independent edge highlights. */
public final class BananaGlassStyle {
    private BananaGlassStyle() {}

    private static int percent(int value) { return Math.max(0, Math.min(100, value)); }

    public static int fillColor(int color, int tint) {
        int alpha = (color >>> 24) * (160 + percent(tint) * 95 / 100) / 255;
        return (color & 0x00ffffff) | (alpha << 24);
    }

    public static int highlightColor(int strength) {
        return (percent(strength) * 180 / 100 << 24) | 0x00ffffff;
    }
}
