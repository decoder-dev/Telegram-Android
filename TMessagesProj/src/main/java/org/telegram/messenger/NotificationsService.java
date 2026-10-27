/*
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

import java.util.Random;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.util.Log;
import com.radolyn.ayugram.AyuConfig;
import org.telegram.ui.LaunchActivity;



public class NotificationsService extends Service {
    private static final String[] notifications = new String[]{
            "⊂(◉‿◉)つ",
            "(｡◕‿‿◕｡)",
            "(｡◕‿◕｡)",
            "¯\\_(ツ)_/¯",
            "\\(^-^)/",
            "＼(＾O＾)／",
            "\\(ᵔᵕᵔ)/",
            "ԅ(≖‿≖ԅ)",
            "(⊃｡•́‿•̀｡)⊃",
            "•ᴗ•",
            "(~‾▿‾)~",
            "｡^‿^｡",
            "(⁎˃ᆺ˂)",
            "(≧◡≦)",
            "\\(★ω★)/",
            "(✿◠‿◠)",
            "＼(٥⁀▽⁀ )／",
            "(*^.^*)",
            "( ` ω ´ )",
            "｡ﾟ･ (>﹏<) ･ﾟ｡",
            "╮(︶▽︶)╭",
            "(￣～￣;)",
            "(＾• ω •＾)"
    };

    @Override
    public void onCreate() {
        super.onCreate();
        ApplicationLoader.postInitApplication();

        if (AyuConfig.keepAliveService && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String CHANNEL_ID = "bananagram_push";
            NotificationChannelCompat channel = new NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                    .setName("BananaGram")
                    .setLightsEnabled(false)
                    .setVibrationEnabled(false)
                    .setSound(null, null)
                    .build();

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.createNotificationChannel(channel);

            var cuteText = notifications[new Random().nextInt(notifications.length)];

            Notification notification;

            // todo: remove try catch
            try {
                var startIntent = new Intent(this, LaunchActivity.class);
                var pendingIntent = PendingIntent.getActivity(this, 0, startIntent, PendingIntent.FLAG_MUTABLE);

                notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentIntent(pendingIntent)
                        .setSmallIcon(R.drawable.msg_premium_badge)
                        .setShowWhen(false)
                        .setOngoing(true)
                        .setContentText(cuteText)
                        .setCategory(NotificationCompat.CATEGORY_STATUS)
                        .build();
            } catch (Exception e) {
                Log.e("AyuGram", "Xiaomi moment", e);

                notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.msg_premium_badge)
                        .setShowWhen(false)
                        .setOngoing(true)
                        .setContentText(cuteText)
                        .setCategory(NotificationCompat.CATEGORY_STATUS)
                        .build();
            }

            startForeground(9999, notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Banana keep-alive re-arms at most one 15-minute inexact wakeup, and only in the background.
        org.telegram.ui.Components.ForegroundDetector detector = org.telegram.ui.Components.ForegroundDetector.getInstance();
        boolean background = detector == null || detector.isBackground();
        ApplicationLoader.syncBananaKeepAlive(background);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onDestroy() {
        super.onDestroy();
        SharedPreferences preferences = MessagesController.getGlobalNotificationsSettings();
        if (preferences.getBoolean("pushService", true)) {
            Intent intent = new Intent("org.telegram.start");
            intent.setPackage(getPackageName());
            sendBroadcast(intent);
        }
    }
}
