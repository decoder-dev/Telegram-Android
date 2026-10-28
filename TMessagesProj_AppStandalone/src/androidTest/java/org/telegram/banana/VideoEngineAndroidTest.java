package org.telegram.banana;

import androidx.test.platform.app.InstrumentationRegistry;
import com.yausername.ffmpeg.FFmpeg;
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
        YoutubeDL dl = YoutubeDL.getInstance(); dl.init(context); FFmpeg.getInstance().init(context);
        YoutubeDLResponse response = dl.execute(new YoutubeDLRequest(Collections.singletonList("--version")), "banana-engine-fixture");
        assertEquals(0, response.getExitCode());
        assertTrue("Extractor must report its version", response.getOut().trim().matches("[0-9]{4}\\.[0-9]{2}\\.[0-9]{2}.*"));
    }
}
