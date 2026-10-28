package org.telegram.messenger.banana;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Container/codec rules matching BananaGram iOS's explicit media conversion actions. */
public final class MediaConversionPolicy {
    public enum Kind { VIDEO_TO_ROUND, AUDIO_TO_VOICE, VOICE_TO_AUDIO }
    public static final int ROUND_SIDE = 384;
    public static final int MAX_ROUND_SECONDS = 60;
    public static final int MAX_VOICE_SECONDS = 3600;
    public static final long MAX_OUTPUT_BYTES = 64L * 1024 * 1024;
    private MediaConversionPolicy() { }

    public static long maxInputBytes(Kind kind) {
        return kind == Kind.VIDEO_TO_ROUND ? 256L * 1024 * 1024 : MAX_OUTPUT_BYTES;
    }
    public static double outputDuration(Kind kind, double duration, double start, double end) {
        if (!Double.isFinite(duration) || duration <= 0 || !Double.isFinite(start) || start < 0 || start >= duration ||
                !Double.isFinite(end) || end < 0 || end > 0 && end <= start) throw new IllegalArgumentException("Invalid media duration");
        if (kind != Kind.VIDEO_TO_ROUND) {
            if (kind == Kind.AUDIO_TO_VOICE && duration > MAX_VOICE_SECONDS) throw new IllegalArgumentException("Audio exceeds one hour");
            return duration;
        }
        double stop = end > 0 ? Math.min(duration, end) : duration;
        return Math.min(MAX_ROUND_SECONDS, stop - start);
    }
    /** Direct process arguments, never a shell expression. Input metadata cannot supply extra options. */
    public static List<String> arguments(Kind kind, String input, String output, double duration, double start, double end) {
        double seconds = outputDuration(kind, duration, start, end);
        if (kind == Kind.VOICE_TO_AUDIO) throw new IllegalArgumentException("Lossless copy requires no encoder");
        List<String> args = new ArrayList<>(Arrays.asList("-hide_banner", "-loglevel", "error", "-nostdin", "-y"));
        if (kind == Kind.VIDEO_TO_ROUND && start > 0) args.addAll(Arrays.asList("-ss", number(start)));
        args.addAll(Arrays.asList("-i", input, "-map_metadata", "-1", "-sn", "-dn", "-threads", "2", "-filter_threads", "1"));
        if (kind == Kind.AUDIO_TO_VOICE) {
            args.addAll(Arrays.asList("-map", "0:a:0", "-vn", "-ac", "1", "-ar", "48000", "-c:a", "libopus", "-b:a", "32k", "-application", "voip", "-f", "ogg"));
        } else {
            args.addAll(Arrays.asList("-t", number(seconds), "-map", "0:v:0", "-map", "0:a:0?",
                    "-vf", "crop=min(iw\\,ih):min(iw\\,ih),scale=384:384,setsar=1", "-r", "30", "-c:v", "libx264",
                    "-preset", "veryfast", "-profile:v", "baseline", "-pix_fmt", "yuv420p", "-b:v", "800k",
                    "-c:a", "aac", "-ac", "1", "-ar", "48000", "-b:a", "64k", "-movflags", "+faststart", "-f", "mp4"));
        }
        args.add(output);
        return args;
    }
    private static String number(double value) { return String.format(Locale.US, "%.6f", value); }
}
