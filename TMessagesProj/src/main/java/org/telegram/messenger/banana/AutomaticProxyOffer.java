package org.telegram.messenger.banana;

import android.os.SystemClock;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.LaunchActivity;

/**
 * Offers Auto MTProxy when Telegram cannot be reached or keeps falling off, so a user who
 * never found the switch in the proxy settings gets it where the trouble shows.
 */
public final class AutomaticProxyOffer {
    private static final String OFFERED_AT = "bananaAutomaticProxyOfferedAt";
    private static final long STUCK_MS = 15_000L;
    /** An offer the user let pass is not repeated for a day. */
    private static final long QUIET_MS = 24L * 60L * 60L * 1000L;

    private static final ProxyRoutePolicy.Trouble trouble = new ProxyRoutePolicy.Trouble(STUCK_MS, 3, 2L * 60L * 1000L);
    private static final Runnable CHECK = AutomaticProxyOffer::check;

    private AutomaticProxyOffer() {}

    /** The connection state of the account on screen; UI thread. */
    public static void onConnectionState(int state) {
        long now = SystemClock.elapsedRealtime();
        if (state == ConnectionsManager.ConnectionStateWaitingForNetwork) {
            trouble.reset();
        } else if (state == ConnectionsManager.ConnectionStateConnected || state == ConnectionsManager.ConnectionStateUpdating) {
            trouble.connected();
        } else {
            trouble.connecting(now);
        }
        AndroidUtilities.cancelRunOnUIThread(CHECK);
        long due = trouble.dueIn(now);
        if (due == 0) {
            check();
        } else if (due > 0) {
            AndroidUtilities.runOnUIThread(CHECK, due);
        }
    }

    private static void check() {
        long due = trouble.dueIn(SystemClock.elapsedRealtime());
        if (due != 0) {
            if (due > 0) {
                AndroidUtilities.runOnUIThread(CHECK, due);
            }
            return;
        }
        if (BananaAutomaticProxy.isEnabled() || !LaunchActivity.isActive || ApplicationLoader.mainInterfacePaused) {
            return;
        }
        long now = System.currentTimeMillis();
        long offeredAt = MessagesController.getGlobalMainSettings().getLong(OFFERED_AT, 0);
        if (offeredAt > 0 && offeredAt <= now && now - offeredAt < QUIET_MS) {
            return;
        }
        trouble.reset();
        MessagesController.getGlobalMainSettings().edit().putLong(OFFERED_AT, now).apply();
        BulletinFactory.global().createSimpleBulletin(R.raw.chats_infotip,
                LocaleController.getString(R.string.BananaAutomaticProxyOffer),
                LocaleController.getString(R.string.BananaAutomaticProxyOfferInfo),
                LocaleController.getString(R.string.BananaAutomaticProxyOfferEnable),
                () -> BananaAutomaticProxy.setEnabled(true)).show();
    }
}
