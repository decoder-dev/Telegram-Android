package org.telegram.messenger.banana;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;

import androidx.core.content.FileProvider;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.Utilities;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * "Copy photo" from the message menu. Telegram keeps media in several places, not all of them
 * shared by our FileProvider, so the photo is first copied into cache/copied_photos (see
 * provider_paths.xml). Only the latest copy is kept.
 */
public final class PhotoClipboard {
    private static final String DIR = "copied_photos";

    private PhotoClipboard() {}

    public static void copy(File photo, Runnable onCopied, Runnable onFailed) {
        if (photo == null || !photo.exists()) {
            onFailed.run();
            return;
        }
        Utilities.globalQueue.postRunnable(() -> {
            Context context = ApplicationLoader.applicationContext;
            Uri uri;
            try {
                uri = FileProvider.getUriForFile(context, ApplicationLoader.getApplicationId() + ".provider", stage(context, photo));
            } catch (Exception e) {
                FileLog.e(e);
                AndroidUtilities.runOnUIThread(onFailed);
                return;
            }
            AndroidUtilities.runOnUIThread(() -> {
                try {
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(ClipData.newUri(context.getContentResolver(), "photo", uri));
                    onCopied.run();
                } catch (Exception e) {
                    FileLog.e(e);
                    onFailed.run();
                }
            });
        });
    }

    private static File stage(Context context, File photo) throws IOException {
        File dir = new File(context.getCacheDir(), DIR);
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("Cannot create " + dir);
        }
        File[] previous = dir.listFiles();
        if (previous != null) {
            for (File file : previous) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }
        File copy = new File(dir, "photo_" + System.currentTimeMillis() + extension(photo.getName()));
        if (!AndroidUtilities.copyFile(photo, copy)) {
            throw new IOException("Cannot copy " + photo);
        }
        return copy;
    }

    // The extension tells FileProvider the MIME type that apps reading the clipboard will see.
    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || name.length() - dot > 5) {
            return ".jpg";
        }
        return name.substring(dot).toLowerCase(Locale.ROOT);
    }
}
