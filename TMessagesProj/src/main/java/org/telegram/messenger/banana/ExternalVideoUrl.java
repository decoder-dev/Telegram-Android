package org.telegram.messenger.banana;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/** URL matching rules for the optional external TikTok and YouTube Shorts handlers. */
public final class ExternalVideoUrl {
    private ExternalVideoUrl() {}

    public static boolean isTikTok(String url) {
        URI uri = parseHttpUri(url);
        return uri != null && isHostOrSubdomain(uri.getHost().toLowerCase(Locale.ROOT), "tiktok.com");
    }

    public static boolean isYouTubeShorts(String url) {
        URI uri = parseHttpUri(url);
        if (uri == null || uri.getPath() == null) return false;
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        String path = uri.getPath();
        return isHostOrSubdomain(host, "youtube.com")
                && (path.equals("/shorts") || path.startsWith("/shorts/"));
    }

    private static URI parseHttpUri(String url) {
        if (url == null || url.trim().isEmpty()) return null;
        try {
            URI uri = new URI(url.trim());
            String scheme = uri.getScheme();
            return uri.getHost() != null && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) ? uri : null;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private static boolean isHostOrSubdomain(String host, String domain) {
        return host.equals(domain) || host.endsWith("." + domain);
    }
}
