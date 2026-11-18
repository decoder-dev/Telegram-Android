package org.telegram.messenger.banana;

/**
 * The equalizer settings as the audio processors read them: one immutable snapshot, replaced when the
 * account plays something or a setting changes. Nothing changes for anyone else; it is playback only.
 */
public final class BananaEqualizer {
    private static final BananaConfig.IntValue[] BANDS = {
            BananaConfig.IntValue.EQUALIZER_BAND_60, BananaConfig.IntValue.EQUALIZER_BAND_230,
            BananaConfig.IntValue.EQUALIZER_BAND_910, BananaConfig.IntValue.EQUALIZER_BAND_3600,
            BananaConfig.IntValue.EQUALIZER_BAND_14000
    };
    public static final int[] GAIN_CHOICES = {-12, -9, -6, -4, -2, 0, 2, 4, 6, 9, 12};

    private static volatile EqualizerDsp.Snapshot current = new EqualizerDsp.Snapshot(new float[EqualizerDsp.BANDS], false, false, 0);
    private static int generation;

    private BananaEqualizer() {}

    public static EqualizerDsp.Snapshot current() {
        return current;
    }

    public static synchronized void publish(int account) {
        if (account < 0) return;
        float[] gains = new float[BANDS.length];
        for (int i = 0; i < BANDS.length; i++) gains[i] = BananaConfig.getInt(account, BANDS[i]);
        current = new EqualizerDsp.Snapshot(gains,
                BananaConfig.get(account, BananaConfig.Option.EQUALIZER_ENABLED),
                BananaConfig.get(account, BananaConfig.Option.EQUALIZER_VIDEOS), ++generation);
    }

    public static BananaConfig.IntValue band(int index) {
        return BANDS[index];
    }
}
