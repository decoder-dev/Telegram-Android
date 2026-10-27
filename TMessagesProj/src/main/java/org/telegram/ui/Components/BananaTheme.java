package org.telegram.ui.Components;

import android.graphics.Color;
import android.util.SparseIntArray;

import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.banana.BananaConfig;
import org.telegram.ui.ActionBar.Theme;

/**
 * BananaGram appearance helpers: Cream / Graphite palettes for Extras UI and
 * selectable ThemeInfo accents that tint chat bubbles via the normal Theme path.
 */
public final class BananaTheme {
    private BananaTheme() {}

    public static final String THEME_CREAM = "Banana Cream";
    public static final String THEME_GRAPHITE = "Banana Graphite";
    public static final String THEME_GLASS = "Banana Glass";
    public static final String THEME_GLASS_NIGHT = "Banana Glass Night";

    /** Warm cream day palette. Hex values match iOS makeBananaGramTheme. */
    public static final class Cream {
        public static final int accent = 0xFF8A5F1E;
        public static final int accentDark = 0xFF85591A;
        public static final int background = 0xFFF6F0E3;
        public static final int surface = 0xFFFFFAF2;
        public static final int bubbleOut = 0xFFFCF2DC;
        public static final int bubbleIn = 0xFFFFFDF8;
        public static final int text = 0xFF3A3430;
        public static final int textSecondary = 0xFF6A655D;
        public static final int previewBg = 0xFFF1E8D6;
    }

    /** Warm graphite night palette. Hex values match iOS makeBananaGramTheme. */
    public static final class Graphite {
        public static final int accent = 0xFFEBD58F;
        public static final int accentDark = 0xFFEBD58F;
        public static final int background = 0xFF1D1C1A;
        public static final int surface = 0xFF272522;
        public static final int bubbleOut = 0xFF5E5236;
        public static final int bubbleIn = 0xFF312F2B;
        public static final int text = 0xFFEFEBE0;
        public static final int textSecondary = 0xFFB5B0A4;
        public static final int previewBg = 0xFF36332E;
    }

    /**
     * Stored ordinals stay compatible: 0 was System and now selects Classic White,
     * 1 Cream, 2 Graphite, 3 Classic Black. Glass and Glass Night are appended.
     */
    public enum Palette {
        CLASSIC_WHITE,
        CREAM,
        GRAPHITE,
        CLASSIC_BLACK,
        GLASS,
        GLASS_NIGHT
    }

    /** Keep native theme colors, adjusting secondary text only when its contrast is too low. */
    public static final Theme.ResourcesProvider COLORS = key -> contrastFix(Theme.getColor(key), key);

    public static final Theme.ResourcesProvider CREAM = key -> overlay(key, Palette.CREAM);
    public static final Theme.ResourcesProvider GRAPHITE = key -> overlay(key, Palette.GRAPHITE);
    public static final Theme.ResourcesProvider GLASS = key -> org.telegram.messenger.banana.BananaThemes.extrasColor(key, false);
    public static final Theme.ResourcesProvider GLASS_NIGHT = key -> org.telegram.messenger.banana.BananaThemes.extrasColor(key, true);

    public static Theme.ResourcesProvider forExtras() {
        Palette palette = selectedPalette();
        if (palette == Palette.CREAM) {
            return CREAM;
        }
        if (palette == Palette.GRAPHITE) {
            return GRAPHITE;
        }
        if (palette == Palette.GLASS) {
            return GLASS;
        }
        if (palette == Palette.GLASS_NIGHT) {
            return GLASS_NIGHT;
        }
        return COLORS;
    }

    public static Palette selectedPalette() {
        int value = BananaConfig.getThemePalette(UserConfig.selectedAccount);
        Palette[] values = Palette.values();
        if (value < 0 || value >= values.length) {
            return Palette.CLASSIC_WHITE;
        }
        return values[value];
    }

    public static void setPalette(int account, Palette palette) {
        BananaConfig.setThemePalette(account, palette.ordinal());
        applyThemeInfo(palette);
    }

