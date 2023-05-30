package com.radolyn.ayugram;

import android.app.Activity;
import android.content.SharedPreferences;


import org.telegram.messenger.ApplicationLoader;

public class AyuConfig {
    private static final Object sync = new Object();
    public static SharedPreferences preferences;
    public static SharedPreferences.Editor editor;
    // AyuGram
    public static boolean sendReadPackets;
    public static boolean sendOnlinePackets;
    public static boolean sendOfflinePacketAfterOnline;
    public static boolean sendUploadProgress;
    public static boolean useScheduledMessages;
    public static boolean keepDeletedMessages;
    public static boolean keepMessagesHistory;
    public static boolean markReadAfterSend;
    public static boolean showFromChannel;
    public static boolean keepAliveService;
    public static boolean enableAds;
    private static boolean configLoaded;

    static {
        loadConfig();
    }

    public static void loadConfig() {
        synchronized (sync) {
            if (configLoaded) {
                return;
            }

            preferences = ApplicationLoader.applicationContext.getSharedPreferences("ayuconfig", Activity.MODE_PRIVATE);
            editor = preferences.edit();

            // AyuGram
            // ~ Ghost essentials
            sendReadPackets = preferences.getBoolean("sendReadPackets", true);
            sendOnlinePackets = preferences.getBoolean("sendOnlinePackets", true);
            sendUploadProgress = preferences.getBoolean("sendUploadProgress", true);

            sendOfflinePacketAfterOnline = preferences.getBoolean("sendOfflinePacketAfterOnline", false);
            markReadAfterSend = preferences.getBoolean("markReadAfterSend", false);

            useScheduledMessages = preferences.getBoolean("useScheduledMessages", false);

            // ~ Message edits & deletion history
            keepDeletedMessages = preferences.getBoolean("keepDeletedMessages", false);
            keepMessagesHistory = preferences.getBoolean("keepMessagesHistory", false);

            // ~ Utils
            showFromChannel = preferences.getBoolean("showFromChannel", false);
            keepAliveService = preferences.getBoolean("keepAliveService", true);
            enableAds = preferences.getBoolean("enableAds", true);

            configLoaded = true;
        }
    }

    public static String getDeletedMark() {
        return AyuConfig.preferences.getString("deletedMarkText", "🧹");
    }
}
