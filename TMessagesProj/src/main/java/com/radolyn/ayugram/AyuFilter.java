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
import android.util.LongSparseArray;
import com.exteragram.messenger.utils.ChatUtils;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class AyuFilter {
    private static ArrayList<Pattern> patterns;
    private static LongSparseArray<HashMap<Integer, Boolean>> filteredCache;

    // Read from the chat list and chat (UI), NotificationsController's queue and DownloadController,
    // and rebuilt from settings: every access goes through the class lock.
    public static synchronized void rebuildCache() {
        var filters = AyuConfig.getRegexFilters();

        var flags = Pattern.MULTILINE;
        if (AyuConfig.regexFiltersCaseInsensitive) {
            flags |= Pattern.CASE_INSENSITIVE;
        }

        patterns = new ArrayList<>();
        for (var filter : filters) {
            try {
                patterns.add(Pattern.compile(filter, flags));
            } catch (PatternSyntaxException e) {
                // A stored expression that no longer compiles must not crash every chat list draw.
                FileLog.e(e);
            }
        }

        filteredCache = new LongSparseArray<>();
    }

    private static boolean isFiltered(CharSequence text) {
        if (!AyuConfig.regexFiltersEnabled) {
            return false;
        }

        if (TextUtils.isEmpty(text)) {
            return false;
        }

        for (var pattern : patterns) {
            if (pattern.matcher(text).find()) {
                return true;
            }
        }

        return false;
    }

    public static synchronized boolean isFiltered(MessageObject msg, MessageObject.GroupedMessages group) {
        if (!AyuConfig.regexFiltersEnabled) {
            return false;
        }

        if (msg == null) {
            return false;
        }

        if (patterns == null) {
            rebuildCache();
        }

        Boolean res;

        var cached = filteredCache.get(msg.getDialogId());
        if (cached != null) {
            res = cached.get(msg.getId());
            if (res != null) {
                return res;
            }
        }

        res = isFiltered(ChatUtils.getMessageText(msg, group));

        if (cached == null) {
            // The cache only grew. Drop it after many chats so a long session does not keep
            // every verdict ever computed; entries are cheap to recompute.
            if (filteredCache.size() >= 256) {
                filteredCache.clear();
            }
            cached = new HashMap<>();
            filteredCache.put(msg.getDialogId(), cached);
        }

        cached.put(msg.getId(), res);

        if (group != null && group.messages != null && !group.messages.isEmpty()) {
            for (var m : group.messages) {
                cached.put(m.getId(), res);
            }
        }

        return res;
    }
}
