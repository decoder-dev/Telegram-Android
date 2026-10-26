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

    /** Warm cream day palette (CSS-like tokens). */
    public static final class Cream {
        public static final int accent = 0xFFE0A800;
        public static final int accentDark = 0xFFC48E00;
        public static final int background = 0xFFF7F1E3;
        public static final int surface = 0xFFFFFBF3;
        public static final int bubbleOut = 0xFFF3D56B;
        public static final int bubbleIn = 0xFFFFFFFF;
        public static final int text = 0xFF2A2418;
        public static final int textSecondary = 0xFF7A6F5C;
        public static final int previewBg = 0xFFEFE6D5;
    }

    /** Charcoal night palette with banana gold accent. */
    public static final class Graphite {
        public static final int accent = 0xFFE0A800;
        public static final int accentDark = 0xFFB88900;
        public static final int background = 0xFF1C1E22;
        public static final int surface = 0xFF2A2D33;
        public static final int bubbleOut = 0xFF4A3F1F;
        public static final int bubbleIn = 0xFF32363D;
        public static final int text = 0xFFF2EFE6;
        public static final int textSecondary = 0xFFA39B8C;
        public static final int previewBg = 0xFF2F333A;
    }

    public enum Palette {
        SYSTEM,
        CREAM,
        GRAPHITE
    }

    /** Keep native theme colors, adjusting secondary text only when its contrast is too low. */
    public static final Theme.ResourcesProvider COLORS = key -> contrastFix(Theme.getColor(key), key);

    public static final Theme.ResourcesProvider CREAM = key -> overlay(key, Palette.CREAM);
    public static final Theme.ResourcesProvider GRAPHITE = key -> overlay(key, Palette.GRAPHITE);

    public static Theme.ResourcesProvider forExtras() {
        Palette palette = selectedPalette();
        if (palette == Palette.CREAM) {
            return CREAM;
        }
        if (palette == Palette.GRAPHITE) {
            return GRAPHITE;
        }
        return COLORS;
    }

    public static Palette selectedPalette() {
        int value = BananaConfig.getThemePalette(UserConfig.selectedAccount);
        if (value == Palette.CREAM.ordinal()) {
            return Palette.CREAM;
        }
        if (value == Palette.GRAPHITE.ordinal()) {
            return Palette.GRAPHITE;
        }
        return Palette.SYSTEM;
    }

    public static void setPalette(int account, Palette palette) {
        BananaConfig.setThemePalette(account, palette.ordinal());
        applyThemeInfo(palette);
    }

    public static void applyThemeInfo(Palette palette) {
        Theme.ThemeInfo info;
        boolean night;
        if (palette == Palette.CREAM) {
            info = Theme.getTheme(THEME_CREAM);
            night = false;
        } else if (palette == Palette.GRAPHITE) {
            info = Theme.getTheme(THEME_GRAPHITE);
            night = true;
        } else {
            return;
        }
        if (info == null) {
            return;
        }
        if (night) {
            Theme.setCurrentNightTheme(info);
        }
        NotificationCenter.getGlobalInstance().postNotificationName(
                NotificationCenter.needSetDayNightTheme, info, night, null, -1);
    }

    /** Sparse overrides used when a Banana palette is active for chat accents. */
    public static void putBubbleOverrides(SparseIntArray into, Palette palette) {
        if (into == null || palette == Palette.SYSTEM) {
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
