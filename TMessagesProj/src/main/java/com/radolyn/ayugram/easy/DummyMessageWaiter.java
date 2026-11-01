/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.easy;

import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;

import java.util.HashSet;
import java.util.Set;

/** Tracks IDs supplied at creation, never another send's entry in the same dialog. */
public class DummyMessageWaiter extends EasyWaiter implements SendMessagesHelper.SendObserver {
    private final Set<Integer> created = new HashSet<>(), pending = new HashSet<>();
    private final int expectedCount;
    private final boolean awaitServer;
    private final SendMessagesHelper.SendObserver batch;
    private final long owner;
    private final boolean testBackend;

    public DummyMessageWaiter(int currentAccount) { this(currentAccount, 1); }

    public DummyMessageWaiter(int currentAccount, int expectedCount) {
        this(currentAccount, expectedCount, true);
    }

    public DummyMessageWaiter(int currentAccount, int expectedCount, boolean awaitServer) {
        this(currentAccount, expectedCount, awaitServer, null);
    }

    public DummyMessageWaiter(int currentAccount, int expectedCount, boolean awaitServer, SendMessagesHelper.SendObserver batch) {
        super(currentAccount);
        if (expectedCount < 1) throw new IllegalArgumentException("No messages to send");
        this.expectedCount = expectedCount;
        this.awaitServer = awaitServer;
        this.batch = batch;
        owner = UserConfig.getInstance(currentAccount).getClientUserId();
        testBackend = ConnectionsManager.getInstance(currentAccount).isTestBackend();
        notifications.add(NotificationCenter.messageReceivedByServer);
        notifications.add(NotificationCenter.messageSendError);
        notifications.add(NotificationCenter.appDidLogout);
    }

    @Override
    public boolean isActive() {
        if (isClosed()) return false;
        if (batch != null && !batch.isActive()) { onError(); return false; }
        if (UserConfig.getInstance(currentAccount).getClientUserId() != owner ||
                ConnectionsManager.getInstance(currentAccount).isTestBackend() != testBackend) {
            fail("Forward account changed");
            return false;
        }
        return true;
    }

    @Override
    public void onMessageCreated(int messageId) {
        if (!isActive()) return;
        if (messageId >= 0) { onError(); return; }
        if (batch != null) batch.onMessageCreated(messageId);
        if (created.add(messageId)) pending.add(messageId);
        if (!awaitServer && created.size() >= expectedCount) unsubscribe();
    }

    @Override
    public void onError() {
        if (batch != null) batch.onError();
        fail("Forward send failed");
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (account != currentAccount) return;
        if (id == NotificationCenter.appDidLogout) { fail("Forward account changed"); return; }
        if (args.length == 0 || !(args[0] instanceof Integer)) return;
        int messageId = (Integer) args[0];
        if (!pending.contains(messageId)) return;
        if (id == NotificationCenter.messageSendError) onError();
        else if (id == NotificationCenter.messageReceivedByServer) {
            pending.remove(messageId);
            if (created.size() >= expectedCount && pending.isEmpty()) unsubscribe();
        }
    }
}
