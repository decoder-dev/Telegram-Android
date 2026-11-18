package org.telegram.messenger.banana;

import android.content.SharedPreferences;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LiteMode;
import org.telegram.messenger.MessagesController;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AnimatedEmojiDrawable;

/**
 * Enhanced Power Saving: LiteMode is app-wide, so its snapshot is kept in the global settings. Turning
 * the switch off restores exactly what was on before, including snow and glow on that account.
 */
public final class BananaPowerSaving {
    private static final String LITE = "bananaEnhancedPowerSavingLite";
    private static final String EFFECTS = "bananaEnhancedPowerSavingEffects";

    private BananaPowerSaving() {}

    public static void apply(int account, boolean enabled) {
        SharedPreferences global = MessagesController.getGlobalMainSettings();
        int snapshot = global.getInt(LITE, PowerSavingSnapshot.NONE);
        if (enabled) {
            if (snapshot != PowerSavingSnapshot.NONE) return; // already on: keep the first snapshot
            int current = LiteMode.getValue(true);
            int effects = PowerSavingSnapshot.packEffects(
                    BananaConfig.get(account, BananaConfig.Option.SNOW_ENABLED),
                    BananaConfig.get(account, BananaConfig.Option.AVATAR_GLOW_ENABLED),
                    BananaConfig.get(account, BananaConfig.Option.REACTION_GLOW_ENABLED));
            global.edit().putInt(LITE, current).putInt(EFFECTS, effects).apply();
            LiteMode.setAllFlags(PowerSavingSnapshot.liteWhenEnabled(current));
            BananaConfig.set(account, BananaConfig.Option.SNOW_ENABLED, false);
            BananaConfig.set(account, BananaConfig.Option.AVATAR_GLOW_ENABLED, false);
            BananaConfig.set(account, BananaConfig.Option.REACTION_GLOW_ENABLED, false);
        } else {
            if (snapshot == PowerSavingSnapshot.NONE) return;
            int effects = global.getInt(EFFECTS, 0);
            LiteMode.setAllFlags(PowerSavingSnapshot.liteWhenDisabled(LiteMode.getValue(true), snapshot));
            global.edit().remove(LITE).remove(EFFECTS).apply();
            BananaConfig.set(account, BananaConfig.Option.SNOW_ENABLED, (effects & PowerSavingSnapshot.SNOW) != 0);
            BananaConfig.set(account, BananaConfig.Option.AVATAR_GLOW_ENABLED, (effects & PowerSavingSnapshot.AVATAR_GLOW) != 0);
            BananaConfig.set(account, BananaConfig.Option.REACTION_GLOW_ENABLED, (effects & PowerSavingSnapshot.REACTION_GLOW) != 0);
        }
        // setAllFlags only stores the value; what LiteMode's own screen does after a change.
        AndroidUtilities.runOnUIThread(() -> {
            AnimatedEmojiDrawable.updateAll();
            Theme.reloadWallpaper(true);
        });
    }
}
