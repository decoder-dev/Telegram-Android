/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.sync;

import com.radolyn.ayugram.AyuConfig;

public class AyuSyncConfig {
    private static String getWebSocketProtocol() {
        return AyuConfig.useSecureConnection ? "wss://" : "ws://";
    }

    private static String getHTTPProtocol() {
        return AyuConfig.useSecureConnection ? "https://" : "http://";
    }

    public static String getWebSocketURL() {
        return getWebSocketProtocol() + AyuConfig.getSyncServerURL() + "/v1/sync/ws";
    }

    public static String getSyncBaseURL() {
        return getHTTPProtocol() + AyuConfig.getSyncServerURL() + "/v1/sync";
    }

    public static String getAyuBaseURL() {
        return getHTTPProtocol() + AyuConfig.getSyncServerURL() + "/v1/ayu";
    }

    public static String getToken() {
        return AyuConfig.getSyncServerToken();
    }

    public static String getProfileURL() {
        return getHTTPProtocol() + AyuConfig.getSyncServerURL() + "/ui/profile?token=" + AyuConfig.getSyncServerToken();
    }
}
