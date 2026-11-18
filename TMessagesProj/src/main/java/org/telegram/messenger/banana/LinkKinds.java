package org.telegram.messenger.banana;

import java.net.URI;
import java.util.Locale;

/** Which links lead off Telegram (ported from BananaGram iOS "Confirm Links"). */
public final class LinkKinds {
    private static final String[] TELEGRAM_HOSTS = {"t.me", "telegram.me", "telegram.dog"};

    private LinkKinds() {}

    public static boolean isTelegramLink(String url) {
        if (url == null) return false;
        String trimmed = url.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("tg:")) return true;
        String candidate = lower.contains("://") ? trimmed : "https://" + trimmed;
        String host;
        try {
            URI uri = new URI(candidate);
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) return false;
            host = uri.getHost();
        } catch (Exception invalid) {
            return false;
        }
        if (host == null) return false;
        host = host.toLowerCase(Locale.ROOT);
        while (host.endsWith(".")) host = host.substring(0, host.length() - 1);
        for (String telegram : TELEGRAM_HOSTS) {
            if (host.equals(telegram) || host.endsWith("." + telegram)) return true;
        }
        return false;
    }

    /** A website: not Telegram, and http(s) or a bare "domain.tld/...". */
    public static boolean isWebsiteLink(String url) {
        if (url == null || isTelegramLink(url)) return false;
        String lower = url.trim().toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://")) return true;
        return !lower.contains(":") && lower.contains(".");
    }
}
