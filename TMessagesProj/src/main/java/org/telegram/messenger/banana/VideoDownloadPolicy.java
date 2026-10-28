package org.telegram.messenger.banana;

import java.net.URI;
import java.io.File;
import java.io.IOException;

public final class VideoDownloadPolicy {
    public static final long MAX_BYTES = 256L * 1024 * 1024;
    private VideoDownloadPolicy() {}
    public static boolean accepts(String url) {
        try {
            URI uri = new URI(url);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getUserInfo() == null
                    && (uri.getPort() == -1 || uri.getPort() == 443)
                    && videoPath(uri, url);
        } catch (Exception invalid) { return false; }
    }
    private static boolean videoPath(URI uri, String url) {
        String path = uri.getPath();
        if (path == null) return false;
        if (ExternalVideoUrl.isYouTubeShorts(url)) return path.matches("/shorts/[^/]+/?");
        if (!ExternalVideoUrl.isTikTok(url)) return false;
        String host = uri.getHost().toLowerCase(java.util.Locale.ROOT);
        if (host.equals("vm.tiktok.com") || host.equals("vt.tiktok.com")) return path.matches("/[^/]+/?");
        return path.matches("/@[^/]+/video/[0-9]+/?") || path.matches("/t/[^/]+/?") || path.matches("/v/[0-9]+\\.html");
    }
    public static File completedVideo(File directory) throws IOException {
        File[] files = directory.listFiles();
        File result = null;
        if (files != null) for (File file : files) {
            if (file.getName().startsWith("video.") && file.getName().endsWith(".mp4") && file.isFile()
                    && file.getCanonicalFile().getParentFile().equals(directory.getCanonicalFile())
                    && file.length() > 0 && file.length() <= MAX_BYTES) {
                if (result != null) throw new IOException("Ambiguous output");
                result = file;
            }
        }
        if (result == null) throw new IOException("No completed MP4");
        return result;
    }
}
