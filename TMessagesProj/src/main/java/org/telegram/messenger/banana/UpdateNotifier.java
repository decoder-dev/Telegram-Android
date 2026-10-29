package org.telegram.messenger.banana;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.exteragram.messenger.ExteraConfig;
import com.exteragram.messenger.utils.UpdaterUtils;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.LaunchActivity;

/**
 * Daily background release check and a "new version" notification, posted once per version.
 * The check follows the updater's own switch; tapping the notification opens the updater sheet.
 */
public final class UpdateNotifier {
    public static final String ACTION_OPEN_UPDATER = "org.telegram.messenger.banana.OPEN_UPDATER";
    private static final String ACTION_CHECK = "org.telegram.messenger.banana.CHECK_UPDATES";
    private static final String CHANNEL = "bananagram_updates";
    private static final int NOTIFICATION_ID = 0x0B0A0B0A;
    private static final String NOTIFIED_KEY = "bananaUpdateNotifiedVersion";

    private static volatile boolean openRequested;

    private UpdateNotifier() {}

    /**
     * Arms or clears the inexact daily alarm. It never wakes the device just for this check.
     * Every process start calls this, so an armed alarm is left alone: re-arming it each time
     * would keep pushing the first run back and the check might never fire.
     */
    public static void schedule(Context context) {
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarm == null) return;
        Intent intent = new Intent(context, Receiver.class).setAction(ACTION_CHECK);
        PendingIntent armed = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE);
        if (BuildVars.PM_BUILD || !ExteraConfig.checkUpdatesOnLaunch) {
            if (armed != null) {
                alarm.cancel(armed);
                armed.cancel();
            }
            return;
        }
        if (armed != null) return;
        PendingIntent check = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        alarm.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_HOUR,
                AlarmManager.INTERVAL_DAY, check);
    }

    public static void notifyAvailable(String version) {
        Context context = ApplicationLoader.applicationContext;
        if (context == null || version == null || version.equals(ExteraConfig.preferences.getString(NOTIFIED_KEY, null))) return;
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        if (!manager.areNotificationsEnabled()) return;
        manager.createNotificationChannel(new NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(LocaleController.getString(R.string.BananaUpdatesChannel)).build());
        Intent open = new Intent(context, LaunchActivity.class).setAction(ACTION_OPEN_UPDATER)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent tap = PendingIntent.getActivity(context, 0, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle(LocaleController.formatString(R.string.BananaUpdateAvailableTitle, version))
                .setContentText(LocaleController.getString(R.string.BananaUpdateAvailableText))
                .setContentIntent(tap)
                .setAutoCancel(true)
                .build();
        try {
            manager.notify(NOTIFICATION_ID, notification);
        } catch (SecurityException noPermission) {
            // POST_NOTIFICATIONS was revoked between the check and the post; try again for the next check.
            return;
        }
        ExteraConfig.editor.putString(NOTIFIED_KEY, version).apply();
    }

    /** Remembers a tap on the notification; LaunchActivity opens the updater once it resumes. */
    public static void handleIntent(Intent intent) {
        if (intent != null && ACTION_OPEN_UPDATER.equals(intent.getAction())) {
            openRequested = true;
            NotificationManagerCompat.from(ApplicationLoader.applicationContext).cancel(NOTIFICATION_ID);
        }
    }

    public static boolean consumeOpenRequest() {
        boolean requested = openRequested;
        openRequested = false;
        return requested;
    }

    public static final class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !ACTION_CHECK.equals(intent.getAction())) return;
            PendingResult result = goAsync();
            try {
                UpdaterUtils.checkUpdatesInBackground(result::finish);
            } catch (RuntimeException e) {
                FileLog.e(e);
                result.finish();
            }
        }
    }
}
