package org.telegram.messenger.banana;

/**
 * Playback equalizer DSP (ported from BananaGram iOS): a preamp, then 5 biquads per channel — a low
 * shelf at 60 Hz, peaks at 230/910/3600 Hz and a high shelf at 14 kHz, gains of -12..12 dB.
 */
public final class EqualizerDsp {
    public static final float[] FREQUENCIES = {60f, 230f, 910f, 3600f, 14000f};
    public static final int BANDS = FREQUENCIES.length;
    public static final int MAX_CHANNELS = 8;
    public static final int KIND_NONE = -1, KIND_AUDIO = 0, KIND_ROUND = 1, KIND_VIDEO = 2;

    /** What the settings say, published as one immutable object. */
    public static final class Snapshot {
        public final float[] gains;
        public final boolean enabled, videos;
        public final int generation;

        public Snapshot(float[] gains, boolean enabled, boolean videos, int generation) {
            this.gains = gains.clone();
            this.enabled = enabled;
            this.videos = videos;
            this.generation = generation;
        }

        public boolean hasGain() {
            for (float gain : gains) if (Math.abs(gain) > 0.05f) return true;
            return false;
        }

        /** Voice, music and round videos when on; other videos only with "Videos too". Service sounds never. */
        public boolean activeFor(int kind) {
            if (!enabled || !hasGain() || kind == KIND_NONE) return false;
            return kind != KIND_VIDEO || videos;
        }
    }

    private final Biquad[][] filters = new Biquad[MAX_CHANNELS][BANDS];
    private final Biquad[] coefficients = new Biquad[BANDS];
    private float preamp = 1f;

    public EqualizerDsp() {
        for (int b = 0; b < BANDS; b++) coefficients[b] = new Biquad();
        for (int c = 0; c < MAX_CHANNELS; c++) for (int b = 0; b < BANDS; b++) filters[c][b] = new Biquad();
    }

    public void configure(float[] gainsDb, float sampleRate) {
        float maxBoost = 0f;
        for (int i = 0; i < BANDS; i++) {
            float gain = gainsDb[i];
            if (gain > maxBoost) maxBoost = gain;
            Biquad f = coefficients[i];
            if (Math.abs(gain) <= 0.05f) {
                f.identity();
            } else if (i == 0) {
                f.lowShelf(sampleRate, FREQUENCIES[i], gain);
            } else if (i == BANDS - 1) {
                f.highShelf(sampleRate, FREQUENCIES[i], gain);
            } else {
                f.peak(sampleRate, FREQUENCIES[i], 1f, gain);
            }
        }
        // Half of the strongest boost as headroom: boosted bands do not clip the rest.
        preamp = (float) Math.pow(10.0, -0.5 * maxBoost / 20.0);
        for (int c = 0; c < MAX_CHANNELS; c++) {
            for (int b = 0; b < BANDS; b++) {
                Biquad state = filters[c][b];
                Biquad source = coefficients[b];
                state.b0 = source.b0;
                state.b1 = source.b1;
                state.b2 = source.b2;
                state.a1 = source.a1;
                state.a2 = source.a2;
            }
        }
    }

    public float preamp() {
        return preamp;
    }

    Biquad band(int index) {
        return coefficients[index];
    }

    public void reset() {
        for (int c = 0; c < MAX_CHANNELS; c++) for (int b = 0; b < BANDS; b++) filters[c][b].reset();
    }

    public float run(int channel, float x) {
        if (channel >= MAX_CHANNELS) return x;
        x *= preamp;
        Biquad[] chain = filters[channel];
        for (int b = 0; b < BANDS; b++) x = chain[b].run(x);
        return x;
    }

    public static short toInt16(float x) {
        return Biquad.toInt16(x);
    }

    public static float softLimit(float x) {
        return Biquad.softLimit(x);
    }
}
