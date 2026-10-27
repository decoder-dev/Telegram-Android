package org.telegram.messenger;

import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.os.PowerManager;
import android.os.SystemClock;

import org.telegram.messenger.banana.BananaAutomaticProxy;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.utils.proxy.ProxySettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ProxyRotationController implements NotificationCenter.NotificationCenterDelegate {
    private final static ProxyRotationController INSTANCE = new ProxyRotationController();

    public final static int DEFAULT_TIMEOUT_INDEX = 1;
    public final static List<Integer> ROTATION_TIMEOUTS = Arrays.asList(
            5, 10, 15, 30, 60
    );

    /** iOS {@code proxyRotationCooldownSeconds}. */
    private static final long ROTATE_COOLDOWN_MS = 30L * 1000L;
    /** Same cap as automatic MTProxy probes: four dials, spaced so the radio is not hit at once. */
    private static final int PROBE_CONCURRENCY = 4;
    private static final long PROBE_GAP_MS = 200L;

    private boolean isCurrentlyChecking;
    private int probeGeneration;
    private int probesInFlight;
    private int probeCursor;
    private int probeRemaining;
    private long lastRotateAt;
    private List<SharedConfig.ProxyInfo> probeBatch;
    private final Runnable probeKick = this::kickProbe;

    private Runnable checkProxyAndSwitchRunnable = () -> {
        if (BananaAutomaticProxy.isEnabled() || !canProbeRadios()) {
            isCurrentlyChecking = false;
            return;
        }
        cancelProbeBatch();
        isCurrentlyChecking = true;
        probeBatch = new ArrayList<>();
        for (int i = 0; i < SharedConfig.proxyList.size(); i++) {
            SharedConfig.ProxyInfo proxyInfo = SharedConfig.proxyList.get(i);
            if (proxyInfo.checking || SystemClock.elapsedRealtime() - proxyInfo.availableCheckTime < 2 * 60 * 1000) {
                continue;
            }
            if (proxyInfo.settings.getType() == ProxySettings.Type.WEB) {
                continue;
            }
            probeBatch.add(proxyInfo);
        }

        if (probeBatch.isEmpty()) {
            isCurrentlyChecking = false;
            switchToAvailable();
            return;
        }
        probeCursor = 0;
        probesInFlight = 0;
        probeRemaining = probeBatch.size();
        AndroidUtilities.runOnUIThread(probeKick);
    };

    private void kickProbe() {
        if (!isCurrentlyChecking || probeBatch == null) {
            return;
        }
        if (BananaAutomaticProxy.isEnabled() || !canProbeRadios()) {
            cancelProbeBatch();
            isCurrentlyChecking = false;
            return;
        }
        if (probesInFlight >= PROBE_CONCURRENCY || probeCursor >= probeBatch.size()) {
            return;
        }
        final int generation = probeGeneration;
        final SharedConfig.ProxyInfo proxyInfo = probeBatch.get(probeCursor);
        probeCursor++;
        probesInFlight++;
        proxyInfo.checking = true;
        final int currentAccount = UserConfig.selectedAccount;
        ConnectionsManager.getInstance(currentAccount).checkProxy(proxyInfo.settings, time -> AndroidUtilities.runOnUIThread(() -> {
            if (generation != probeGeneration) {
                return;
            }
            proxyInfo.availableCheckTime = SystemClock.elapsedRealtime();
            proxyInfo.checking = false;
            if (time == -1) {
                proxyInfo.available = false;
                proxyInfo.ping = 0;
            } else {
                proxyInfo.ping = time;
                proxyInfo.available = true;
            }
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxyCheckDone, proxyInfo);
            probesInFlight = Math.max(0, probesInFlight - 1);
            probeRemaining = Math.max(0, probeRemaining - 1);
            if (generation != probeGeneration || !isCurrentlyChecking) {
                return;
            }
            if (probeRemaining == 0) {
                switchToAvailable();
            } else if (probeBatch != null && probeCursor < probeBatch.size()) {
                AndroidUtilities.runOnUIThread(probeKick, PROBE_GAP_MS);
            }
        }));
        if (probeCursor < probeBatch.size() && probesInFlight < PROBE_CONCURRENCY) {
            AndroidUtilities.runOnUIThread(probeKick, PROBE_GAP_MS);
        }
    }

    private void cancelProbeBatch() {
        probeGeneration++;
        if (probeBatch != null) {
            for (int i = 0; i < probeBatch.size(); i++) {
                probeBatch.get(i).checking = false;
            }
        }
        probeBatch = null;
        probeCursor = 0;
        probesInFlight = 0;
        probeRemaining = 0;
        AndroidUtilities.cancelRunOnUIThread(probeKick);
    }

    private boolean canProbeRadios() {
        if (!ApplicationLoader.isScreenOn) {
            return false;
        }
        if (org.telegram.messenger.banana.BananaHooks.powerSaving(UserConfig.selectedAccount)) {
            return false;
        }
        try {
            PowerManager powerManager = (PowerManager) ApplicationLoader.applicationContext.getSystemService(android.content.Context.POWER_SERVICE);
            if (powerManager != null && powerManager.isPowerSaveMode()) {
                return false;
            }
        } catch (Throwable ignore) {
        }
        try {
            NetworkInfo info = ApplicationLoader.currentNetworkInfo;
            if (info != null && !info.isConnected()) {
                return false;
            }
        } catch (Throwable ignore) {
        }
        return true;
    }

    public static void init() {
        INSTANCE.initInternal();
    }

    @SuppressWarnings("ComparatorCombinators")
    private void switchToAvailable() {
        isCurrentlyChecking = false;

        if (BananaAutomaticProxy.isEnabled() || !SharedConfig.proxyRotationEnabled) {
            return;
        }

        long now = SystemClock.elapsedRealtime();
        if (now - lastRotateAt < ROTATE_COOLDOWN_MS) {
            scheduleAnotherLook(ROTATE_COOLDOWN_MS - (now - lastRotateAt));
            return;
        }

        List<SharedConfig.ProxyInfo> sortedList = new ArrayList<>(SharedConfig.proxyList);
        Collections.sort(sortedList, (o1, o2) -> Long.compare(o1.ping, o2.ping));
        for (SharedConfig.ProxyInfo info : sortedList) {
            if (info == SharedConfig.currentProxy || info.checking || !info.available) {
                continue;
            }
            if (info.settings.getType() == ProxySettings.Type.WEB) {
                continue;
            }

            lastRotateAt = now;
            SharedPreferences.Editor editor = MessagesController.getGlobalMainSettings().edit();
            editor.putBoolean("proxy_enabled", true);
            info.settings.toSharedPreferences(editor);
            editor.apply();

            SharedConfig.currentProxy = info;
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxyChangedByRotation);
            ConnectionsManager.setProxySettings(true, SharedConfig.currentProxy.settings);
            return;
        }
        scheduleAnotherLook(0L);
    }

    private void scheduleAnotherLook(long minimumWait) {
        int state = ConnectionsManager.getInstance(UserConfig.selectedAccount).getConnectionState();
        if (state != ConnectionsManager.ConnectionStateConnectingToProxy || BananaAutomaticProxy.isEnabled()) {
            return;
        }
        long timeout = ROTATION_TIMEOUTS.get(SharedConfig.proxyRotationTimeout) * 1000L;
        AndroidUtilities.runOnUIThread(checkProxyAndSwitchRunnable, Math.max(timeout, minimumWait));
    }

    private void initInternal() {
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            NotificationCenter.getInstance(i).addObserver(this, NotificationCenter.didUpdateConnectionState);
        }
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.proxyCheckDone);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.proxySettingsChanged);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.screenStateChanged);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.proxyCheckDone) {
            return;
        } else if (id == NotificationCenter.screenStateChanged) {
            if (!ApplicationLoader.isScreenOn) {
                AndroidUtilities.cancelRunOnUIThread(checkProxyAndSwitchRunnable);
                cancelProbeBatch();
                isCurrentlyChecking = false;
            }
        } else if (id == NotificationCenter.proxySettingsChanged) {
            AndroidUtilities.cancelRunOnUIThread(checkProxyAndSwitchRunnable);
            cancelProbeBatch();
            isCurrentlyChecking = false;
        } else if (id == NotificationCenter.didUpdateConnectionState && account == UserConfig.selectedAccount) {
            if (BananaAutomaticProxy.isEnabled()) {
                AndroidUtilities.cancelRunOnUIThread(checkProxyAndSwitchRunnable);
                cancelProbeBatch();
                isCurrentlyChecking = false;
                return;
            }
            if (!SharedConfig.proxyRotationEnabled || !SharedConfig.isProxyEnabled() || SharedConfig.proxyList.size() <= 1) {
                return;
            }

            int state = ConnectionsManager.getInstance(account).getConnectionState();

            if (state == ConnectionsManager.ConnectionStateConnectingToProxy) {
                if (!isCurrentlyChecking && canProbeRadios()) {
                    AndroidUtilities.runOnUIThread(checkProxyAndSwitchRunnable, ROTATION_TIMEOUTS.get(SharedConfig.proxyRotationTimeout) * 1000L);
                }
            } else {
                AndroidUtilities.cancelRunOnUIThread(checkProxyAndSwitchRunnable);
                cancelProbeBatch();
                isCurrentlyChecking = false;
            }
        }
    }
}
