/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.easy;

import android.util.Log;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.NotificationCenter;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

// music for real coders
// https://music.youtube.com/watch?v=Cw_tD7MXeEI
// https://music.youtube.com/watch?v=uLxUmn3qOjA
public abstract class EasyWaiter implements NotificationCenter.NotificationCenterDelegate {

    protected final ArrayList<Integer> notifications;
    protected final CountDownLatch latch;
    protected final int currentAccount;

    public EasyWaiter(int currentAccount) {
        this.currentAccount = currentAccount;
        this.notifications = new ArrayList<>();

        latch = new CountDownLatch(1);
    }

    public void subscribe() {
        if (notifications.isEmpty()) {
            Log.e("AyuGram", "saving some hours of debugging just for you <3");
            throw new IllegalStateException("NO NOTIFICATIONS SPECIFIED");
        }

        if (Thread.currentThread() != ApplicationLoader.applicationHandler.getLooper().getThread()) {
            var subscribeLatch = new CountDownLatch(1);

            AndroidUtilities.runOnUIThread(() -> {
                for (var notification : notifications) {
                    NotificationCenter.getInstance(currentAccount).addObserver(this, notification);
                }

                subscribeLatch.countDown();
            });

            try {
                subscribeLatch.await();
            } catch (InterruptedException e) {
                Log.w("AyuGran", "wtf", e);
            }
        } else {
            for (var notification : notifications) {
                NotificationCenter.getInstance(currentAccount).addObserver(this, notification);
            }
        }
    }

    protected void unsubscribe() {
        if (Thread.currentThread() != ApplicationLoader.applicationHandler.getLooper().getThread()) {
            var unsubscribeLatch = new CountDownLatch(1);

            AndroidUtilities.runOnUIThread(() -> {
                for (var notification : notifications) {
                    NotificationCenter.getInstance(currentAccount).removeObserver(this, notification);
                }

                unsubscribeLatch.countDown();
            });

            try {
                unsubscribeLatch.await();
            } catch (InterruptedException e) {
                Log.w("AyuGran", "wtf", e);
            }
        } else {
            for (var notification : notifications) {
                NotificationCenter.getInstance(currentAccount).removeObserver(this, notification);
            }
        }

        latch.countDown();
    }

    // Long enough for a large download or upload on a slow link. Without a bound, a send or load
    // that never reports success or failure kept this thread and its observers alive forever.
    private static final long MAX_WAIT_MINUTES = 15;

    public void await() {
        try {
            if (!latch.await(MAX_WAIT_MINUTES, TimeUnit.MINUTES)) {
                Log.w("AyuGram", getClass().getSimpleName() + " timed out; continuing without its event");
                unsubscribe();
            }
        } catch (InterruptedException e) {
            Log.w("AyuGram", "wait interrupted", e);
            Thread.currentThread().interrupt();
            unsubscribe();
        }
    }
}
