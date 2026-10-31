package org.telegram.messenger.banana;

import android.net.NetworkInfo;
import android.net.Uri;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.ui.Components.ForegroundDetector;
import org.telegram.utils.proxy.ProxySettings;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Public MTProxy list, ported from iOS {@code ManagedAutomaticMtProxy}.
 *
 * iOS refreshes every 10 minutes and may consider a larger pool. This client probes at most
 * {@link #PROBE_LIMIT} candidates per pass, at most {@link #PROBE_CONCURRENCY} at a time, with
 * {@link #PROBE_GAP_MS} between dials. While enabled it owns routing whatever "Use proxy" was
 * set to and whichever proxy was selected; turning it off brings that earlier state back.
 * Turning "Use proxy" off while it is enabled pauses it instead of being undone by a later switch.
 * The refresh runnable is not re-armed while the process stays in the background, the screen
 * is off, power save is on, or there is no network.
 */
public final class BananaAutomaticProxy {
    private static final String ENABLED = "bananaAutomaticMtProxy";
    private static final String KEYS = "bananaAutomaticMtProxyKeys";
    /** The user turned "Use proxy" off while automatic mode was on. */
    private static final String PAUSED = "bananaAutomaticMtProxyPaused";
    /** Key of the route selected before automatic mode took over, and whether it was in use. */
    private static final String PREVIOUS = "bananaAutomaticMtProxyPrevious";
    private static final String PREVIOUS_ENABLED = "bananaAutomaticMtProxyPreviousEnabled";
    /** {@code automaticMtProxyRefreshInterval}. */
    private static final long REFRESH_MS = 10L * 60L * 1000L;
    /** {@code automaticMtProxySelectionDelay}. */
    private static final long SELECTION_DELAY_MS = 200L;
    /** {@code automaticMtProxyConnectionFallbackDelay}. */
    private static final long CONNECTION_FALLBACK_MS = 6L * 1000L;
    /** {@code automaticMtProxyMinimumSwitchInterval}. */
    private static final long MIN_SWITCH_MS = 60L * 1000L;
    /** {@code automaticMtProxySwitchRttFactor}. */
    private static final double SWITCH_RTT_FACTOR = 0.7d;
    /**
     * Minimum gap before a failure-driven switch. A just-selected route needs time for its
     * handshake; on a slow mobile link it can take longer than the 6 s fallback, and switching
     * again every few seconds reconnects every account and never lets any route connect.
     */
    private static final long FAILURE_SWITCH_MIN_MS = 20L * 1000L;
    private static final int STORED_LIMIT = 40;
    /** Candidates dialed in one refresh. Kept at the concurrency cap so a pass cannot queue a second wave. */
    private static final int PROBE_LIMIT = 4;
    /** {@code ProxyStatusProbeQueue(limit: 4)}. */
    private static final int PROBE_CONCURRENCY = 4;
    /** Extra gap so Android does not open even those four sockets in one burst. */
    private static final long PROBE_GAP_MS = 200L;
    /** Stop the 10-minute refresh after the UI has been gone this long. */
    private static final long LONG_BACKGROUND_MS = 60L * 1000L;

    private static final String[] LIST_URLS = new String[] {
            "https://cdn.jsdelivr.net/gh/SoliSpirit/mtproto@master/all_proxies.txt",
            "https://cdn.jsdelivr.net/gh/kort0881/telegram-proxy-collector@main/proxy_ru.txt",
            "https://cdn.jsdelivr.net/gh/dubblebyte/free-mtproto-proxies@main/all_proxies.txt",
            "https://cdn.jsdelivr.net/gh/Chumbayoumba/free-telegram-proxy-russia-2026@main/proxies.txt",
            "https://raw.gitmirror.com/SoliSpirit/mtproto/master/all_proxies.txt",
            "https://raw.gitmirror.com/kort0881/telegram-proxy-collector/main/proxy_ru.txt",
            "https://raw.gitmirror.com/dubblebyte/free-mtproto-proxies/main/all_proxies.txt",
            "https://raw.gitmirror.com/Chumbayoumba/free-telegram-proxy-russia-2026/main/proxies.txt",
            "https://raw.githubusercontent.com/SoliSpirit/mtproto/master/all_proxies.txt",
            "https://raw.githubusercontent.com/kort0881/telegram-proxy-collector/main/proxy_ru.txt",
            "https://raw.githubusercontent.com/dubblebyte/free-mtproto-proxies/main/all_proxies.txt",
            "https://raw.githubusercontent.com/Chumbayoumba/free-telegram-proxy-russia-2026/main/proxies.txt",
    };

    /** Blocking list downloads must not stall Telegram's shared global queue. */
    private static final DispatchQueue FETCH_QUEUE = new DispatchQueue("banana-proxy-fetch");

    private static final Runnable REFRESH = BananaAutomaticProxy::refresh;
    private static final Runnable PROBE_KICK = BananaAutomaticProxy::kickProbe;
    private static final Runnable SELECT = BananaAutomaticProxy::selectBest;
    private static final Runnable FALLBACK = BananaAutomaticProxy::connectionFallback;

    private static long lastSwitchAt;
    private static long lastRefreshAt;
    private static long backgroundedAt;
    private static boolean refreshing;
    private static volatile int refreshGeneration;
    private static boolean listenersInstalled;
    private static boolean pendingProbe;
    private static boolean failureDrivenSwitch;

    private static int probeGeneration;
    private static int probesInFlight;
    private static int probeCursor;
    private static int probePending;
    private static List<SharedConfig.ProxyInfo> probeQueue;
    private static List<SharedConfig.ProxyInfo> lastAdded = new ArrayList<>();

    private BananaAutomaticProxy() {}

    public static boolean isEnabled() {
        return MessagesController.getGlobalMainSettings().getBoolean(ENABLED, false);
    }

    public static void setEnabled(boolean enabled) {
        setEnabled(enabled, true);
    }

    /** A proxy link the user connects to wins over automatic mode. Call before selecting it. */
    public static void handOverToManual() {
        if (isEnabled()) {
            setEnabled(false, false);
        }
    }

    private static void setEnabled(boolean enabled, boolean restorePrevious) {
        if (enabled == isEnabled()) {
            if (enabled) {
                setPaused(false);
            }
            return;
        }
        // A route the user paused stays off when automatic mode hands back.
        final boolean wasPaused = isPaused();
        android.content.SharedPreferences.Editor editor = MessagesController.getGlobalMainSettings().edit();
        editor.putBoolean(ENABLED, enabled).putBoolean(PAUSED, false);
        if (enabled) {
            SharedConfig.ProxyInfo current = SharedConfig.currentProxy;
            if (current != null && current.settings != null && !isAutomatic(current)) {
                editor.putString(PREVIOUS, key(current.settings)).putBoolean(PREVIOUS_ENABLED, SharedConfig.isProxyEnabled());
            } else {
                editor.remove(PREVIOUS).remove(PREVIOUS_ENABLED);
            }
        }
        editor.apply();
        AndroidUtilities.cancelRunOnUIThread(REFRESH);
        AndroidUtilities.cancelRunOnUIThread(FALLBACK);
        cancelProbes();
        refreshGeneration++;
        refreshing = false;
        pendingProbe = false;
        if (enabled) {
            // iOS clears the dwell stamp when the user turns the feature on.
            lastSwitchAt = 0L;
            refresh();
        } else {
            // Synchronous: a proxy link selected right after handOverToManual() may share a key
            // with the pool and must not be removed by a cleanup that runs after it.
            removeAutomaticExceptManualCurrent();
            if (restorePrevious) {
                restorePrevious(!wasPaused);
            }
            MessagesController.getGlobalMainSettings().edit().remove(PREVIOUS).remove(PREVIOUS_ENABLED).apply();
        }
    }

    /** Whether automatic mode is on and not paused: what "Use proxy" shows while it is on. */
    public static boolean isActive() {
        return isEnabled() && !isPaused();
    }

    /**
     * The "Use proxy" switch while automatic mode is on. Off pauses it and disconnects, so a
     * later probe cannot turn the proxy back on. On resumes it: an automatic route already
     * selected is used at once, otherwise the proxy turns on when a probed route answers.
     */
    public static void setUsed(boolean used) {
        if (!isEnabled()) {
            return;
        }
        SharedConfig.ProxyInfo current = SharedConfig.currentProxy;
        android.content.SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        if (!used) {
            setPaused(true);
            if (preferences.getBoolean("proxy_enabled", false)) {
                preferences.edit().putBoolean("proxy_enabled", false).apply();
                ConnectionsManager.setProxySettings(false, current != null ? current.settings : null);
            }
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
            return;
        }
        boolean automaticSelected = current != null && current.settings != null && isAutomatic(current);
        if (automaticSelected && !SharedConfig.isProxyEnabled()) {
            android.content.SharedPreferences.Editor editor = preferences.edit();
            current.settings.toSharedPreferences(editor);
            editor.putBoolean("proxy_enabled", true).apply();
            ConnectionsManager.setProxySettings(true, current.settings);
        }
        if (isPaused()) {
            setPaused(false);
        } else if (!automaticSelected) {
            refresh();
        }
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
    }

    /** Use proxy was turned off elsewhere (the proxy error dialog): pause automatic mode. */
    public static void onProxyDisabled() {
        if (isEnabled()) {
            setPaused(true);
        }
    }

    private static boolean isPaused() {
        return MessagesController.getGlobalMainSettings().getBoolean(PAUSED, false);
    }

    private static void setPaused(boolean paused) {
        if (paused == isPaused()) {
            return;
        }
        MessagesController.getGlobalMainSettings().edit().putBoolean(PAUSED, paused).apply();
        if (paused) {
            AndroidUtilities.cancelRunOnUIThread(REFRESH);
            AndroidUtilities.cancelRunOnUIThread(FALLBACK);
            cancelProbes();
            pendingProbe = false;
            return;
        }
        lastSwitchAt = 0L;
        if (lastAdded != null && !lastAdded.isEmpty()) {
            startProbe(lastAdded);
        } else {
            refresh();
        }
    }

    /** Puts back the route and "Use proxy" state that automatic mode replaced. */
    private static void restorePrevious(boolean allowUse) {
        android.content.SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        String previousKey = preferences.getString(PREVIOUS, null);
        if (TextUtils.isEmpty(previousKey) || SharedConfig.currentProxy != null) {
            return;
        }
        SharedConfig.ProxyInfo previous = find(previousKey);
        if (previous == null || previous.settings == null) {
            return;
        }
        boolean use = allowUse && preferences.getBoolean(PREVIOUS_ENABLED, false);
        android.content.SharedPreferences.Editor editor = preferences.edit();
        previous.settings.toSharedPreferences(editor);
        editor.putBoolean("proxy_enabled", use);
        editor.apply();
        SharedConfig.currentProxy = previous;
        if (use) {
            ConnectionsManager.setProxySettings(true, previous.settings);
        }
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
    }

    public static void init() {
        installListeners();
        if (isEnabled() && !backgroundedLong()) {
            AndroidUtilities.runOnUIThread(REFRESH, 1500);
        }
    }

    public static void refresh() {
        if (!isEnabled() || refreshing || isPaused()) {
            return;
        }
        if (shouldDeferRefresh()) {
            if (!backgroundedLong()) {
                scheduleRefresh(REFRESH_MS);
            }
            return;
        }
        refreshing = true;
        lastRefreshAt = SystemClock.elapsedRealtime();
        final int generation = ++refreshGeneration;
        FETCH_QUEUE.postRunnable(() -> {
            List<List<ProxySettings>> lists = new ArrayList<>();
            int attempts = 0;
            for (String listUrl : LIST_URLS) {
                if (generation != refreshGeneration || !isEnabled()) {
                    break;
                }
                attempts++;
                String text = fetch(listUrl);
                if (!TextUtils.isEmpty(text)) {
                    List<ProxySettings> parsed = preferFakeTls(parseList(text));
                    if (!parsed.isEmpty()) {
                        lists.add(parsed);
                    }
                }
                List<ProxySettings> merged = mergeRoundRobin(lists);
                // The first four URLs are the four publishers. Stop once the pool is full
                // instead of opening every mirror in parallel the way a naive port would.
                if (merged.size() >= STORED_LIMIT && (lists.size() >= 2 || attempts >= 4)) {
                    break;
                }
                if (!merged.isEmpty() && attempts >= 4) {
                    break;
                }
            }
            List<ProxySettings> pool = mergeRoundRobin(lists);
            AndroidUtilities.runOnUIThread(() -> {
                if (generation != refreshGeneration) {
                    return;
                }
                refreshing = false;
                if (!isEnabled()) {
                    return;
                }
                applyPool(pool);
                if (!backgroundedLong()) {
                    scheduleRefresh(REFRESH_MS);
                }
            });
        });
    }

    static List<ProxySettings> parseList(String text) {
        List<ProxySettings> result = new ArrayList<>();
        String[] tokens = text.split("[\\s\"'<>]+");
        for (String raw : tokens) {
            String token = raw.replaceAll("^[.,;()\\[\\]{}]+|[.,;()\\[\\]{}]+$", "");
            if (token.isEmpty() || token.startsWith("#")) {
                continue;
            }
            ProxySettings settings = parseToken(token);
            if (settings != null && settings.isValid() && settings.getType() == ProxySettings.Type.MTPROTO) {
                result.add(settings);
            }
        }
        return result;
    }

    static ProxySettings parseToken(String token) {
        String lower = token.toLowerCase(Locale.US);
        if (lower.startsWith("tg:") || lower.startsWith("http://") || lower.startsWith("https://")) {
            ProxySettings settings = ProxySettings.fromUri(Uri.parse(token));
            if (settings != null && settings.getType() == ProxySettings.Type.MTPROTO) {
                return settings;
            }
            return null;
        }
        String[] parts = token.split(":");
        if (parts.length < 3) {
            return null;
        }
        String secret = parts[parts.length - 1];
        String portText = parts[parts.length - 2];
        StringBuilder host = new StringBuilder();
        for (int i = 0; i < parts.length - 2; i++) {
            if (i > 0) {
                host.append(':');
            }
            host.append(parts[i]);
        }
        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            return null;
        }
        if (port <= 0 || port > 65535 || TextUtils.isEmpty(secret) || !usableHost(host.toString())) {
            return null;
        }
        return ProxySettings.builder()
                .setType(ProxySettings.Type.MTPROTO)
                .setAddress(host.toString())
                .setPort(port)
                .setSecret(secret)
                .build();
    }

    private static void scheduleRefresh(long delay) {
        AndroidUtilities.cancelRunOnUIThread(REFRESH);
        if (!isEnabled() || isPaused() || backgroundedLong()) {
            return;
        }
        AndroidUtilities.runOnUIThread(REFRESH, Math.max(0L, delay));
    }

    private static boolean shouldDeferRefresh() {
        return backgroundedLong() || isPowerSave() || !hasNetwork();
    }

    private static boolean backgroundedLong() {
        ForegroundDetector detector = ForegroundDetector.getInstance();
        if (detector != null && detector.isForeground()) {
            return false;
        }
        if (backgroundedAt == 0L) {
            return true;
        }
        return SystemClock.elapsedRealtime() - backgroundedAt >= LONG_BACKGROUND_MS;
    }

    private static boolean canProbe() {
        if (!isEnabled() || !ApplicationLoader.isScreenOn || backgroundedLong()) {
            return false;
        }
        if (isPowerSave() || !hasNetwork() || isPaused()) {
            return false;
        }
        return true;
    }

    private static boolean isPowerSave() {
        if (BananaHooks.powerSaving(UserConfig.selectedAccount)) {
            return true;
        }
        try {
            PowerManager pm = (PowerManager) ApplicationLoader.applicationContext.getSystemService(android.content.Context.POWER_SERVICE);
            return pm != null && pm.isPowerSaveMode();
        } catch (Throwable ignore) {
            return false;
        }
    }

    private static boolean hasNetwork() {
        try {
            NetworkInfo info = ApplicationLoader.currentNetworkInfo;
            if (info != null) {
                return info.isConnected();
            }
        } catch (Throwable ignore) {
        }
        int state = ConnectionsManager.getInstance(UserConfig.selectedAccount).getConnectionState();
        return state != ConnectionsManager.ConnectionStateWaitingForNetwork;
    }

    private static void applyPool(List<ProxySettings> pool) {
        SharedConfig.loadProxyList();
        Set<String> previous = loadKeys();
        Set<String> manual = new HashSet<>();
        for (int i = 0; i < SharedConfig.proxyList.size(); i++) {
            SharedConfig.ProxyInfo info = SharedConfig.proxyList.get(i);
            String existing = key(info.settings);
            if (!previous.contains(existing)) {
                manual.add(existing);
            }
        }
        List<ProxySettings> automatic = new ArrayList<>();
        for (ProxySettings settings : pool) {
            if (!manual.contains(key(settings))) {
                automatic.add(settings);
            }
        }
        SharedConfig.ProxyInfo current = SharedConfig.currentProxy;
        if (current != null && previous.contains(key(current.settings)) && !containsSettings(automatic, current.settings)) {
            automatic.add(0, current.settings);
        }
        if (automatic.size() > STORED_LIMIT) {
            automatic = new ArrayList<>(automatic.subList(0, STORED_LIMIT));
        }
        Set<String> nextKeys = new HashSet<>();
        for (ProxySettings settings : automatic) {
            nextKeys.add(key(settings));
        }
        for (String oldKey : previous) {
            if (nextKeys.contains(oldKey)) {
                continue;
            }
            SharedConfig.ProxyInfo info = find(oldKey);
            if (info != null && info != SharedConfig.currentProxy) {
                SharedConfig.deleteProxy(info);
            }
        }
        List<SharedConfig.ProxyInfo> added = new ArrayList<>();
        for (ProxySettings settings : automatic) {
            added.add(SharedConfig.addProxy(new SharedConfig.ProxyInfo(settings)));
        }
        saveKeys(nextKeys);
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
        startProbe(added);
    }

    private static boolean containsSettings(List<ProxySettings> list, ProxySettings settings) {
        String wanted = key(settings);
        for (ProxySettings item : list) {
            if (wanted.equals(key(item))) {
                return true;
            }
        }
        return false;
    }

    private static void startProbe(List<SharedConfig.ProxyInfo> added) {
        cancelProbes();
        pendingProbe = false;
        failureDrivenSwitch = false;
        if (!isEnabled() || added == null || added.isEmpty()) {
            return;
        }
        int limit = Math.min(PROBE_LIMIT, added.size());
        List<SharedConfig.ProxyInfo> probe = new ArrayList<>(added.subList(0, limit));
        SharedConfig.ProxyInfo active = SharedConfig.currentProxy;
        if (active != null && isAutomatic(active) && !probe.contains(active)) {
            if (probe.size() >= PROBE_LIMIT) {
                probe.remove(probe.size() - 1);
            }
            probe.add(0, active);
        }
        lastAdded = probe;
        if (!canProbe()) {
            pendingProbe = true;
            return;
        }
        probeQueue = probe;
        probeCursor = 0;
        probesInFlight = 0;
        probePending = probe.size();
        AndroidUtilities.cancelRunOnUIThread(PROBE_KICK);
        AndroidUtilities.runOnUIThread(PROBE_KICK);
    }

    private static void kickProbe() {
        if (!isEnabled() || probeQueue == null) {
            return;
        }
        if (!canProbe()) {
            pendingProbe = true;
            cancelProbes();
            return;
        }
        if (probesInFlight >= PROBE_CONCURRENCY || probeCursor >= probeQueue.size()) {
            return;
        }
        final int generation = probeGeneration;
        final SharedConfig.ProxyInfo info = probeQueue.get(probeCursor);
        probeCursor++;
        if (info == null || info.settings == null) {
            probePending = Math.max(0, probePending - 1);
            if (probePending == 0) {
                AndroidUtilities.cancelRunOnUIThread(SELECT);
                AndroidUtilities.runOnUIThread(SELECT, SELECTION_DELAY_MS);
            } else if (probeCursor < probeQueue.size()) {
                AndroidUtilities.runOnUIThread(PROBE_KICK, PROBE_GAP_MS);
            }
            return;
        }
        probesInFlight++;
        info.checking = true;
        final int account = UserConfig.selectedAccount;
        ConnectionsManager.getInstance(account).checkProxy(info.settings, time -> AndroidUtilities.runOnUIThread(() -> {
            if (generation != probeGeneration) {
                return;
            }
            info.checking = false;
            info.availableCheckTime = System.currentTimeMillis();
            if (time == -1) {
                info.available = false;
                info.ping = 0;
            } else {
                info.available = true;
                info.ping = time;
            }
            probesInFlight = Math.max(0, probesInFlight - 1);
            probePending = Math.max(0, probePending - 1);
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxyCheckDone, info);
            if (!isEnabled() || generation != probeGeneration) {
                return;
            }
            if (probePending == 0) {
                AndroidUtilities.cancelRunOnUIThread(SELECT);
                AndroidUtilities.runOnUIThread(SELECT, SELECTION_DELAY_MS);
            } else if (probeQueue != null && probeCursor < probeQueue.size()) {
                AndroidUtilities.runOnUIThread(PROBE_KICK, PROBE_GAP_MS);
            }
        }));
        if (probeQueue != null && probeCursor < probeQueue.size() && probesInFlight < PROBE_CONCURRENCY) {
            AndroidUtilities.runOnUIThread(PROBE_KICK, PROBE_GAP_MS);
        }
    }

    private static void selectBest() {
        if (probeQueue == null) {
            return;
        }
        maybeSwitch(probeQueue, failureDrivenSwitch);
    }

    private static void cancelProbes() {
        probeGeneration++;
        if (probeQueue != null) {
            for (int i = 0; i < probeQueue.size(); i++) {
                SharedConfig.ProxyInfo info = probeQueue.get(i);
                if (info != null) {
                    info.checking = false;
                }
            }
        }
        probeQueue = null;
        probeCursor = 0;
        probesInFlight = 0;
        probePending = 0;
        AndroidUtilities.cancelRunOnUIThread(PROBE_KICK);
        AndroidUtilities.cancelRunOnUIThread(SELECT);
    }

    private static void connectionFallback() {
        if (!isEnabled() || !canProbe()) {
            return;
        }
        if (ConnectionsManager.getInstance(UserConfig.selectedAccount).getConnectionState()
                != ConnectionsManager.ConnectionStateConnectingToProxy) {
            return;
        }
        long sinceSwitch = System.currentTimeMillis() - lastSwitchAt;
        if (sinceSwitch < FAILURE_SWITCH_MIN_MS) {
            AndroidUtilities.runOnUIThread(FALLBACK, FAILURE_SWITCH_MIN_MS - sinceSwitch);
            return;
        }
        failureDrivenSwitch = true;
        if (lastAdded != null && !lastAdded.isEmpty()) {
            maybeSwitch(lastAdded, true);
            return;
        }
        failureDrivenSwitch = false;
    }

    private static void maybeSwitch(List<SharedConfig.ProxyInfo> probed, boolean failureDriven) {
        if (!isEnabled() || probed == null || probed.isEmpty()) {
            return;
        }
        if (!canProbe()) {
            pendingProbe = true;
            return;
        }
        SharedConfig.ProxyInfo current = SharedConfig.currentProxy;
        // A hand-added route stays in the list but is replaced: the "Use proxy" switch, not
        // the route that happened to be selected, decides whether automatic mode connects.
        boolean currentAutomatic = current != null && isAutomatic(current);
        boolean currentUsed = currentAutomatic && SharedConfig.isProxyEnabled();
        SharedConfig.ProxyInfo best = null;
        for (int i = 0; i < probed.size(); i++) {
            SharedConfig.ProxyInfo info = probed.get(i);
            if (info == null || (failureDriven && info == current)) {
                continue;
            }
            if (!info.available || info.ping <= 0) {
                continue;
            }
            if (best == null || info.ping < best.ping) {
                best = info;
            }
        }
        // A pass that found no live candidate must not enable a dead proxy.
        if (best == null || best.settings == null || (best == current && currentUsed)) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean currentDown = failureDriven || !currentUsed || !current.available || current.ping <= 0;
        if (!currentDown) {
            if (now - lastSwitchAt < MIN_SWITCH_MS) {
                return;
            }
            if (best.ping >= current.ping * SWITCH_RTT_FACTOR) {
                return;
            }
        }
        lastSwitchAt = now;
        failureDrivenSwitch = false;
        android.content.SharedPreferences.Editor editor = MessagesController.getGlobalMainSettings().edit();
        editor.putBoolean("proxy_enabled", true);
        best.settings.toSharedPreferences(editor);
        editor.apply();
        SharedConfig.currentProxy = best;
        ConnectionsManager.setProxySettings(true, best.settings);
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
    }

    private static void removeAutomaticExceptManualCurrent() {
        SharedConfig.loadProxyList();
        Set<String> keys = loadKeys();
        for (String key : new ArrayList<>(keys)) {
            SharedConfig.ProxyInfo info = find(key);
            if (info != null && info != SharedConfig.currentProxy) {
                SharedConfig.deleteProxy(info);
            } else if (info != null && info == SharedConfig.currentProxy && isAutomatic(info)) {
                SharedConfig.deleteProxy(info);
            }
        }
        saveKeys(new HashSet<>());
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
    }

    private static boolean isAutomatic(SharedConfig.ProxyInfo info) {
        return info != null && loadKeys().contains(key(info.settings));
    }

    private static SharedConfig.ProxyInfo find(String key) {
        for (int i = 0; i < SharedConfig.proxyList.size(); i++) {
            SharedConfig.ProxyInfo info = SharedConfig.proxyList.get(i);
            if (key.equals(key(info.settings))) {
                return info;
            }
        }
        return null;
    }

    private static String key(ProxySettings settings) {
        if (settings == null) {
            return "";
        }
        String address = settings.getAddress() == null ? "" : settings.getAddress();
        String secret = settings.getSecret() == null ? "" : settings.getSecret();
        return address + "\n" + settings.getPort() + "\n" + secret;
    }

    private static Set<String> loadKeys() {
        Set<String> keys = new HashSet<>();
        String raw = MessagesController.getGlobalMainSettings().getString(KEYS, "");
        if (TextUtils.isEmpty(raw)) {
            return keys;
        }
        for (String part : raw.split("\u0001")) {
            if (!part.isEmpty()) {
                keys.add(part);
            }
        }
        return keys;
    }

    private static void saveKeys(Set<String> keys) {
        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            if (sb.length() > 0) {
                sb.append('\u0001');
            }
            sb.append(key);
        }
        MessagesController.getGlobalMainSettings().edit().putString(KEYS, sb.toString()).apply();
    }

    /** Stable round-robin across publishers, then FakeTLS first. Same shape as iOS. */
    private static List<ProxySettings> mergeRoundRobin(List<List<ProxySettings>> lists) {
        List<ProxySettings> result = new ArrayList<>();
        if (lists == null || lists.isEmpty()) {
            return result;
        }
        Set<String> seen = new HashSet<>();
        int[] index = new int[lists.size()];
        boolean progressed = true;
        while (progressed && result.size() < STORED_LIMIT) {
            progressed = false;
            for (int i = 0; i < lists.size(); i++) {
                List<ProxySettings> list = lists.get(i);
                int at = index[i];
                while (at < list.size()) {
                    ProxySettings server = list.get(at);
                    at++;
                    if (seen.add(key(server))) {
                        result.add(server);
                        progressed = true;
                        break;
                    }
                }
                index[i] = at;
                if (result.size() >= STORED_LIMIT) {
                    break;
                }
            }
        }
        return preferFakeTls(result);
    }

    private static List<ProxySettings> preferFakeTls(List<ProxySettings> input) {
        List<ProxySettings> fake = new ArrayList<>();
        List<ProxySettings> rest = new ArrayList<>();
        for (ProxySettings settings : input) {
            if (fakeTls(settings)) {
                fake.add(settings);
            } else {
                rest.add(settings);
            }
        }
        fake.addAll(rest);
        return fake;
    }

    private static boolean fakeTls(ProxySettings settings) {
        if (settings == null || settings.getSecret() == null) {
            return false;
        }
        String secret = settings.getSecret().toLowerCase(Locale.US);
        return secret.startsWith("ee") && secret.length() >= 34;
    }

    private static boolean usableHost(String host) {
        if (TextUtils.isEmpty(host) || host.length() > 253) {
            return false;
        }
        return host.matches("[A-Za-z0-9_.:\\-\\[\\]]+");
    }

    private static void closeQuietly(java.io.InputStream stream) {
        if (stream == null) {
            return;
        }
        try {
            stream.close();
        } catch (Throwable ignore) {
        }
    }

    private static String fetch(String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setInstanceFollowRedirects(true);
            if (connection.getResponseCode() != 200) {
                closeQuietly(connection.getErrorStream());
                return "";
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null && sb.length() < 512_000) {
                    sb.append(line).append('\n');
                }
            }
            return sb.toString();
        } catch (Throwable e) {
            FileLog.e(e);
            return "";
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static void installListeners() {
        if (listenersInstalled) {
            return;
        }
        listenersInstalled = true;
        NotificationCenter.getGlobalInstance().addObserver((id, account, args) -> {
            if (!ApplicationLoader.isScreenOn) {
                if (probeQueue != null) {
                    pendingProbe = true;
                }
                cancelProbes();
                AndroidUtilities.cancelRunOnUIThread(FALLBACK);
            } else if (pendingProbe && isEnabled()) {
                startProbe(lastAdded);
            }
        }, NotificationCenter.screenStateChanged);
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            NotificationCenter.getInstance(i).addObserver((id, account, args) -> {
                if (account != UserConfig.selectedAccount) {
                    return;
                }
                AndroidUtilities.cancelRunOnUIThread(FALLBACK);
                int state = ConnectionsManager.getInstance(account).getConnectionState();
                if (state == ConnectionsManager.ConnectionStateWaitingForNetwork) {
                    if (probeQueue != null) {
                        pendingProbe = true;
                    }
                    cancelProbes();
                    return;
                }
                if (!isEnabled()) {
                    return;
                }
                if (state == ConnectionsManager.ConnectionStateConnectingToProxy) {
                    AndroidUtilities.runOnUIThread(FALLBACK, CONNECTION_FALLBACK_MS);
                }
            }, NotificationCenter.didUpdateConnectionState);
        }
        ForegroundDetector detector = ForegroundDetector.getInstance();
        if (detector != null) {
            detector.addListener(new ForegroundDetector.Listener() {
                @Override
                public void onBecameForeground() {
                    onForeground();
                }

                @Override
                public void onBecameBackground() {
                    onBackground();
                }
            });
        }
    }

    private static void onForeground() {
        backgroundedAt = 0L;
        ApplicationLoader.startPushService();
        if (!isEnabled()) {
            return;
        }
        long elapsed = lastRefreshAt == 0L ? REFRESH_MS : SystemClock.elapsedRealtime() - lastRefreshAt;
        if (elapsed >= REFRESH_MS) {
            AndroidUtilities.cancelRunOnUIThread(REFRESH);
            refresh();
        } else {
            scheduleRefresh(REFRESH_MS - elapsed);
            if (pendingProbe) {
                startProbe(lastAdded);
            }
        }
    }

    private static void onBackground() {
        backgroundedAt = SystemClock.elapsedRealtime();
        if (probeQueue != null) {
            pendingProbe = true;
        }
        cancelProbes();
        AndroidUtilities.cancelRunOnUIThread(FALLBACK);
        ApplicationLoader.startPushService();
    }
}
