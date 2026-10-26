package org.telegram.messenger.banana;

import android.net.Uri;
import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
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
 * Fetches public MTProxy lists and installs them beside manually added proxies.
 * Links use the existing tg://proxy and https://t.me/proxy parser. host:port:secret
 * lines from the same lists are accepted as the same MTProto secret format.
 */
public final class BananaAutomaticProxy {
    private static final String ENABLED = "bananaAutomaticMtProxy";
    private static final String KEYS = "bananaAutomaticMtProxyKeys";
    private static final long REFRESH_MS = 10L * 60L * 1000L;
    private static final long MIN_SWITCH_MS = 60L * 1000L;
    private static final int STORED_LIMIT = 40;
    private static final int PROBE_LIMIT = 20;

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

    private static final Runnable REFRESH = BananaAutomaticProxy::refresh;
    private static long lastSwitchAt;
    private static boolean refreshing;

    private BananaAutomaticProxy() {}

    public static boolean isEnabled() {
        return MessagesController.getGlobalMainSettings().getBoolean(ENABLED, false);
    }

    public static void setEnabled(boolean enabled) {
        MessagesController.getGlobalMainSettings().edit().putBoolean(ENABLED, enabled).apply();
        AndroidUtilities.cancelRunOnUIThread(REFRESH);
        if (enabled) {
            refresh();
        } else {
            AndroidUtilities.runOnUIThread(BananaAutomaticProxy::removeAutomaticExceptManualCurrent);
        }
    }

    public static void init() {
        if (isEnabled()) {
            AndroidUtilities.runOnUIThread(REFRESH, 1500);
        }
    }

    public static void refresh() {
        if (!isEnabled() || refreshing) {
            return;
        }
        refreshing = true;
        Utilities.globalQueue.postRunnable(() -> {
            List<ProxySettings> parsed = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (String listUrl : LIST_URLS) {
                String text = fetch(listUrl);
                if (TextUtils.isEmpty(text)) {
                    continue;
                }
                for (ProxySettings settings : parseList(text)) {
                    String key = key(settings);
                    if (seen.add(key)) {
                        parsed.add(settings);
                    }
                    if (parsed.size() >= STORED_LIMIT) {
                        break;
                    }
                }
                if (parsed.size() >= STORED_LIMIT) {
                    break;
                }
            }
            parsed = preferFakeTls(parsed);
            List<ProxySettings> pool = parsed;
            AndroidUtilities.runOnUIThread(() -> {
                refreshing = false;
                if (!isEnabled()) {
                    return;
                }
                applyPool(pool);
                AndroidUtilities.cancelRunOnUIThread(REFRESH);
                AndroidUtilities.runOnUIThread(REFRESH, REFRESH_MS);
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

    private static void applyPool(List<ProxySettings> pool) {
        SharedConfig.loadProxyList();
        Set<String> nextKeys = new HashSet<>();
        for (ProxySettings settings : pool) {
            nextKeys.add(key(settings));
        }
        Set<String> previous = loadKeys();
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
        for (ProxySettings settings : pool) {
            SharedConfig.ProxyInfo info = SharedConfig.addProxy(new SharedConfig.ProxyInfo(settings));
            added.add(info);
        }
        saveKeys(nextKeys);
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
        probeAndMaybeSwitch(added);
    }

    private static void probeAndMaybeSwitch(List<SharedConfig.ProxyInfo> added) {
        int limit = Math.min(PROBE_LIMIT, added.size());
        if (limit == 0) {
            return;
        }
        final int[] pending = new int[] {limit};
        final List<SharedConfig.ProxyInfo> probed = new ArrayList<>(added.subList(0, limit));
        int account = UserConfig.selectedAccount;
        for (int i = 0; i < limit; i++) {
            SharedConfig.ProxyInfo info = probed.get(i);
            info.checking = true;
            ConnectionsManager.getInstance(account).checkProxy(info.settings, time -> AndroidUtilities.runOnUIThread(() -> {
                info.checking = false;
                info.availableCheckTime = System.currentTimeMillis();
                if (time == -1) {
                    info.available = false;
                    info.ping = 0;
                } else {
                    info.available = true;
                    info.ping = time;
                }
                pending[0]--;
                if (pending[0] == 0) {
                    maybeSwitch(probed);
                }
            }));
        }
    }

    private static void maybeSwitch(List<SharedConfig.ProxyInfo> probed) {
        if (!isEnabled()) {
            return;
        }
        SharedConfig.ProxyInfo best = null;
        for (SharedConfig.ProxyInfo info : probed) {
            if (!info.available || info.ping <= 0) {
                continue;
            }
            if (best == null || fakeTls(info) && !fakeTls(best) || fakeTls(info) == fakeTls(best) && info.ping < best.ping) {
                best = info;
            }
        }
        if (best == null) {
            return;
        }
        SharedConfig.ProxyInfo current = SharedConfig.currentProxy;
        if (current != null && !isAutomatic(current)) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean currentDown = current == null || !current.available || current.ping <= 0;
        boolean faster = current != null && best.ping < current.ping * 0.7;
        if (!currentDown && !faster) {
            return;
        }
        if (!currentDown && now - lastSwitchAt < MIN_SWITCH_MS) {
            return;
        }
        lastSwitchAt = now;
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
            if (info != null) {
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
        return settings.getAddress() + "\n" + settings.getPort() + "\n" + settings.getSecret();
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

    private static boolean fakeTls(SharedConfig.ProxyInfo info) {
        return info != null && fakeTls(info.settings);
    }

    private static boolean fakeTls(ProxySettings settings) {
        String secret = settings.getSecret().toLowerCase(Locale.US);
        return secret.startsWith("ee") && secret.length() >= 34;
    }

    private static boolean usableHost(String host) {
        if (TextUtils.isEmpty(host) || host.length() > 253) {
            return false;
        }
        return host.matches("[A-Za-z0-9_.:\\-\\[\\]]+");
    }

    private static String fetch(String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setInstanceFollowRedirects(true);
            if (connection.getResponseCode() != 200) {
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
}
