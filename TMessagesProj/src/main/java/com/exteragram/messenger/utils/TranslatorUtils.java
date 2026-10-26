package com.exteragram.messenger.utils;

import androidx.core.util.Consumer;

import org.telegram.messenger.AndroidUtilities;

/**
 * Stub translator helpers used by updater UI.
 */
public final class TranslatorUtils {
    private TranslatorUtils() {}

    public static String formatUserAgent() {
        return "BananaGram-Android";
    }

    public static void translate(String text, String language, Consumer<String> callback) {
        translate(text, language, callback, null);
    }

    public static void translate(String text, String language, Consumer<String> callback, Runnable onError) {
        if (callback == null) {
            return;
        }
        AndroidUtilities.runOnUIThread(() -> callback.accept(text));
    }
}
