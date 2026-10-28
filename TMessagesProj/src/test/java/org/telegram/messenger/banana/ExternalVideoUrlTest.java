package org.telegram.messenger.banana;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExternalVideoUrlTest {
    @Test
    public void recognizesTikTokDomainsOnly() {
        assertTrue(BananaHooks.isTikTokUrl("https://www.tiktok.com/@name/video/123"));
        assertTrue(BananaHooks.isTikTokUrl("https://vm.tiktok.com/abc/"));
        assertFalse(BananaHooks.isTikTokUrl("https://not-tiktok.com/video/123"));
        assertFalse(BananaHooks.isTikTokUrl("https://example.com/?next=tiktok.com"));
        assertFalse(BananaHooks.isTikTokUrl("javascript://tiktok.com/video/123"));
    }

    @Test
    public void recognizesYouTubeShortsPathOnly() {
        assertTrue(BananaHooks.isYouTubeShortsUrl("https://youtube.com/shorts/abc"));
        assertTrue(BananaHooks.isYouTubeShortsUrl("https://www.youtube.com/shorts"));
        assertFalse(BananaHooks.isYouTubeShortsUrl("https://youtube.com.evil.example/shorts/abc"));
        assertFalse(BananaHooks.isYouTubeShortsUrl("https://example.com/?next=youtube.com/shorts/abc"));
        assertFalse(BananaHooks.isYouTubeShortsUrl("https://youtube.com/watch?v=abc&path=/shorts"));
        assertFalse(BananaHooks.isYouTubeShortsUrl("https://youtu.be/abc?label=shorts"));
    }
}