    public static void applyThemeInfo(Palette palette) {
        if (Theme.selectedAutoNightType == Theme.AUTO_NIGHT_TYPE_SCHEDULED) {
            Theme.selectedAutoNightType = Theme.AUTO_NIGHT_TYPE_NONE;
            Theme.saveAutoNightThemeConfig();
        }
        Theme.ThemeInfo info;
        boolean dark;
        if (palette == Palette.CREAM) {
            info = Theme.getTheme(THEME_CREAM);
            dark = false;
        } else if (palette == Palette.GRAPHITE) {
            info = Theme.getTheme(THEME_GRAPHITE);
            dark = true;
        } else if (palette == Palette.CLASSIC_BLACK) {
            info = Theme.getTheme("Classic Black");
            dark = true;
        } else if (palette == Palette.GLASS) {
            info = Theme.getTheme(THEME_GLASS);
            dark = false;
        } else if (palette == Palette.GLASS_NIGHT) {
            info = Theme.getTheme(THEME_GLASS_NIGHT);
            dark = true;
        } else {
            info = Theme.getTheme("Classic White");
            dark = false;
        }
        if (info == null) {
            return;
        }
        if (dark) {
            Theme.setCurrentNightTheme(info);
        }
        NotificationCenter.getGlobalInstance().postNotificationName(
                NotificationCenter.needSetDayNightTheme, info, false, null, -1);
    }

    /** Sparse overrides used when a Banana palette is active for chat accents. */
    public static void putBubbleOverrides(SparseIntArray into, Palette palette) {
        if (into == null || palette == Palette.CLASSIC_WHITE || palette == Palette.CLASSIC_BLACK) {
            return;
        }
        if (palette == Palette.GLASS || palette == Palette.GLASS_NIGHT) {
            boolean night = palette == Palette.GLASS_NIGHT;
            into.put(Theme.key_chat_outBubble, night ? 0xFF1C3D66 : 0xFFE4F0FF);
            into.put(Theme.key_chat_inBubble, night ? 0xFF243140 : 0xFFFCFDFE);
            into.put(Theme.key_chat_messagePanelSend, night ? 0xFF8EBBFF : 0xFF3D8BFF);
            into.put(Theme.key_actionBarDefault, night ? 0xFF171E28 : 0xFFE7EEF5);
            into.put(Theme.key_windowBackgroundWhite, night ? 0xFF171E28 : 0xFFE7EEF5);
            into.put(Theme.key_windowBackgroundGray, night ? 0xFF121820 : 0xFFDCE6F0);
            into.put(Theme.key_checkbox, night ? 0xFF8EBBFF : 0xFF3D8BFF);
            into.put(Theme.key_switchTrackChecked, night ? 0xFF8EBBFF : 0xFF3D8BFF);
            return;
        }
        if (palette == Palette.CREAM) {
            into.put(Theme.key_chat_outBubble, Cream.bubbleOut);
            into.put(Theme.key_chat_inBubble, Cream.bubbleIn);
            into.put(Theme.key_chat_messagePanelSend, Cream.accent);
            into.put(Theme.key_actionBarDefault, Cream.accent);
            into.put(Theme.key_windowBackgroundWhite, Cream.surface);
            into.put(Theme.key_windowBackgroundGray, Cream.background);
            into.put(Theme.key_checkbox, Cream.accent);
            into.put(Theme.key_switchTrackChecked, Cream.accent);
        } else {
            into.put(Theme.key_chat_outBubble, Graphite.bubbleOut);
            into.put(Theme.key_chat_inBubble, Graphite.bubbleIn);
            into.put(Theme.key_chat_messagePanelSend, Graphite.accent);
            into.put(Theme.key_actionBarDefault, Graphite.surface);
            into.put(Theme.key_windowBackgroundWhite, Graphite.surface);
            into.put(Theme.key_windowBackgroundGray, Graphite.background);
            into.put(Theme.key_checkbox, Graphite.accent);
            into.put(Theme.key_switchTrackChecked, Graphite.accent);
        }
    }

