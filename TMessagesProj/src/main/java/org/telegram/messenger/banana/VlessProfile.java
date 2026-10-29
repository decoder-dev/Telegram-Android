package org.telegram.messenger.banana;

import android.net.Uri;
import android.text.TextUtils;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Strict subset of a vless:// share link. Does not replace SOCKS5 or MTProto proxies. */
public final class VlessProfile {
    private static final String PREF = "bananaVlessUrl";
    private static final int MAX_LENGTH = 8192;

    public final String url;
    public final String userId;
    public final String host;
    public final int port;
    public final String transport;
    public final String security;

    private VlessProfile(String url, String userId, String host, int port, String transport, String security) {
        this.url = url;
        this.userId = userId;
        this.host = host;
        this.port = port;
        this.transport = transport;
        this.security = security;
    }

    public static String load() {
        return MessagesController.getGlobalMainSettings().getString(PREF, "");
    }

    public static void save(String url) {
        MessagesController.getGlobalMainSettings().edit().putString(PREF, url == null ? "" : url.trim()).apply();
    }

    public String summary() {
        return host + ":" + port + " · " + transport.toUpperCase(Locale.US) + " · " + security.toUpperCase(Locale.US);
    }

    public static VlessProfile parse(String raw) {
        if (raw == null) {
            throw error("empty");
        }
        String url = raw.trim();
        if (url.isEmpty()) {
            throw error("empty");
        }
        if (url.length() > MAX_LENGTH) {
            throw error("tooLong");
        }
        if (!url.toLowerCase(Locale.US).startsWith("vless://")) {
            throw error("scheme");
        }
        int hash = url.indexOf('#');
        if (hash >= 0) {
            url = url.substring(0, hash);
        }
        Uri uri = Uri.parse(url);
        String userId = uri.getUserInfo();
        if (!isUuid(userId)) {
            throw error("uuid");
        }
        String host = uri.getHost();
        if (TextUtils.isEmpty(host)) {
            throw error("host");
        }
        int port = uri.getPort();
        if (port <= 0 || port > 65535) {
            throw error("port");
        }
        Set<String> seen = new HashSet<>();
        String query = uri.getEncodedQuery();
        if (query != null) {
            for (String part : query.split("&")) {
                int eq = part.indexOf('=');
                String name = eq >= 0 ? part.substring(0, eq) : part;
                if (!seen.add(name)) {
                    throw error("duplicate");
                }
            }
        }
        String transport = param(uri, "type", "tcp").toLowerCase(Locale.US);
        if (!transport.equals("tcp") && !transport.equals("ws") && !transport.equals("grpc")
                && !transport.equals("httpupgrade") && !transport.equals("xhttp")) {
            throw error("transport");
        }
        String security = param(uri, "security", "none").toLowerCase(Locale.US);
        if (!security.equals("none") && !security.equals("tls") && !security.equals("reality")) {
            throw error("security");
        }
        if (security.equals("reality") && TextUtils.isEmpty(param(uri, "pbk", ""))) {
            throw error("pbk");
        }
        String flow = param(uri, "flow", "");
        if (!flow.isEmpty() && !flow.equals("xtls-rprx-vision")) {
            throw error("flow");
        }
        String fp = param(uri, "fp", "");
        if (!fp.isEmpty() && !isFingerprint(fp)) {
            throw error("fp");
        }
        String sid = param(uri, "sid", "");
        if (!sid.isEmpty() && (!sid.matches("(?i)[0-9a-f]{1,16}") || (sid.length() % 2) != 0)) {
            throw error("sid");
        }
        String alpn = param(uri, "alpn", "");
        if (!alpn.isEmpty()) {
            for (String item : alpn.split(",")) {
                String value = item.trim();
                if (!value.equals("h2") && !value.equals("http/1.1")) {
                    throw error("alpn");
                }
            }
        }
        String allow = param(uri, "allowInsecure", "");
        if (!allow.isEmpty() && !allow.equals("0") && !allow.equals("1")
                && !allow.equalsIgnoreCase("true") && !allow.equalsIgnoreCase("false")) {
            throw error("allowInsecure");
        }
        return new VlessProfile(raw.trim(), userId, host, port, transport, security);
    }

    public static String errorText(IllegalArgumentException error) {
        String code = error == null || error.getMessage() == null ? "" : error.getMessage();
        switch (code) {
            case "tooLong":
                return LocaleController.getString(R.string.BananaVlessErrorTooLong);
            case "uuid":
                return LocaleController.getString(R.string.BananaVlessErrorUuid);
            case "host":
                return LocaleController.getString(R.string.BananaVlessErrorHost);
            case "port":
                return LocaleController.getString(R.string.BananaVlessErrorPort);
            case "duplicate":
                return LocaleController.getString(R.string.BananaVlessErrorDuplicate);
            case "transport":
                return LocaleController.getString(R.string.BananaVlessErrorTransport);
            case "security":
                return LocaleController.getString(R.string.BananaVlessErrorSecurity);
            case "pbk":
                return LocaleController.getString(R.string.BananaVlessErrorPbk);
            case "flow":
                return LocaleController.getString(R.string.BananaVlessErrorFlow);
            case "fp":
                return LocaleController.getString(R.string.BananaVlessErrorFp);
            case "sid":
                return LocaleController.getString(R.string.BananaVlessErrorSid);
            case "alpn":
                return LocaleController.getString(R.string.BananaVlessErrorAlpn);
            case "allowInsecure":
                return LocaleController.getString(R.string.BananaVlessErrorAllowInsecure);
            default:
                return LocaleController.getString(R.string.BananaVlessErrorInvalid);
        }
    }

    private static IllegalArgumentException error(String code) {
        return new IllegalArgumentException(code);
    }

    private static String param(Uri uri, String name, String fallback) {
        String value = uri.getQueryParameter(name);
        return value == null ? fallback : value;
    }

    private static boolean isUuid(String value) {
        if (value == null) {
            return false;
        }
        return value.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    private static boolean isFingerprint(String fp) {
        switch (fp.toLowerCase(Locale.US)) {
            case "chrome":
            case "firefox":
            case "safari":
            case "ios":
            case "android":
            case "edge":
            case "360":
            case "qq":
            case "random":
            case "randomized":
                return true;
            default:
                return false;
        }
    }
}
