package org.telegram.messenger.banana;

import org.junit.Test;
import java.util.List;
import java.util.Locale;
import static org.junit.Assert.*;
import static org.telegram.messenger.banana.MediaConversionPolicy.Kind.*;

public class MediaConversionPolicyTest {
    @Test public void roundVideosRespectTrimAndNeverExceedOneMinute() {
        assertEquals(60, MediaConversionPolicy.outputDuration(VIDEO_TO_ROUND, 200, 0, 0), 0);
        assertEquals(20, MediaConversionPolicy.outputDuration(VIDEO_TO_ROUND, 200, 50, 70), 0);
        assertEquals(10, MediaConversionPolicy.outputDuration(VIDEO_TO_ROUND, 200, 190, 0), 0);
    }
    @Test public void rejectsInvalidGeometryAndOversizedVoiceWithoutSilentlyTruncatingMusic() {
        for (double duration : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            try { MediaConversionPolicy.outputDuration(VIDEO_TO_ROUND, duration, 0, 0); fail(); } catch (IllegalArgumentException expected) { }
        }
        try { MediaConversionPolicy.outputDuration(VIDEO_TO_ROUND, 10, 9, 8); fail(); } catch (IllegalArgumentException expected) { }
        try { MediaConversionPolicy.outputDuration(AUDIO_TO_VOICE, 3601, 0, 0); fail(); } catch (IllegalArgumentException expected) { }
        assertEquals(3601, MediaConversionPolicy.outputDuration(VOICE_TO_AUDIO, 3601, 0, 0), 0);
    }
    @Test public void voiceUsesAnActualMonoOpusEncoderAndPathsStaySingleArguments() {
        String input = "/private/A track $(title); --help.mp3";
        List<String> args = MediaConversionPolicy.arguments(AUDIO_TO_VOICE, input, "/output/voice.ogg", 10, 0, 0);
        assertEquals(input, args.get(args.indexOf("-i") + 1));
        assertEquals("libopus", args.get(args.indexOf("-c:a") + 1));
        assertEquals("1", args.get(args.indexOf("-ac") + 1));
        assertEquals("48000", args.get(args.indexOf("-ar") + 1));
        assertFalse(args.contains("-t"));
    }
    @Test public void videoUsesH264SquareAndLocaleIndependentTrim() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("ru", "RU"));
            List<String> args = MediaConversionPolicy.arguments(VIDEO_TO_ROUND, "/in.mp4", "/out.mp4", 10, 1.5, 3.5);
            assertEquals("1.500000", args.get(args.indexOf("-ss") + 1));
            assertEquals("2.000000", args.get(args.indexOf("-t") + 1));
            assertEquals("libx264", args.get(args.indexOf("-c:v") + 1));
            assertTrue(args.get(args.indexOf("-vf") + 1).contains("scale=384:384"));
        } finally { Locale.setDefault(previous); }
    }
    @Test public void voiceToMusicRequiresNoLossyReencoding() {
        try { MediaConversionPolicy.arguments(VOICE_TO_AUDIO, "/in.ogg", "/out.ogg", 2, 0, 0); fail(); }
        catch (IllegalArgumentException expected) { }
        assertEquals(64L * 1024 * 1024, MediaConversionPolicy.maxInputBytes(AUDIO_TO_VOICE));
    }
}
