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

/** Foreground, cancellable yt-dlp downloads. No Telegram credentials are supplied. */
public final class BananaVideoDownloadService extends Service {
    private static final String CHANNEL = "banana_video_downloads";
    private static final int ID = 9814;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
    private volatile Download active;
    private volatile boolean destroyed;
    private int latestStartId;

    private static final class Download {
        final String id = java.util.UUID.randomUUID().toString();
        final AccountScope scope;
        final AtomicBoolean cancelled = new AtomicBoolean(), finished = new AtomicBoolean();
        final File directory;
        volatile CancellationSignal export;
        ScheduledFuture<?> timeout, budget;
        long lastProgressAt;

        Download(AccountScope scope) {
            this.scope = scope;
            directory = new File(AndroidUtilities.getCacheDir(), "banana-video-" + id);
        }

        void cancel() {
            cancelled.set(true);
            CancellationSignal saving = export;
            if (saving != null) saving.cancel();
            try { YoutubeDL.getInstance().destroyProcessById(id); }
            catch (RuntimeException error) { FileLog.e("BananaGram: download cancellation failed (" + error.getClass().getSimpleName() + ")"); }
        }
    }

    public static void start(Context context, int account, String url) {
        if (!VideoDownloadPolicy.accepts(url)) throw new IllegalArgumentException("Unsupported video URL");
        AccountScope scope = AccountScope.capture(account);
        Intent intent = new Intent(context, BananaVideoDownloadService.class).putExtra("url", url).putExtra("account", account)
                .putExtra("owner", scope.owner).putExtra("testBackend", scope.testBackend).putExtra("generation", scope.generation());
        androidx.core.content.ContextCompat.startForegroundService(context, intent);
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    private void notification(Download download, String text, int progress) {
        if (destroyed || progress >= 0 && (download == null || active != download || download.cancelled.get() || download.finished.get())) return;
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL, LocaleController.getString(R.string.BananaVideoDownload), NotificationManager.IMPORTANCE_LOW));
        Intent cancel = new Intent(this, getClass()).setAction("cancel");
        PendingIntent action = PendingIntent.getService(this, 0, cancel, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification note = new NotificationCompat.Builder(this, CHANNEL).setSmallIcon(R.drawable.msg_download)
                .setContentTitle(LocaleController.getString(R.string.BananaVideoDownload)).setContentText(text)
                .setOnlyAlertOnce(true).setOngoing(true).setProgress(100, Math.max(0, progress), progress < 0)
                .addAction(0, LocaleController.getString(R.string.Cancel), action).build();
        startForeground(ID, note);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        latestStartId = startId;
        if (intent != null && "cancel".equals(intent.getAction())) {
            Download download = active;
            if (download != null) download.cancel();
            else stopSelfResult(startId);
            return START_NOT_STICKY;
        }
        if (active != null) {
            notification(active, LocaleController.getString(R.string.BananaVideoDownloading), -1);
            toast(R.string.BananaVideoBusy);
            return START_NOT_STICKY;
        }
        // A foreground-service start must be acknowledged even when its request is rejected.
        notification(null, LocaleController.getString(R.string.BananaVideoPreparing), -1);
        String url = intent == null ? null : intent.getStringExtra("url");
        int account = intent == null ? -1 : intent.getIntExtra("account", -1);
        if (!VideoDownloadPolicy.accepts(url) || account < 0 || account >= UserConfig.MAX_ACCOUNT_COUNT) {
            stopSelfResult(startId);
            return START_NOT_STICKY;
        }
        AccountScope scope = AccountScope.capture(account);
        if (scope.owner <= 0 || scope.owner != intent.getLongExtra("owner", 0)
                || scope.testBackend != intent.getBooleanExtra("testBackend", false)
                || scope.generation() != intent.getLongExtra("generation", -1)) {
            stopSelfResult(startId);
            return START_NOT_STICKY;
        }
        Download download = new Download(scope);
        active = download;
        worker.execute(() -> download(url, download));
        return START_NOT_STICKY;
    }

