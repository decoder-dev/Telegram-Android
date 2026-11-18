package org.telegram.messenger.banana;

/**
 * Round video bitrates from BananaGram iOS RoundVideoRecordingProfile: 1.2 Mbit/s normally, 1.5 at
 * 60 FPS; "maximum quality" 5, or 7.5 at 60 FPS. The output stays 480p on Android.
 */
public final class RoundVideoProfile {
    private RoundVideoProfile() {}

    public static int bitrate(int base, boolean sixtyFps, boolean maximumQuality) {
        if (maximumQuality) return sixtyFps ? 7_500_000 : 5_000_000;
        if (sixtyFps) return Math.max(base, 1_500_000);
        return base;
    }
}
