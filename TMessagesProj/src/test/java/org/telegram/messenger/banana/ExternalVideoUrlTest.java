package org.telegram.messenger.banana;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExternalVideoUrlTest {
    @Test
    public void recognizesTikTokDomainsOnly() {
        assertTrue(ExternalVideoUrl.isTikTok("https://www.tiktok.com/@name/video/123"));
        assertTrue(ExternalVideoUrl.isTikTok("https://vm.tiktok.com/abc/"));
        assertFalse(ExternalVideoUrl.isTikTok("https://not-tiktok.com/video/123"));
        assertFalse(ExternalVideoUrl.isTikTok("https://example.com/?next=tiktok.com"));
        assertFalse(ExternalVideoUrl.isTikTok("javascript://tiktok.com/video/123"));
    }

    @Test
    public void recognizesYouTubeShortsPathOnly() {
        assertTrue(ExternalVideoUrl.isYouTubeShorts("https://youtube.com/shorts/abc"));
        assertTrue(ExternalVideoUrl.isYouTubeShorts("https://www.youtube.com/shorts"));
        assertFalse(ExternalVideoUrl.isYouTubeShorts("https://youtube.com.evil.example/shorts/abc"));
        assertFalse(ExternalVideoUrl.isYouTubeShorts("https://example.com/?next=youtube.com/shorts/abc"));
        assertFalse(ExternalVideoUrl.isYouTubeShorts("https://youtube.com/watch?v=abc&path=/shorts"));
        assertFalse(ExternalVideoUrl.isYouTubeShorts("https://youtu.be/abc?label=shorts"));
    }
}
