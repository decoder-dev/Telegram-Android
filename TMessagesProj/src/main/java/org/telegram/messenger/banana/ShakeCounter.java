package org.telegram.messenger.banana;

/** Three shakes within five seconds (ported from BananaGram iOS "Shake to Lock"). */
public final class ShakeCounter {
    public static final long WINDOW_MS = 5000;
    public static final int SHAKES = 3;
    private final long[] times = new long[SHAKES];
    private int count;

    /** Records a shake; true when it completes three in the window, after which the count starts over. */
    public synchronized boolean note(long nowMs) {
        int kept = 0;
        for (int i = 0; i < count; i++) {
            if (nowMs - times[i] <= WINDOW_MS) times[kept++] = times[i];
        }
        count = kept;
        times[count++] = nowMs;
        if (count >= SHAKES) {
            count = 0;
            return true;
        }
        return false;
    }

    public synchronized void reset() {
        count = 0;
    }
}
