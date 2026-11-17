/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram;

import android.os.Looper;
import android.text.TextUtils;
import android.util.LongSparseArray;
import com.exteragram.messenger.utils.ChatUtils;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.banana.IsolatedMatch;
import org.telegram.messenger.banana.MessageFilters;
import org.telegram.ui.Components.Bulletin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class AyuFilter {
    // Immutable snapshot, replaced as a whole by rebuildCache().
    private static List<Pattern> patterns;
    // Keyed by dialog, then by account and message id: two accounts can share dialog and message ids.
    private static LongSparseArray<HashMap<Long, Boolean>> filteredCache;
    // Messages whose verdict the isolated worker is computing for the UI thread.
    private static final HashSet<String> pending = new HashSet<>();
    private static long revision;
    private static final int MAX_PENDING = 24;

    // User expressions never run in this process: Android's regex engine cannot be interrupted, so
    // one like (a|aa)+$ would freeze the chat list (UI thread) or the notifications queue. They run
    // in IsolatedRegexService, which is killed when an expression takes too long. Nothing blocks
    // while holding the class lock.
    public static synchronized void rebuildCache() {
        var filters = AyuConfig.getRegexFilters();

        var flags = Pattern.MULTILINE;
        if (AyuConfig.regexFiltersCaseInsensitive) {
            flags |= Pattern.CASE_INSENSITIVE;
        }

        ArrayList<Pattern> compiled = new ArrayList<>();
        for (var filter : filters) {
            try {
                // Compiling is linear in the expression; matching is what can run away.
                compiled.add(Pattern.compile(filter, flags));
            } catch (PatternSyntaxException e) {
                // A stored expression that no longer compiles must not crash every chat list draw.
                FileLog.e(e);
            }
        }

        patterns = Collections.unmodifiableList(compiled);
        filteredCache = new LongSparseArray<>();
        pending.clear();
        revision++;
        // "Skipped until you edit the filters": an edit gives every expression another chance.
        IsolatedMatch.clearQuarantine();
    }

    private static void reportSlow(String expression) {
        FileLog.e("AyuFilter: skipping an expression that took too long to match: " + expression);
        // The switch stays on in settings, so say which expression stopped working.
        AndroidUtilities.runOnUIThread(() -> NotificationCenter.getGlobalInstance().postNotificationName(
                NotificationCenter.showBulletin, Bulletin.TYPE_ERROR,
                LocaleController.formatString(R.string.AyuFilterTooSlow,
                        expression.length() > 40 ? expression.substring(0, 40) + "…" : expression)));
    }

    /** Off the main thread: waits for the worker. Null when the answer is unknown (fail open, not cached). */
    private static Boolean matchBlocking(List<Pattern> snapshot, String text) {
        for (Pattern pattern : snapshot) {
            boolean wasQuarantined = IsolatedMatch.isQuarantined(pattern.pattern(), pattern.flags());
            IsolatedMatch.Outcome outcome = IsolatedMatch.findBlocking(pattern.pattern(), pattern.flags(), text);
            if (outcome == IsolatedMatch.Outcome.MATCHED) {
                return true;
            } else if (outcome == IsolatedMatch.Outcome.UNKNOWN) {
                // The worker is unavailable or busy: do not wait again for every remaining pattern.
                return null;
            } else if (outcome == IsolatedMatch.Outcome.QUARANTINE && !wasQuarantined) {
                reportSlow(pattern.pattern());
            }
        }
        return Boolean.FALSE;
    }

    private static synchronized void store(long expectedRevision, MessageObject msg, MessageObject.GroupedMessages group, boolean res) {
        if (expectedRevision != revision || filteredCache == null) {
            return;
        }
        var cached = filteredCache.get(msg.getDialogId());
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
    }

    /**
     * On the UI thread an unknown verdict reads as "visible" and is computed asynchronously; the
     * chat and chat list refresh when a message turns out to be filtered. Background callers
     * (notifications, downloads) wait for the isolated worker and fail open.
     */
    public static boolean isFiltered(MessageObject msg, MessageObject.GroupedMessages group) {
        if (!AyuConfig.regexFiltersEnabled) {
            return false;
        }

        if (msg == null) {
            return false;
        }

        final List<Pattern> snapshot;
        final long expectedRevision;
        synchronized (AyuFilter.class) {
            if (patterns == null) {
                rebuildCache();
            }
            var cached = filteredCache.get(msg.getDialogId());
            if (cached != null) {
                Boolean res = cached.get(cacheKey(msg));
                if (res != null) {
                    return res;
                }
            }
            snapshot = patterns;
            expectedRevision = revision;
        }

        CharSequence chars = ChatUtils.getMessageText(msg, group);
        if (snapshot.isEmpty() || TextUtils.isEmpty(chars)) {
            store(expectedRevision, msg, group, false);
            return false;
        }
        String text = chars.toString();

        if (Looper.myLooper() != Looper.getMainLooper()) {
            Boolean res = matchBlocking(snapshot, text);
            if (res == null) {
                return false;
            }
            store(expectedRevision, msg, group, res);
            return res;
        }

        String pendingKey = msg.getDialogId() + ":" + cacheKey(msg);
        synchronized (AyuFilter.class) {
            // The worker has one thread and a bounded queue: rows past this limit are evaluated
            // when they are bound again after the current ones finish.
            if (expectedRevision != revision || pending.size() >= MAX_PENDING || !pending.add(pendingKey)) {
                return false;
            }
        }
        final int account = msg.currentAccount;
        IsolatedMatch.anyAsync(snapshot, text, AyuFilter::reportSlow, res -> {
            synchronized (AyuFilter.class) {
                pending.remove(pendingKey);
            }
            if (res == null) {
                return;
            }
            store(expectedRevision, msg, group, res);
            if (res) {
                // Rebinds the chat rows (ChatActivity listens) and reloads the chat list.
                MessageFilters.refreshViews(account);
            }
        });
        return false;
    }

    private static long cacheKey(MessageObject msg) {
        return ((long) msg.currentAccount << 32) | (msg.getId() & 0xffffffffL);
    }
}
