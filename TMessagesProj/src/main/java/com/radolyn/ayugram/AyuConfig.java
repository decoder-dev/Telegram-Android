package com.radolyn.ayugram;

import android.app.Activity;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

public class AyuConfig {
    private static final Object sync = new Object();
    private static boolean configLoaded;

    public static SharedPreferences preferences;
    public static SharedPreferences.Editor editor;


    public static boolean enableAds;


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

            // ...
            enableAds = preferences.getBoolean("enableAds", true);

            configLoaded = true;
        }
    }
}
