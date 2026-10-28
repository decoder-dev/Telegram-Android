package org.telegram.messenger.banana;

/** The four palettes shared with BananaGram iOS. Glass is an effect, not a palette. */
public final class BananaThemePolicy {
    public static final String WHITE = "Classic White";
    public static final String BLACK = "Classic Black";
    public static final String CREAM = "Banana Cream";
    public static final String GRAPHITE = "Banana Graphite";

    private BananaThemePolicy() {}

    public static boolean isAllowed(String name) {
        return WHITE.equals(name) || BLACK.equals(name) || CREAM.equals(name) || GRAPHITE.equals(name);
    }

    public static String migrate(String name, boolean night) {
        if (isAllowed(name)) return name;
        if ("Banana Glass Night".equals(name)) return BLACK;
        if ("Banana Glass".equals(name)) return WHITE;
        return night ? GRAPHITE : CREAM;
    }

    /** Retains the four published ordinals and migrates the retired glass palettes. */
    public static int paletteOrdinal(int value) {
        if (value == 5) return 3;
        return value >= 0 && value < 4 ? value : 0;
    }
}
