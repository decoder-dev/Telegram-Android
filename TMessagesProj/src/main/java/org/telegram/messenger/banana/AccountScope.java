package org.telegram.messenger.banana;

import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import java.util.concurrent.atomic.AtomicLongArray;

/** Identity of one account lifetime, including logout/relogin with the same user ID. */
public final class AccountScope {
    private static final AtomicLongArray generations = new AtomicLongArray(UserConfig.MAX_ACCOUNT_COUNT);
    public final int account;
    public final long owner;
    public final boolean testBackend;
    private final long generation;

    private AccountScope(int account) {
        this.account = account;
        generation = generations.get(account);
        owner = UserConfig.getInstance(account).getClientUserId();
        testBackend = ConnectionsManager.getInstance(account).isTestBackend();
    }

    public static AccountScope capture(int account) {
        if (account < 0 || account >= UserConfig.MAX_ACCOUNT_COUNT) throw new IllegalArgumentException("Invalid account");
        return new AccountScope(account);
    }

    public static void invalidate(int account) {
        generations.incrementAndGet(account);
    }

    public long generation() { return generation; }

    public boolean isCurrent() {
        return generations.get(account) == generation && UserConfig.getInstance(account).getClientUserId() == owner
                && ConnectionsManager.getInstance(account).isTestBackend() == testBackend;
    }
}
