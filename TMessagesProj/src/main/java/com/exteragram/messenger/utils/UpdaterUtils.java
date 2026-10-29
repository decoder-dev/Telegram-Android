/*

 This is the source code of exteraGram for Android.

 We do not and cannot prevent the use of our code,
 but be respectful and credit the original author.

 Copyright @immat0x1, 2023

*/

package com.exteragram.messenger.utils;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

import androidx.core.content.FileProvider;

import com.exteragram.messenger.ExteraConfig;
import com.exteragram.messenger.preferences.updater.UpdaterBottomSheet;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.TypefaceSpan;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdaterUtils {

    public static final DispatchQueue otaQueue = new DispatchQueue("otaQueue");

    private static final String RELEASES_URI = "https://api.github.com/repos/decoder-dev/Telegram-Android/releases?per_page=20";
    private static String downloadURL = null;
    private static String releaseDigest = null;
    public static String version, changelog, size, uploadDate;
    public static File otaPath, versionPath, apkFile;

    private static long downloadId = -1L;
    private static final long updateCheckInterval = 24 * 3600000L;
    private static final Pattern RELEASE_VERSION = Pattern.compile("^v?(\\d+)\\.(\\d+)\\.(\\d+)(?:-rc\\.(\\d+))?$", Pattern.CASE_INSENSITIVE);

    private static boolean updateDownloaded;
    private static boolean receiverRegistered;
    private static boolean checkingForUpdates;
    private static boolean automaticDownload;

    public static void checkDirs() {
        otaPath = new File(ApplicationLoader.applicationContext.getExternalFilesDir(null), "ota");
        if (version != null) {
            versionPath = new File(otaPath, version);
            apkFile = new File(versionPath, "update.apk");
            try {
                if (!versionPath.exists())
                    versionPath.mkdirs();
            } catch (Exception e) {
                FileLog.e(e);
            }
            updateDownloaded = apkFile.exists();
        }
    }

    public static void checkUpdates(BaseFragment fragment, boolean manual) {
        checkUpdates(fragment, manual, null, null);
    }

    public interface OnUpdateNotFound {
        void run();
    }

    public interface OnUpdateFound {
        void run();
    }

    public interface OnUpdateError {
        void run();
    }

    public static void checkUpdates(BaseFragment fragment, boolean manual, OnUpdateNotFound onUpdateNotFound, OnUpdateFound onUpdateFound) {
        checkUpdates(fragment, manual, onUpdateNotFound, onUpdateFound, null);
    }

    public static void checkUpdates(BaseFragment fragment, boolean manual, OnUpdateNotFound onUpdateNotFound,
            OnUpdateFound onUpdateFound, OnUpdateError onUpdateError) {
        long now = System.currentTimeMillis();
        if (BuildVars.PM_BUILD || checkingForUpdates
                || (!manual && now - ExteraConfig.lastUpdateCheckTime < updateCheckInterval)
                || (!manual && now - ExteraConfig.updateScheduleTimestamp < 60 * 60 * 1000L))
            return;

        checkingForUpdates = true;
        otaQueue.postRunnable(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URI(RELEASES_URI).toURL().openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(20000);
                connection.setRequestProperty("User-Agent", TranslatorUtils.formatUserAgent());
                connection.setRequestProperty("Accept", "application/vnd.github+json");
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new IllegalStateException("GitHub release check returned HTTP " + connection.getResponseCode());
                }

                var textBuilder = new StringBuilder();
                try (Reader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    int c;
                    while ((c = reader.read()) != -1) {
                        if (textBuilder.length() >= 2_000_000) {
                            throw new IllegalStateException("GitHub release response is too large");
                        }
                        textBuilder.append((char) c);
                    }
                }

                Update update = findUpdate(new JSONArray(textBuilder.toString()));
                ExteraConfig.editor.putLong("lastUpdateCheckTime", ExteraConfig.lastUpdateCheckTime = System.currentTimeMillis()).apply();
                if (update != null) {
                    version = update.version;
                    changelog = update.changelog;
                    size = update.size;
                    downloadURL = update.downloadURL;
                    releaseDigest = update.digest;
                    uploadDate = update.uploadDate;
                    checkDirs();
                    if (!manual && ExteraConfig.autoDownloadUpdates) {
                        downloadApk(ApplicationLoader.applicationContext, update.downloadURL,
                                "BananaGram " + update.version, update.digest, true);
                    } else if (fragment != null) {
                        AndroidUtilities.runOnUIThread(() -> {
                            (new UpdaterBottomSheet(fragment.getContext(), fragment, true, update)).show();
                            if (onUpdateFound != null) onUpdateFound.run();
                        });
                    }
                } else {
                    if (manual && onUpdateNotFound != null)
                        AndroidUtilities.runOnUIThread(onUpdateNotFound::run);
                }
            } catch (Exception e) {
                FileLog.e(e);
                if (manual) {
                    Runnable callback = onUpdateError != null ? onUpdateError::run
                            : onUpdateNotFound != null ? onUpdateNotFound::run : null;
                    if (callback != null) AndroidUtilities.runOnUIThread(callback);
                }
            } finally {
                if (connection != null) connection.disconnect();
                checkingForUpdates = false;
            }
        }, 200);
    }

    private static Update findUpdate(JSONArray releases) throws Exception {
        Update newest = null;
        for (int i = 0; i < releases.length(); i++) {
            JSONObject release = releases.getJSONObject(i);
            if (release.optBoolean("draft", true)
                    || (release.optBoolean("prerelease", false) && !ExteraConfig.updatePrereleaseChannel)) {
                continue;
            }
            String tag = release.optString("tag_name", "");
            Update candidate = findApkAsset(release, tag);
            if (candidate != null && candidate.isNew()
                    && (newest == null || compareVersions(candidate.version, newest.version) > 0)) {
                newest = candidate;
            }
        }
        return newest;
    }

    private static Update findApkAsset(JSONObject release, String tag) throws Exception {
        JSONArray assets = release.optJSONArray("assets");
        if (assets == null || assets.length() == 0) return null;
        String abi = getInstalledApkType();
        if (abi == null) return null;
        JSONObject selected = null;
        JSONObject universal = null;
        JSONObject onlyApk = null;
        int apkCount = 0;
        for (int i = 0; i < assets.length(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            String name = asset.optString("name", "");
            if (!name.toLowerCase(Locale.US).endsWith(".apk")) continue;
            apkCount++;
            onlyApk = asset;
            String lowered = name.toLowerCase(Locale.US);
            if (lowered.contains(abi.toLowerCase(Locale.US))) selected = asset;
            if (lowered.contains("universal") || lowered.equals("app.apk")) universal = asset;
        }
        if (selected == null) selected = universal != null ? universal : (apkCount == 1 ? onlyApk : null);
        if (selected == null) return null;
        long bytes = selected.optLong("size", -1L);
        String digest = selected.optString("digest", "");
        String link = selected.optString("browser_download_url", "");
        URL url = new URL(link);
        if (!"https".equalsIgnoreCase(url.getProtocol())
                || !("github.com".equalsIgnoreCase(url.getHost())
                || "release-assets.githubusercontent.com".equalsIgnoreCase(url.getHost()))
                || bytes <= 0 || bytes > 512L * 1024 * 1024
                || !digest.matches("sha256:[a-fA-F0-9]{64}")) {
            return null;
        }
        String formattedDate = LocaleController.formatDateTime(publishedSeconds(release.optString("published_at", "")), true);
        return new Update(tag, release.optString("body", ""), AndroidUtilities.formatFileSize(bytes),
                link, formattedDate, digest.substring("sha256:".length()).toLowerCase(Locale.US));
    }

    /** GitHub timestamps are UTC ISO-8601; parsing them as local 12-hour time shifted the shown date. */
    static long publishedSeconds(String iso) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        format.setLenient(false);
        try {
            Date date = format.parse(iso);
            return date == null ? 0 : date.getTime() / 1000;
        } catch (Exception e) {
            return 0;
        }
    }

    static int compareVersions(String left, String right) {
        Matcher a = RELEASE_VERSION.matcher(left);
        Matcher b = RELEASE_VERSION.matcher(right);
        if (!a.matches() || !b.matches()) return 0;
        for (int i = 1; i <= 3; i++) {
            int result = Integer.compare(Integer.parseInt(a.group(i)), Integer.parseInt(b.group(i)));
            if (result != 0) return result;
        }
        boolean aRc = a.group(4) != null;
        boolean bRc = b.group(4) != null;
        if (aRc != bRc) return aRc ? -1 : 1;
        if (!aRc) return 0;
        return Integer.compare(Integer.parseInt(a.group(4)), Integer.parseInt(b.group(4)));
    }

    public static void downloadApk(Context context, String link, String title) {
        downloadApk(context, link, title, releaseDigest, false);
    }

    public static void installApk(Context context, String path) {
        installApk(context, path, releaseDigest);
    }

    private static synchronized void downloadApk(Context context, String link, String title, String digest, boolean automatic) {
        if (context == null || version == null || digest == null || downloadInProgress(context)) return;
        checkDirs();
        if (updateDownloaded) {
            if (automatic) return;
            installApk(context, apkFile.getAbsolutePath(), digest);
            // installApk clears the flag and deletes the file when it fails verification; download it again.
            if (updateDownloaded) return;
        }
        if (!isSafeReleaseUrl(link)) return;
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(link));
            if (automatic) {
                request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
                request.setAllowedOverMetered(false);
                request.setAllowedOverRoaming(false);
            } else {
                request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
            }
            request.setTitle(title);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalFilesDir(context, "ota/" + version, "update.apk");
            DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager == null) return;
            automaticDownload = automatic;
            releaseDigest = digest;
            downloadId = manager.enqueue(request);
            registerReceiverOnce(context.getApplicationContext());
        } catch (Exception e) {
            downloadId = -1L;
            FileLog.e(e);
        }
    }

    /**
     * DownloadManager broadcasts come from the DownloadProvider app, so a not-exported receiver
     * never hears them. Exporting is safe: the id is checked against DownloadManager and the APK's
     * digest, package, version and signers are verified before any install prompt.
     */
    private static synchronized void registerReceiverOnce(Context applicationContext) {
        if (receiverRegistered) return;
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        filter.addAction(DownloadManager.ACTION_NOTIFICATION_CLICKED);
        if (Build.VERSION.SDK_INT >= 33) {
            applicationContext.registerReceiver(new DownloadReceiver(), filter, Context.RECEIVER_EXPORTED);
        } else {
            applicationContext.registerReceiver(new DownloadReceiver(), filter);
        }
        receiverRegistered = true;
    }

    /** A missed completion broadcast must not leave the updater stuck until the app restarts. */
    private static boolean downloadInProgress(Context context) {
        if (downloadId == -1L) return false;
        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager != null) {
            try (Cursor cursor = manager.query(new DownloadManager.Query().setFilterById(downloadId))) {
                if (cursor != null && cursor.moveToFirst()) {
                    int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                    if (status == DownloadManager.STATUS_PENDING || status == DownloadManager.STATUS_RUNNING
                            || status == DownloadManager.STATUS_PAUSED) {
                        return true;
                    }
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        downloadId = -1L;
        return false;
    }

    private static void installApk(Context context, String path, String digest) {
        var file = new File(path);
        if (context == null || !file.isFile() || digest == null || !verifyApk(file, digest, context)) {
            if (file.exists()) file.delete();
            updateDownloaded = false;
            return;
        }
        var install = new Intent(Intent.ACTION_VIEW);
        Uri fileUri;
        if (Build.VERSION.SDK_INT >= 24) {
            fileUri = FileProvider.getUriForFile(context, ApplicationLoader.getApplicationId() + ".provider", file);
        } else {
            fileUri = Uri.fromFile(file);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !ApplicationLoader.applicationContext.getPackageManager().canRequestPackageInstalls()) {
            AlertsCreator.createApkRestrictedDialog(context, null).show();
            return;
        }
        if (fileUri != null) {
            install.setDataAndType(fileUri, "application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            if (install.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(install);
            }
        }
    }

    private static boolean isSafeReleaseUrl(String value) {
        try {
            URL url = new URL(value);
            return "https".equalsIgnoreCase(url.getProtocol())
                    && ("github.com".equalsIgnoreCase(url.getHost())
                    || "release-assets.githubusercontent.com".equalsIgnoreCase(url.getHost()));
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean verifyApk(File file, String expectedDigest, Context context) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var input = new java.io.FileInputStream(file)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = input.read(buffer)) != -1) digest.update(buffer, 0, read);
            }
            char[] hex = "0123456789abcdef".toCharArray();
            StringBuilder actual = new StringBuilder(64);
            for (byte value : digest.digest()) {
                int unsigned = value & 0xff;
                actual.append(hex[unsigned >>> 4]).append(hex[unsigned & 0x0f]);
            }
            if (!actual.toString().equalsIgnoreCase(expectedDigest)) return false;
            int signingFlags = Build.VERSION.SDK_INT >= 28 ? android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES : android.content.pm.PackageManager.GET_SIGNATURES;
            var info = context.getPackageManager().getPackageArchiveInfo(file.getAbsolutePath(), signingFlags);
            if (info == null || version == null || info.versionName == null || !context.getPackageName().equals(info.packageName)
                    || compareVersions(info.versionName, version) != 0) return false;
            var installed = context.getPackageManager().getPackageInfo(context.getPackageName(), signingFlags);
            android.content.pm.Signature[] candidateSigners = Build.VERSION.SDK_INT >= 28 && info.signingInfo != null ? info.signingInfo.getApkContentsSigners() : info.signatures;
            android.content.pm.Signature[] installedSigners = Build.VERSION.SDK_INT >= 28 && installed.signingInfo != null ? installed.signingInfo.getApkContentsSigners() : installed.signatures;
            if (candidateSigners == null || installedSigners == null) return false;
            byte[][] candidateBytes = new byte[candidateSigners.length][], installedBytes = new byte[installedSigners.length][];
            for (int n = 0; n < candidateSigners.length; n++) candidateBytes[n] = candidateSigners[n].toByteArray();
            for (int n = 0; n < installedSigners.length; n++) installedBytes[n] = installedSigners[n].toByteArray();
            if (!org.telegram.messenger.banana.ApkSigningPolicy.sameSigners(candidateBytes, installedBytes)) return false;
            long installedVersionCode = Build.VERSION.SDK_INT >= 28
                    ? installed.getLongVersionCode() : installed.versionCode;
            long downloadedVersionCode = Build.VERSION.SDK_INT >= 28
                    ? info.getLongVersionCode() : info.versionCode;
            return downloadedVersionCode > installedVersionCode;
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }

    public static String getOtaDirSize() {
        checkDirs();
        return AndroidUtilities.formatFileSize(Utilities.getDirSize(otaPath.getAbsolutePath(), 5, true), true, false);
    }

    public static String getInstalledApkType() {
        try {
            var info = ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
            switch (info.versionCode % 10) {
                case 1:
                case 3:
                    return "armeabi-v7a";
                case 2:
                case 4:
                    return "x86";
                case 5:
                case 7:
                    return "arm64-v8a";
                case 6:
                case 8:
                    return "x86_64";
                case 0:
                case 9:
                    return "universal";
            }
        } catch (Exception e) {
            return Build.SUPPORTED_ABIS[0];
        }
        return null;
    }

    public static void cleanOtaDir() {
        checkDirs();
        cleanFolder(otaPath);
    }

    public static void cleanFolder(File folder) {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    cleanFolder(file);
                }
            }
        }
        folder.delete();
    }

    public static long getMillisFromDate(String d, String format) {
        @SuppressLint("SimpleDateFormat")
        var sdf = new SimpleDateFormat(format);
        try {
            Date date = sdf.parse(d);
            assert date != null;
            return date.getTime();
        } catch (Exception ignore) {
            return 1L;
        }
    }

    public static SpannableStringBuilder replaceTags(CharSequence str) {
        try {
            int start;
            int end;
            StringBuilder stringBuilder = new StringBuilder(str);
            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(str);
            String symbol = "", font = AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM;
            for (int i = 0; i < 3; i++) {
                switch (i) {
                    case 0:
                        symbol = "**";
                        font = AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM;
                        break;
                    case 1:
                        symbol = "_";
                        font = AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM_ITALIC;
                        break;
                    case 2:
                        symbol = "`";
                        font = AndroidUtilities.TYPEFACE_ROBOTO_MONO;
                        break;
                }
                while ((start = stringBuilder.indexOf(symbol)) != -1) {
                    stringBuilder.replace(start, start + symbol.length(), "");
                    spannableStringBuilder.replace(start, start + symbol.length(), "");
                    end = stringBuilder.indexOf(symbol);
                    if (end >= 0) {
                        stringBuilder.replace(end, end + symbol.length(), "");
                        spannableStringBuilder.replace(end, end + symbol.length(), "");
                        spannableStringBuilder.setSpan(new TypefaceSpan(AndroidUtilities.getTypeface(font)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }
            return spannableStringBuilder;
        } catch (Exception e) {
            FileLog.e(e);
        }
        return new SpannableStringBuilder(str);
    }

    public static class DownloadReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent){
            String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                long finishedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
                if (finishedId != downloadId) return;
                DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(finishedId);
                Cursor cursor = downloadManager.query(query);
                try {
                    if (cursor != null && cursor.moveToFirst()) {
                        int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                        int status = cursor.getInt(columnIndex);
                        downloadId = -1L;
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            updateDownloaded = apkFile != null && apkFile.isFile()
                                    && verifyApk(apkFile, releaseDigest, context);
                            if (!updateDownloaded) {
                                if (apkFile != null) apkFile.delete();
                            } else if (!automaticDownload) {
                                installApk(context, apkFile.getAbsolutePath(), releaseDigest);
                            }
                        } else {
                            updateDownloaded = false;
                        }
                    }
                } finally {
                    if (cursor != null) cursor.close();
                }
            } else if (DownloadManager.ACTION_NOTIFICATION_CLICKED.equals(action)) {
                try {
                    Intent viewDownloadIntent = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
                    viewDownloadIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(viewDownloadIntent);
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
        }
    }

    public static class Update {
        public final String version, size, downloadURL, uploadDate, changelog, digest;

        public Update(String version, String changelog, String size, String downloadURL, String uploadDate) {
            this(version, changelog, size, downloadURL, uploadDate, null);
        }

        public Update(String version, String changelog, String size, String downloadURL, String uploadDate, String digest) {
            this.version = version;
            this.changelog = changelog;
            this.size = size;
            this.downloadURL = downloadURL;
            this.uploadDate = uploadDate;
            this.digest = digest;
        }

        public boolean isNew() {
            return compareVersions(version, BuildVars.BUILD_VERSION_STRING) > 0;
        }

        // todo: force update
        public boolean isForce() {
            return version.toLowerCase().contains("force");
        }
    }
}