    private static int overlay(int key, Palette palette) {
        int color = Theme.getColor(key);
        if (palette == Palette.CREAM) {
            if (key == Theme.key_windowBackgroundWhite || key == Theme.key_dialogBackground) {
                return Cream.surface;
            }
            if (key == Theme.key_windowBackgroundGray) {
                return Cream.background;
            }
            if (key == Theme.key_actionBarDefault || key == Theme.key_checkboxSquareBackground ||
                    key == Theme.key_switchTrackChecked || key == Theme.key_radioBackgroundChecked ||
                    key == Theme.key_chat_messagePanelSend || key == Theme.key_featuredStickers_addButton) {
                return Cream.accent;
            }
            if (key == Theme.key_chat_outBubble) {
                return Cream.bubbleOut;
            }
            if (key == Theme.key_chat_inBubble) {
                return Cream.bubbleIn;
            }
            if (key == Theme.key_windowBackgroundWhiteBlackText) {
                return Cream.text;
            }
            if (key == Theme.key_windowBackgroundWhiteGrayText2 || key == Theme.key_windowBackgroundWhiteGrayText4 ||
                    key == Theme.key_windowBackgroundWhiteHintText) {
                return Cream.textSecondary;
            }
            if (key == Theme.key_windowBackgroundWhiteBlueHeader) {
                return Cream.accentDark;
            }
        } else if (palette == Palette.GRAPHITE) {
            if (key == Theme.key_windowBackgroundWhite || key == Theme.key_dialogBackground) {
                return Graphite.surface;
            }
            if (key == Theme.key_windowBackgroundGray) {
                return Graphite.background;
            }
            if (key == Theme.key_actionBarDefault || key == Theme.key_checkboxSquareBackground ||
                    key == Theme.key_switchTrackChecked || key == Theme.key_radioBackgroundChecked ||
                    key == Theme.key_chat_messagePanelSend || key == Theme.key_featuredStickers_addButton) {
                return Graphite.accent;
            }
            if (key == Theme.key_chat_outBubble) {
                return Graphite.bubbleOut;
            }
            if (key == Theme.key_chat_inBubble) {
                return Graphite.bubbleIn;
            }
            if (key == Theme.key_windowBackgroundWhiteBlackText) {
                return Graphite.text;
            }
            if (key == Theme.key_windowBackgroundWhiteGrayText2 || key == Theme.key_windowBackgroundWhiteGrayText4 ||
                    key == Theme.key_windowBackgroundWhiteHintText) {
                return Graphite.textSecondary;
            }
            if (key == Theme.key_windowBackgroundWhiteBlueHeader) {
                return Graphite.accent;
            }
        }
        return contrastFix(color, key);
    }

    private static int contrastFix(int color, int key) {
        if (key != Theme.key_windowBackgroundWhiteGrayText4 && key != Theme.key_windowBackgroundWhiteGrayText2 &&
                key != Theme.key_windowBackgroundWhiteHintText && key != Theme.key_windowBackgroundWhiteBlueHeader) {
            return color;
        }
        int background = ColorUtils.setAlphaComponent(Theme.getColor(key == Theme.key_windowBackgroundWhiteGrayText4 ?
                Theme.key_windowBackgroundGray : Theme.key_windowBackgroundWhite), 255);
        int foreground = ColorUtils.setAlphaComponent(color, 255);
        int text = ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), 255);
        for (int step = 0; step <= 20; step++) {
            int readable = ColorUtils.blendARGB(foreground, text, step / 20f);
            if (ColorUtils.calculateContrast(readable, background) >= 4.5) {
                return readable;
            }
        }
        return ColorUtils.calculateContrast(Color.BLACK, background) > ColorUtils.calculateContrast(Color.WHITE, background) ?
                Color.BLACK : Color.WHITE;
    }
}
