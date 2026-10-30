/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.utils;

public class AyuStateVariable {
    private final Object sync = new Object();

    public boolean val;
    public int resetAfter;

    /** Sets both fields under the lock, so process() never sees a new value with an old counter. */
    public void set(boolean value, int resetAfter) {
        synchronized (sync) {
            this.val = value;
            this.resetAfter = resetAfter;
        }
    }

    public boolean process() {
        synchronized (sync) {
            if (resetAfter == -1) {
                return val;
            }

            resetAfter -= 1;
            var currentVal = val;

            if (resetAfter == 0) {
                val = false;
            }

            return currentVal;
        }
    }
}
