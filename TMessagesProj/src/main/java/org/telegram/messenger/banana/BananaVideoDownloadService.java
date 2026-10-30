package org.telegram.messenger.banana;

import android.app.*;
import android.content.*;
import android.os.*;
import androidx.core.app.NotificationCompat;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import org.telegram.messenger.*;
import java.io.File;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Foreground, cancellable yt-dlp downloads. No cookies or Telegram session material is supplied. */
public final class BananaVideoDownloadService extends Service {
    private static final String CHANNEL = "banana_video_downloads";
    private static final int ID = 9814;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private volatile String processId;
    private volatile boolean busy;
    private volatile boolean destroyed;
    private long lastProgressAt;

    public static void start(Context context, int account, String url) {
        if (!VideoDownloadPolicy.accepts(url)) throw new IllegalArgumentException("Unsupported video URL");
        Intent intent = new Intent(context, BananaVideoDownloadService.class).putExtra("url", url).putExtra("account", account)
                .putExtra("owner", UserConfig.getInstance(account).getClientUserId());
        androidx.core.content.ContextCompat.startForegroundService(context, intent);
    }
    @Override public IBinder onBind(Intent intent) { return null; }
    private void notification(String text, int progress) {
        if (destroyed || progress >= 0 && (!busy || cancelled.get())) return;
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(new NotificationChannel(CHANNEL, LocaleController.getString(R.string.BananaVideoDownload), NotificationManager.IMPORTANCE_LOW));
        Intent cancel = new Intent(this, getClass()).setAction("cancel");
        PendingIntent action = PendingIntent.getService(this, 0, cancel, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification note = new NotificationCompat.Builder(this, CHANNEL).setSmallIcon(R.drawable.msg_download)
                .setContentTitle(LocaleController.getString(R.string.BananaVideoDownload)).setContentText(text)
                .setOnlyAlertOnce(true).setOngoing(true).setProgress(100, Math.max(0, progress), progress < 0)
                .addAction(0, LocaleController.getString(R.string.Cancel), action).build();
        startForeground(ID, note);
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            notification(LocaleController.getString(R.string.BananaVideoPreparing), -1);
            stopSelf();
            return START_NOT_STICKY;
        }
        if ("cancel".equals(intent.getAction())) {
            cancelled.set(true);
            String id = processId;
            if (id != null) YoutubeDL.getInstance().destroyProcessById(id);
            if (!busy) stopSelf();
            return START_NOT_STICKY;
        }
        if (busy) {
            // Every startForegroundService() needs its own startForeground(), even while the
            // running download already holds the notification; repost it to satisfy that.
            notification(LocaleController.getString(R.string.BananaVideoDownloading), -1);
            toast(R.string.BananaVideoBusy);
            return START_NOT_STICKY;
        }
        // startForegroundService() requires startForeground() even for a rejected request, or Android 9+ crashes the app.
        notification(LocaleController.getString(R.string.BananaVideoPreparing), -1);
        String url = intent.getStringExtra("url");
        int account = intent.getIntExtra("account", -1);
        long owner = intent.getLongExtra("owner", 0);
        if (!VideoDownloadPolicy.accepts(url) || account < 0 || account >= UserConfig.MAX_ACCOUNT_COUNT || owner <= 0
                || UserConfig.getInstance(account).getClientUserId() != owner) { stopSelf(); return START_NOT_STICKY; }
        busy = true; cancelled.set(false); lastProgressAt = 0; processId = java.util.UUID.randomUUID().toString();
        worker.execute(() -> download(url, account, owner));
        return START_NOT_STICKY;
    }
    private void download(String url, int account, long owner) {
        File directory = new File(getCacheDir(), "banana-video-" + processId);
        ScheduledFuture<?> timeout = timer.schedule(() -> {
            cancelled.set(true); String id = processId;
            if (id != null) YoutubeDL.getInstance().destroyProcessById(id);
        }, 10, TimeUnit.MINUTES);
        ScheduledFuture<?> budget = timer.scheduleWithFixedDelay(() -> {
            if (directoryBytes(directory) > VideoDownloadPolicy.MAX_BYTES * 2) {
                cancelled.set(true); String id = processId;
                if (id != null) YoutubeDL.getInstance().destroyProcessById(id);
                toast(R.string.BananaVideoFailed);
            }
        }, 2, 2, TimeUnit.SECONDS);
        boolean handedOff = false;
        try {
            if (!directory.mkdirs()) throw new java.io.IOException("Cannot create download directory");
            YoutubeDL dl = YoutubeDL.getInstance(); dl.init(this); FFmpegNativeRuntime.initialize(this);
            if (cancelled.get()) return;
            // Keep site extractors current. An update failure may still leave a working bundled extractor.
            long updated = getSharedPreferences(CHANNEL, 0).getLong("updated", 0);
            if (System.currentTimeMillis() - updated > 24 * 60 * 60 * 1000L) {
                try { dl.updateYoutubeDL(this, YoutubeDL.UpdateChannel._STABLE); getSharedPreferences(CHANNEL, 0).edit().putLong("updated", System.currentTimeMillis()).apply(); }
                catch (Exception unavailable) { /* execute with the packaged extractor */ }
            }
            if (cancelled.get()) return;
            YoutubeDLRequest request = new YoutubeDLRequest(url);
            request.addOption("--ignore-config").addOption("--no-playlist").addOption("--no-cache-dir")
                    .addOption("--playlist-items", "1").addOption("--max-filesize", VideoDownloadPolicy.MAX_BYTES)
                    .addOption("--socket-timeout", 20).addOption("--retries", 2).addOption("--fragment-retries", 2)
                    .addOption("--merge-output-format", "mp4").addOption("--remux-video", "mp4")
                    .addOption("-f", "bv*[ext=mp4]+ba[ext=m4a]/b[ext=mp4]/b")
                    .addOption("-o", new File(directory, "video.%(ext)s").getAbsolutePath());
            dl.execute(request, processId, (progress, eta, line) -> {
                if (cancelled.get() || UserConfig.getInstance(account).getClientUserId() != owner) dl.destroyProcessById(processId);
                else if (SystemClock.elapsedRealtime() - lastProgressAt >= 500) {
                    lastProgressAt = SystemClock.elapsedRealtime();
                    AndroidUtilities.runOnUIThread(() -> notification(LocaleController.getString(R.string.BananaVideoDownloading), progress.intValue()));
                }
                return kotlin.Unit.INSTANCE;
            });
            if (cancelled.get() || UserConfig.getInstance(account).getClientUserId() != owner) return;
            File file = VideoDownloadPolicy.completedVideo(directory);
            MediaController.saveFile(file.getAbsolutePath(), this, 1, "BananaGram_" + System.currentTimeMillis() + ".mp4", "video/mp4", uri -> {
                if (!cancelled.get()) toast(uri != null ? R.string.BananaVideoSaved : R.string.BananaVideoFailed);
                busy = false; processId = null;
                erase(directory); stopSelf();
            }, false);
            handedOff = true;
        } catch (Exception | LinkageError failed) {
            // yt-dlp output may contain the private URL. Do not write it to exported logs.
            if (!cancelled.get()) toast(R.string.BananaVideoFailed);
        } finally {
            timeout.cancel(false); budget.cancel(false);
            if (!handedOff) { processId = null; busy = false; erase(directory); stopSelf(); }
        }
    }
    private static long directoryBytes(File directory) {
        long bytes = 0; File[] files = directory.listFiles();
        if (files != null) for (File file : files) bytes += file.isDirectory() ? directoryBytes(file) : file.length();
        return bytes;
    }
    @Override public void onTimeout(int startId, int fgsType) { cancelled.set(true); stopSelf(); }
    private void toast(int message) { AndroidUtilities.runOnUIThread(() -> android.widget.Toast.makeText(this, LocaleController.getString(message), android.widget.Toast.LENGTH_LONG).show()); }
    private static void erase(File directory) { File[] files = directory.listFiles(); if (files != null) for (File file : files) { if (file.isDirectory()) erase(file); else file.delete(); } directory.delete(); }
    @Override public void onDestroy() {
        destroyed = true; cancelled.set(true);
        // The download thread clears processId in its finally; read it once so null never reaches yt-dlp.
        String id = processId;
        if (id != null) {
            try {
                YoutubeDL.getInstance().destroyProcessById(id);
            } catch (RuntimeException e) {
                FileLog.e(e);
            }
        }
        worker.shutdownNow(); timer.shutdownNow(); super.onDestroy();
    }
}
