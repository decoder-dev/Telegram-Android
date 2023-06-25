/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram;

import android.text.TextUtils;
import org.telegram.messenger.MessageObject;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class AyuFilter {
    private static ArrayList<Pattern> patterns;

    public static void rebuildCache() {
        var filters = AyuConfig.getRegexFilters();

        patterns = new ArrayList<>();
        for (var filter: filters) {
            patterns.add(Pattern.compile(filter, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE));
        }
    }

    public static boolean isFiltered(CharSequence text) {
        if (!AyuConfig.regexFiltersEnabled) {
            return false;
        }

        if (TextUtils.isEmpty(text)) {
            return false;
        }

        if (patterns == null) {
            rebuildCache();
        }

        for (var pattern: patterns) {
            if (pattern.matcher(text).find()) {
                return true;
            }
        }

        return false;
    }

    public static boolean isFiltered(MessageObject msg) {
        return AyuConfig.regexFiltersEnabled && (isFiltered(msg.messageText) || isFiltered(msg.caption));
    }
}