    private void download(String url, Download download) {
        boolean handedOff = false;
        try {
            if (destroyed || download.cancelled.get() || !download.scope.isCurrent()) return;
            download.timeout = timer.schedule(download::cancel, 10, TimeUnit.MINUTES);
            download.budget = timer.scheduleWithFixedDelay(() -> {
                if (!download.scope.isCurrent()) download.cancel();
                else if (!download.cancelled.get() && VideoDownloadPolicy.directoryBytes(download.directory) > VideoDownloadPolicy.MAX_BYTES * 2) {
                    download.cancel();
                    toast(R.string.BananaVideoFailed);
                }
            }, 2, 2, TimeUnit.SECONDS);
            if (!download.directory.mkdirs()) throw new java.io.IOException("Cannot create download directory");
            YoutubeDL dl = YoutubeDL.getInstance();
            dl.init(this);
            FFmpegNativeRuntime.initialize(this);
            if (download.cancelled.get() || !download.scope.isCurrent()) return;
            long updated = getSharedPreferences(CHANNEL, 0).getLong("updated", 0);
            if (System.currentTimeMillis() - updated > 24 * 60 * 60 * 1000L) {
                try {
                    dl.updateYoutubeDL(this, YoutubeDL.UpdateChannel._STABLE);
                    getSharedPreferences(CHANNEL, 0).edit().putLong("updated", System.currentTimeMillis()).apply();
                } catch (Exception unavailable) { /* Keep the bundled extractor usable offline. */ }
            }
            if (download.cancelled.get() || !download.scope.isCurrent()) return;
            YoutubeDLRequest request = new YoutubeDLRequest(url);
            request.addOption("--ignore-config").addOption("--no-playlist").addOption("--no-cache-dir")
                    .addOption("--playlist-items", "1").addOption("--max-filesize", VideoDownloadPolicy.MAX_BYTES)
                    .addOption("--socket-timeout", 20).addOption("--retries", 2).addOption("--fragment-retries", 2)
                    .addOption("--merge-output-format", "mp4").addOption("--remux-video", "mp4")
                    .addOption("-f", "bv*[ext=mp4]+ba[ext=m4a]/b[ext=mp4]/b")
                    .addOption("-o", new File(download.directory, "video.%(ext)s").getAbsolutePath());
            dl.execute(request, download.id, (progress, eta, line) -> {
                if (download.cancelled.get() || !download.scope.isCurrent()) download.cancel();
                else if (SystemClock.elapsedRealtime() - download.lastProgressAt >= 500) {
                    download.lastProgressAt = SystemClock.elapsedRealtime();
                    AndroidUtilities.runOnUIThread(() -> notification(download, LocaleController.getString(R.string.BananaVideoDownloading), progress.intValue()));
                }
                return kotlin.Unit.INSTANCE;
            });
            if (download.cancelled.get() || !download.scope.isCurrent()) return;
            File file = VideoDownloadPolicy.completedVideo(download.directory);
            handedOff = true;
            download.export = MediaController.saveFile(file.getAbsolutePath(), this, 1,
                    "BananaGram_" + System.currentTimeMillis() + ".mp4", "video/mp4", uri -> {
                if (!download.cancelled.get() && download.scope.isCurrent() && !destroyed) toast(R.string.BananaVideoSaved);
                else Utilities.globalQueue.postRunnable(() -> {
                    try { getContentResolver().delete(uri, null, null); }
                    catch (RuntimeException ignored) { }
                });
                finish(download);
            }, false, () -> {
                if (!download.cancelled.get() && download.scope.isCurrent() && !destroyed) toast(R.string.BananaVideoFailed);
                finish(download);
            });
            if (download.cancelled.get() || !download.scope.isCurrent() || destroyed) download.export.cancel();
        } catch (Exception | LinkageError failed) {
            handedOff = false;
            if (!download.cancelled.get() && download.scope.isCurrent() && !destroyed) toast(R.string.BananaVideoFailed);
        } finally {
            if (!handedOff) finish(download);
        }
    }

    private void finish(Download download) {
        if (!download.finished.compareAndSet(false, true)) return;
        if (download.timeout != null) download.timeout.cancel(false);
        if (download.budget != null) download.budget.cancel(false);
        Utilities.globalQueue.postRunnable(() -> {
            if (!VideoDownloadPolicy.erase(download.directory)) FileLog.e("BananaGram: download cache cleanup failed");
        });
        AndroidUtilities.runOnUIThread(() -> {
            if (active == download) {
                active = null;
                stopSelfResult(latestStartId);
            }
        });
    }

    @Override public void onTimeout(int startId, int fgsType) {
        Download download = active;
        if (download != null) download.cancel();
        stopSelf();
    }

    private void toast(int message) {
        AndroidUtilities.runOnUIThread(() -> {
            if (!destroyed) android.widget.Toast.makeText(this, LocaleController.getString(message), android.widget.Toast.LENGTH_LONG).show();
        });
    }

    @Override public void onDestroy() {
        destroyed = true;
        Download download = active;
        if (download != null) download.cancel();
        worker.shutdownNow();
        timer.shutdownNow();
        super.onDestroy();
    }
}
