package org.telegram.banana;

import androidx.test.platform.app.InstrumentationRegistry;
import org.telegram.messenger.banana.FFmpegNativeRuntime;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.YoutubeDLResponse;
import org.junit.Test;
import java.util.Collections;
import static org.junit.Assert.*;

/** Exercises the real packaged Python/yt-dlp executables, without fetching a video. */
public class VideoEngineAndroidTest {
    @Test public void packagedExtractorRunsOnAndroid() throws Exception {
        android.content.Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        YoutubeDL dl = YoutubeDL.getInstance(); dl.init(context); FFmpegNativeRuntime.initialize(context);
        for (String name : FFmpegNativeRuntime.WEBP_LIBRARIES) {
            java.io.File original = new java.io.File(context.getApplicationInfo().nativeLibraryDir, name);
            java.io.File installed = new java.io.File(context.getNoBackupFilesDir(), "youtubedl-android/packages/ffmpeg/usr/lib/" + name);
            assertArrayEquals("FFmpeg must use the packaged aligned replacement", java.nio.file.Files.readAllBytes(original.toPath()), java.nio.file.Files.readAllBytes(installed.toPath()));
        }
        YoutubeDLResponse response = dl.execute(new YoutubeDLRequest(Collections.singletonList("--version")), "banana-engine-fixture");
        assertEquals(0, response.getExitCode());
        assertTrue("Extractor must report its version", response.getOut().trim().matches("[0-9]{4}\\.[0-9]{2}\\.[0-9]{2}.*"));
    }
}
