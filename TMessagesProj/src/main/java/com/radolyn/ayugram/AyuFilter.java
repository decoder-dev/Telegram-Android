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
    // Keyed by dialog, then by account and message id: two accounts can share dialog and message ids.
    private static LongSparseArray<HashMap<Long, Boolean>> filteredCache;

    // Matched on the UI thread (chat list, chat) and in notifications: an expression that blows
    // its time budget is dropped until the filters change instead of freezing the app.
    private static final long MATCH_BUDGET_NS = 20_000_000L;

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

        for (int i = 0; i < patterns.size(); i++) {
            Pattern pattern = patterns.get(i);
            Boolean matched = org.telegram.messenger.banana.BoundedMatch.find(pattern, text, MATCH_BUDGET_NS);
            if (matched == null) {
                FileLog.e("AyuFilter: dropping an expression that took too long to match: " + pattern.pattern());
                patterns.remove(i--);
                filteredCache = new LongSparseArray<>();
            } else if (matched) {
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
            res = cached.get(cacheKey(msg));
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

        cached.put(cacheKey(msg), res);

        if (group != null && group.messages != null && !group.messages.isEmpty()) {
            for (var m : group.messages) {
                cached.put(cacheKey(m), res);
            }
        }

        return res;
    }

    private static long cacheKey(MessageObject msg) {
        return ((long) msg.currentAccount << 32) | (msg.getId() & 0xffffffffL);
    }
}
