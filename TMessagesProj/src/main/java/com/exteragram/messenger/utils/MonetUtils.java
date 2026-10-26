package com.exteragram.messenger.utils;

import android.graphics.Color;

/**
 * Stub Monet color helper — returns neutral fallbacks.
 */
public final class MonetUtils {
    private MonetUtils() {}

    public static int getColor(String token) {
        if (token == null) {
            return Color.GRAY;
        }
        if (token.contains("800") || token.contains("700")) {
            return 0xFF424242;
        }
        return 0xFFE0E0E0;
    }
}
