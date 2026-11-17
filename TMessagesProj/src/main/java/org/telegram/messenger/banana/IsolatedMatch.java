package org.telegram.messenger.banana;

import android.os.Looper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.telegram.messenger.ApplicationLoader;

/**
 * User expressions evaluated in {@link IsolatedRegexService}, for the callers that need one verdict
 * per message (notifications, downloads, the legacy AyuGram filter).
 *
 * Android's java.util.regex hands a String to ICU and cannot be interrupted, so an expression like
 * (a|aa)+$ cannot be bounded in this process: only killing the isolated worker stops it.
 */
public final class IsolatedMatch {
    /** How long a background caller waits for the worker, including a cold bind. Fail open after. */
    public static final long BLOCKING_WAIT_MS = 1500;
    private static final int QUARANTINE_LIMIT = 256;

    public enum Outcome { MATCHED, NOT_MATCHED, UNKNOWN, QUARANTINE }

    private static RegexEngine engine;
    // Expressions that timed out or do not compile, keyed with their flags. Bounded and app-wide.
    private static final Map<String, Boolean> quarantine = Collections.synchronizedMap(
            new LinkedHashMap<String, Boolean>(32, .75f, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > QUARANTINE_LIMIT;
                }
            });

    private IsolatedMatch() {}

    /** Pure mapping of a worker result, shared by the blocking and asynchronous paths. */
    public static Outcome classify(int status, boolean matched) {
        switch (status) {
            case RegexProtocol.OK: return matched ? Outcome.MATCHED : Outcome.NOT_MATCHED;
            case RegexProtocol.TIMEOUT:
            case RegexProtocol.BAD_PATTERN:
                return Outcome.QUARANTINE;
            default:
                // UNAVAILABLE, BUSY, INPUT_LIMIT, EVALUATION_ERROR: transient or this input only.
                return Outcome.UNKNOWN;
        }
    }

    public static String key(String expression, int flags) {
        return flags + "\n" + expression;
    }

    public static boolean isQuarantined(String expression, int flags) {
        return quarantine.containsKey(key(expression, flags));
    }

    public static void clearQuarantine() {
        quarantine.clear();
    }

    private static synchronized RegexEngine engine() {
        if (engine == null) engine = new RegexEngine(ApplicationLoader.applicationContext);
        return engine;
    }

    private static Outcome record(String expression, int flags, RegexEngine.Result result) {
        boolean matched = result.status == RegexProtocol.OK && result.matches != null
                && result.matches.length > 0 && result.matches[0];
        Outcome outcome = classify(result.status, matched);
        if (outcome == Outcome.QUARANTINE) quarantine.put(key(expression, flags), Boolean.TRUE);
        return outcome;
    }

    /**
     * Matches one text off the main thread. Returns {@link Outcome#UNKNOWN} when the worker did not
     * answer within {@link #BLOCKING_WAIT_MS}; callers fail open.
     */
    public static Outcome findBlocking(String expression, int flags, String text) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("IsolatedMatch.findBlocking on the main thread");
        }
        if (isQuarantined(expression, flags)) return Outcome.QUARANTINE;
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Outcome> outcome = new AtomicReference<>(Outcome.UNKNOWN);
        engine().match(expression, flags, new String[]{text}, result -> {
            outcome.set(record(expression, flags, result));
            done.countDown();
        });
        try {
            if (!done.await(BLOCKING_WAIT_MS, TimeUnit.MILLISECONDS)) return Outcome.UNKNOWN;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return Outcome.UNKNOWN;
        }
        return outcome.get();
    }

    /**
     * Whether any pattern matches, evaluated one expression at a time. The callback runs on the main
     * thread with TRUE/FALSE, or null when a transient failure left the answer unknown. Expressions
     * quarantined during the run are reported through {@code quarantined}.
     */
    public static void anyAsync(List<Pattern> patterns, String text, Consumer<String> quarantined, Consumer<Boolean> callback) {
        next(new ArrayList<>(patterns), 0, text, false, quarantined, callback);
    }

    private static void next(ArrayList<Pattern> patterns, int index, String text, boolean unknown,
                             Consumer<String> quarantined, Consumer<Boolean> callback) {
        while (index < patterns.size() && isQuarantined(patterns.get(index).pattern(), patterns.get(index).flags())) index++;
        if (index >= patterns.size()) {
            callback.accept(unknown ? null : Boolean.FALSE);
            return;
        }
        Pattern pattern = patterns.get(index);
        final int current = index;
        engine().match(pattern.pattern(), pattern.flags(), new String[]{text}, result -> {
            Outcome outcome = record(pattern.pattern(), pattern.flags(), result);
            if (outcome == Outcome.MATCHED) {
                callback.accept(Boolean.TRUE);
                return;
            }
            if (outcome == Outcome.QUARANTINE && quarantined != null) quarantined.accept(pattern.pattern());
            next(patterns, current + 1, text, unknown || outcome == Outcome.UNKNOWN, quarantined, callback);
        });
    }
}
