package org.telegram.messenger.banana;

/**
 * Colour roles of the two BananaGram themes, free of Android types so JVM tests can check their
 * contrast. BananaThemes maps each role onto the Telegram theme keys that use it.
 */
public final class BananaPalette {
    public final boolean dark;
    public final int background, surface, raised, foreground, secondary, accent, onAccent, separator, pinned;
    public final int outgoing, outgoingGradient, outgoingText, outgoingSecondary, outgoingAccent;
    public final int highlighted, search, disabled;
    public final int wallpaperTo1, wallpaperTo2, wallpaperTo3;

    private BananaPalette(boolean dark, int background, int surface, int raised, int foreground, int secondary,
                          int accent, int onAccent, int separator, int pinned, int outgoing, int outgoingGradient,
                          int outgoingText, int outgoingSecondary, int outgoingAccent, int highlighted, int search,
                          int disabled, int wallpaperTo1, int wallpaperTo2, int wallpaperTo3) {
        this.dark = dark;
        this.background = background;
        this.surface = surface;
        this.raised = raised;
        this.foreground = foreground;
        this.secondary = secondary;
        this.accent = accent;
        this.onAccent = onAccent;
        this.separator = separator;
        this.pinned = pinned;
        this.outgoing = outgoing;
        this.outgoingGradient = outgoingGradient;
        this.outgoingText = outgoingText;
        this.outgoingSecondary = outgoingSecondary;
        this.outgoingAccent = outgoingAccent;
        this.highlighted = highlighted;
        this.search = search;
        this.disabled = disabled;
        this.wallpaperTo1 = wallpaperTo1;
        this.wallpaperTo2 = wallpaperTo2;
        this.wallpaperTo3 = wallpaperTo3;
    }

    public static final BananaPalette CREAM = new BananaPalette(false,
            0xFFF6F0E3, 0xFFFFFAF2, 0xFFFFFDF8, 0xFF3A3430, 0xFF6A655D,
            0xFF8A5F1E, 0xFFFFFFFF, 0xFFE6DDCC, 0xFFFBF6EA,
            0xFFFCF2DC, 0xFFF8EBCB, 0xFF3A3430, 0xFF85591A, 0xFF85591A,
            0xFFF1E8D6, 0xFFF0E9DB,
            // 3:1 against the surface and the background, the WCAG minimum for controls.
            0xFF918A7E,
            0xFFF4E7C9, 0xFFFCF6E8, 0xFFF0E2C2);

    public static final BananaPalette GRAPHITE = new BananaPalette(true,
            0xFF1D1C1A, 0xFF272522, 0xFF312F2B, 0xFFEFEBE0, 0xFFB5B0A4,
            0xFFEBD58F, 0xFF1B1A16, 0xFF3A3733, 0xFF2B2926,
            0xFF5E5236, 0xFF4A4029, 0xFFEFEBE0, 0xFFD5CBBA, 0xFFEBD58F,
            0xFF36332E, 0xFF302E2A, 0xFF77726A,
            0xFF34312B, 0xFF2A2824, 0xFF1D1C1A);

    /** WCAG 2 contrast ratio of two opaque colours. */
    public static double contrast(int a, int b) {
        double la = luminance(a), lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminance(int color) {
        return 0.2126 * channel((color >> 16) & 0xFF) + 0.7152 * channel((color >> 8) & 0xFF) + 0.0722 * channel(color & 0xFF);
    }

    private static double channel(int value) {
        double c = value / 255d;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }
}
