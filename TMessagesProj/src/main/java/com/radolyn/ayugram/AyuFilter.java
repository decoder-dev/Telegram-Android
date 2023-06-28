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
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class AyuFilter {
    private static ArrayList<Pattern> patterns;

    public static void rebuildCache() {
        var filters = AyuConfig.getRegexFilters();

        var flags = Pattern.MULTILINE;
        if (AyuConfig.regexFiltersCaseInsensitive) {
            flags |= Pattern.CASE_INSENSITIVE;
        }

        patterns = new ArrayList<>();
        for (var filter : filters) {
            patterns.add(Pattern.compile(filter, flags));
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

        for (var pattern : patterns) {
            if (pattern.matcher(text).find()) {
                return true;
            }
        }

        return false;
    }

    private static boolean isFiltered(ArrayList<TLRPC.MessageEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return false;
        }

        for (var entity : entities) {
            if (entity instanceof TLRPC.TL_messageEntityUrl) {
                if (isFiltered(entity.url)) {
                    return true;
                }
            } else if (entity instanceof TLRPC.TL_messageEntityTextUrl) {
                if (isFiltered(entity.url)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean isFiltered(MessageObject msg) {
        return AyuConfig.regexFiltersEnabled && msg != null && (
                isFiltered(msg.messageText) ||
                        isFiltered(msg.caption) ||
                        isFiltered(msg.messageOwner.entities) ||
                        (
                                msg.isPoll() &&
                                        isFiltered(((TLRPC.TL_messageMediaPoll) msg.messageOwner.media).poll.question)
                        )
        );
    }
}
