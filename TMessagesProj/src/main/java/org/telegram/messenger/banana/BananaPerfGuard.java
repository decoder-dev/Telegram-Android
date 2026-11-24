package org.telegram.messenger.banana;

import android.os.Looper;
import android.util.Printer;
import android.widget.Toast;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LiteMode;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;

/**
 * Main-thread freeze watchdog. The UI looper reports every dispatch; when heavy dispatches
 * (over 250 ms each) keep coming (8 within 10 s), the app is visibly freezing, so the two
 * heaviest cosmetic layers — liquid glass and chat blur — are switched off once per install
 * and a toast tells the user. Both stay re-enableable in Settings → Power Saving; if they
 * were already off there is nothing to degrade and the watchdog only logs.
 *
 * The looper allows a single printer, and nothing else in the app sets one (verified
 * 2026-11-24). The printer runs on the main thread; everything here is a few integer ops
 * per dispatch.
 */
public final class BananaPerfGuard implements Printer {
    private static final long HEAVY_DISPATCH_MS = 250;
    private static final long WINDOW_MS = 10_000;
    private static final int HEAVY_LIMIT = 8;
    private static final int DEGRADE_MASK = LiteMode.FLAG_LIQUID_GLASS | LiteMode.FLAG_CHAT_BLUR;
    private static final String PREF_DEGRADED = "bananaPerfGuardDegraded";
    private static final String DISPATCH_PREFIX = ">>>>>";

    private static final BananaPerfGuard INSTANCE = new BananaPerfGuard();

    /** Rolling-window counter of heavy dispatches; pure logic, unit-tested on the JVM. */
    static final class RollingCounter {
        private final long windowMs;
        private final int limit;
        private long windowStart;
        private int count;

        RollingCounter(long windowMs, int limit) {
            this.windowMs = windowMs;
            this.limit = limit;
        }

        /** Returns true once per burst: the window that crossed the limit is closed and a new one starts. */
        boolean add(long now, boolean heavy) {
            if (!heavy) return false;
            if (now - windowStart > windowMs) {
                windowStart = now;
                count = 0;
            }
            if (++count < limit) return false;
            count = 0;
            windowStart = now;
            return true;
        }
    }

    private final RollingCounter counter = new RollingCounter(WINDOW_MS, HEAVY_LIMIT);
    private long dispatchStart;

    private BananaPerfGuard() {}

    public static void install() {
        Looper.getMainLooper().setMessageLogging(INSTANCE);
    }

    @Override
    public void println(String line) {
        if (line == null) return;
        if (line.charAt(0) == '>') {
            // ">>>>> Dispatching to ..."
            dispatchStart = android.os.SystemClock.uptimeMillis();
            return;
        }
        if (dispatchStart == 0) return;
        long elapsed = android.os.SystemClock.uptimeMillis() - dispatchStart;
        dispatchStart = 0;
        if (counter.add(android.os.SystemClock.uptimeMillis(), elapsed >= HEAVY_DISPATCH_MS)) {
            degrade();
        }
    }

    private void degrade() {
        if (MessagesController.getGlobalMainSettings().getBoolean(PREF_DEGRADED, false)) return;
        int flags = LiteMode.getValue(true);
        if ((flags & DEGRADE_MASK) == 0) {
            // Nothing heavy left to switch off; keep the evidence for support.
            FileLog.w("BananaPerfGuard: sustained UI freezes with glass and blur already off");
            return;
        }
        LiteMode.setAllFlags(flags & ~DEGRADE_MASK);
        MessagesController.getGlobalMainSettings().edit().putBoolean(PREF_DEGRADED, true).apply();
        FileLog.w("BananaPerfGuard: sustained UI freezes, disabled liquid glass and chat blur");
        try {
            Toast.makeText(ApplicationLoader.applicationContext,
                    LocaleController.getString(R.string.BananaPerfGuardDegraded), Toast.LENGTH_LONG).show();
        } catch (Throwable ignored) {
            // A toast must never become its own freeze source.
        }
    }
}
