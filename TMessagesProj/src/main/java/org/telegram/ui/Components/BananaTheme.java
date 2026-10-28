package org.telegram.ui.Components;

import android.graphics.Color;

import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.banana.BananaConfig;
import org.telegram.ui.ActionBar.Theme;

/** BananaGram settings share the active application's palette, including live theme changes. */
public final class BananaTheme {
    private BananaTheme() {}

    public static final String THEME_CREAM = "Banana Cream";
    public static final String THEME_GRAPHITE = "Banana Graphite";

    /**
     * Stored ordinals stay compatible: 0 was System and now selects Classic White,
     * 1 Cream, 2 Graphite, 3 Classic Black. Retired glass ordinals migrate on read.
     */
    public enum Palette {
        CLASSIC_WHITE,
        CREAM,
        GRAPHITE,
        CLASSIC_BLACK
    }

    /** Keep native theme colors, adjusting secondary text only when its contrast is too low. */
    public static final Theme.ResourcesProvider COLORS = key -> contrastFix(Theme.getColor(key), key);

    public static Theme.ResourcesProvider forExtras() {
        // Views keep this provider for their lifetime. Resolve the active colors at lookup time
        // instead of retaining a Cream/Graphite overlay after the user changes the palette.
        return COLORS;
    }

    public static Palette selectedPalette() {
        int value = org.telegram.messenger.banana.BananaThemePolicy.paletteOrdinal(
                BananaConfig.getThemePalette(UserConfig.selectedAccount));
        Theme.ThemeInfo active = Theme.getActiveTheme();
        if (Theme.isBananaTheme(active)) {
            if (org.telegram.messenger.banana.BananaThemePolicy.CREAM.equals(active.name)) return Palette.CREAM;
            if (org.telegram.messenger.banana.BananaThemePolicy.GRAPHITE.equals(active.name)) return Palette.GRAPHITE;
            if (org.telegram.messenger.banana.BananaThemePolicy.BLACK.equals(active.name)) return Palette.CLASSIC_BLACK;
            return Palette.CLASSIC_WHITE;
        }
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
