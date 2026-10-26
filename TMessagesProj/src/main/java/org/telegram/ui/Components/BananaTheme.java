package org.telegram.ui.Components;

import android.graphics.Color;
import androidx.core.graphics.ColorUtils;
import org.telegram.ui.ActionBar.Theme;

/** Keep native theme colors, adjusting secondary text only when its contrast is too low. */
public final class BananaTheme {
    private BananaTheme() {}

    public static final Theme.ResourcesProvider COLORS = key -> {
        int color = Theme.getColor(key);
        if (key != Theme.key_windowBackgroundWhiteGrayText4 && key != Theme.key_windowBackgroundWhiteGrayText2 &&
                key != Theme.key_windowBackgroundWhiteHintText && key != Theme.key_windowBackgroundWhiteBlueHeader) return color;
        int background = ColorUtils.setAlphaComponent(Theme.getColor(key == Theme.key_windowBackgroundWhiteGrayText4 ?
                Theme.key_windowBackgroundGray : Theme.key_windowBackgroundWhite), 255);
        int foreground = ColorUtils.setAlphaComponent(color, 255);
        int text = ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), 255);
        for (int step = 0; step <= 20; step++) {
            int readable = ColorUtils.blendARGB(foreground, text, step / 20f);
            if (ColorUtils.calculateContrast(readable, background) >= 4.5) return readable;
        }
        return ColorUtils.calculateContrast(Color.BLACK, background) > ColorUtils.calculateContrast(Color.WHITE, background) ?
                Color.BLACK : Color.WHITE;
    };
}
