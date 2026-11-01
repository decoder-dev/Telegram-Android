/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.easy;

import android.os.SystemClock;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SendMessagesHelper;

import java.util.ArrayList;

public class DummyMessageWaiter extends EasyWaiter {
    private final ArrayList<Integer> alreadySent;
    private int sendingId;
    private Runnable lookup;

    public DummyMessageWaiter(int currentAccount) {
        super(currentAccount);

        alreadySent = new ArrayList<>();

        notifications.add(NotificationCenter.messageReceivedByServer);
        notifications.add(NotificationCenter.messageSendError);
        notifications.add(NotificationCenter.messageReceivedByAck);
    }

    public void trySetSendingId(long dialogId) {
        // SendMessagesHelper's collections and notification callbacks belong to the UI thread.
        // The old tight worker loop both raced those collections and burned a CPU core for 3.5s.
        AndroidUtilities.runOnUIThread(() -> {
            if (lookup != null) AndroidUtilities.cancelRunOnUIThread(lookup);
            final long deadline = SystemClock.elapsedRealtime() + 3500;
            lookup = new Runnable() {
                @Override public void run() {
                    if (latch.getCount() == 0) return;
                    int id = SendMessagesHelper.getInstance(currentAccount).getSendingMessageId(dialogId);
                    if (id < 0) {
                        setSendingId(id);
                    } else if (SystemClock.elapsedRealtime() >= deadline) {
                        unsubscribe();
                    } else {
                        AndroidUtilities.runOnUIThread(this, 50);
                    }
                }
            };
            lookup.run();
        });
    }

    private void setSendingId(int sendingId) {
        this.sendingId = sendingId;

        if (!alreadySent.isEmpty()) {
            if (alreadySent.contains(sendingId)) {
                unsubscribe();
            }
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.messageReceivedByAck || id == NotificationCenter.messageReceivedByServer || id == NotificationCenter.messageSendError) {
            var msgId = (Integer) args[0];

            if (sendingId == 0) {
                alreadySent.add(msgId);
                return;
            }

            if (msgId == sendingId) {
                unsubscribe();
            }
        }
    }
}
