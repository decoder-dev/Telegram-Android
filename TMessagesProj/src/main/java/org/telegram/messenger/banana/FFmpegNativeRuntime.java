package org.telegram.messenger.banana;

import android.content.Context;
import com.yausername.ffmpeg.FFmpeg;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** Install our 16 KiB WebP builds before either encoder or yt-dlp can load FFmpeg. */
public final class FFmpegNativeRuntime {
    public static final String[] WEBP_LIBRARIES = {"libsharpyuv.so", "libwebpdecoder.so", "libwebp.so", "libwebpdemux.so", "libwebpmux.so"};
    private static boolean prepared;
    private FFmpegNativeRuntime() { }
    public static synchronized void initialize(Context context) throws Exception {
        if (prepared) return;
        FFmpeg.getInstance().init(context);
        File destination = new File(context.getNoBackupFilesDir(), "youtubedl-android/packages/ffmpeg/usr/lib");
        if (!destination.isDirectory()) throw new IOException("FFmpeg library directory missing");
        for (String name : WEBP_LIBRARIES) {
            File source = new File(context.getApplicationInfo().nativeLibraryDir, name);
            if (!source.isFile() || source.length() == 0) throw new IOException("Packaged WebP dependency missing");
            File temporary = File.createTempFile("banana-webp-", ".so", destination);
            try {
                Files.copy(source.toPath(), temporary.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Files.move(temporary.toPath(), new File(destination, name).toPath(),
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally { temporary.delete(); }
        }
        prepared = true;
    }
}
