package org.telegram.messenger.banana;

import org.telegram.messenger.LiteMode;

/**
 * Enhanced Power Saving (ported from BananaGram iOS): turns off autoplay, animated stickers and emoji,
 * message effects, wallpaper motion and blur, remembering what was on to restore it exactly later.
 */
public final class PowerSavingSnapshot {
    public static final int MASK = LiteMode.FLAG_AUTOPLAY_VIDEOS | LiteMode.FLAG_AUTOPLAY_GIFS | LiteMode.FLAGS_ANIMATED_STICKERS
            | LiteMode.FLAGS_ANIMATED_EMOJI | LiteMode.FLAG_CHAT_BLUR | LiteMode.FLAG_CHAT_BACKGROUND | LiteMode.FLAG_PARTICLES;
    public static final int NONE = -1;
    public static final int SNOW = 1, AVATAR_GLOW = 2, REACTION_GLOW = 4;

    private PowerSavingSnapshot() {}

    public static int liteWhenEnabled(int current) {
        return current & ~MASK;
    }

    /** Restores the masked flags from the snapshot; flags outside the mask keep what the user set meanwhile. */
    public static int liteWhenDisabled(int current, int snapshot) {
        if (snapshot == NONE) return current;
        return (current & ~MASK) | (snapshot & MASK);
    }

    public static int packEffects(boolean snow, boolean avatarGlow, boolean reactionGlow) {
        return (snow ? SNOW : 0) | (avatarGlow ? AVATAR_GLOW : 0) | (reactionGlow ? REACTION_GLOW : 0);
    }
}
