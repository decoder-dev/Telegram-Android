package org.telegram.messenger.banana;

/**
 * RBJ cookbook biquads in transposed direct form II, ported from BananaGram iOS ForkVoiceDSP.c.
 * Setting new coefficients keeps the state, so a gain change does not click.
 */
final class Biquad {
    static final float PI = 3.14159265358979f;
    float b0 = 1f, b1, b2, a1, a2;
    float z1, z2;

    float run(float x) {
        float y = b0 * x + z1;
        z1 = b1 * x - a1 * y + z2;
        z2 = b2 * x - a2 * y;
        return y;
    }

    void reset() {
        z1 = z2 = 0f;
    }

    static float clampFrequency(float frequency, float sampleRate) {
        float limit = sampleRate * 0.45f;
        if (frequency > limit) return limit;
        if (frequency < 10f) return 10f;
        return frequency;
    }

    void set(float nb0, float nb1, float nb2, float na0, float na1, float na2) {
        float inv = 1f / na0;
        b0 = nb0 * inv;
        b1 = nb1 * inv;
        b2 = nb2 * inv;
        a1 = na1 * inv;
        a2 = na2 * inv;
    }

    void identity() {
        b0 = 1f;
        b1 = b2 = a1 = a2 = 0f;
    }

    void peak(float sampleRate, float frequency, float q, float gainDb) {
        float a = (float) Math.pow(10.0, gainDb / 40.0);
        float w0 = 2f * PI * clampFrequency(frequency, sampleRate) / sampleRate;
        float cosw = (float) Math.cos(w0);
        float alpha = (float) Math.sin(w0) / (2f * q);
        set(1f + alpha * a, -2f * cosw, 1f - alpha * a, 1f + alpha / a, -2f * cosw, 1f - alpha / a);
    }

    void lowShelf(float sampleRate, float frequency, float gainDb) {
        float a = (float) Math.pow(10.0, gainDb / 40.0);
        float w0 = 2f * PI * clampFrequency(frequency, sampleRate) / sampleRate;
        float cosw = (float) Math.cos(w0);
        float alpha = (float) Math.sin(w0) * 0.5f * 1.41421356f;
        float s = 2f * (float) Math.sqrt(a) * alpha;
        set(a * ((a + 1f) - (a - 1f) * cosw + s),
                2f * a * ((a - 1f) - (a + 1f) * cosw),
                a * ((a + 1f) - (a - 1f) * cosw - s),
                (a + 1f) + (a - 1f) * cosw + s,
                -2f * ((a - 1f) + (a + 1f) * cosw),
                (a + 1f) + (a - 1f) * cosw - s);
    }

    void highShelf(float sampleRate, float frequency, float gainDb) {
        float a = (float) Math.pow(10.0, gainDb / 40.0);
        float w0 = 2f * PI * clampFrequency(frequency, sampleRate) / sampleRate;
        float cosw = (float) Math.cos(w0);
        float alpha = (float) Math.sin(w0) * 0.5f * 1.41421356f;
        float s = 2f * (float) Math.sqrt(a) * alpha;
        set(a * ((a + 1f) + (a - 1f) * cosw + s),
                -2f * a * ((a - 1f) + (a + 1f) * cosw),
                a * ((a + 1f) + (a - 1f) * cosw - s),
                (a + 1f) - (a - 1f) * cosw + s,
                2f * ((a - 1f) - (a + 1f) * cosw),
                (a + 1f) - (a - 1f) * cosw - s);
    }

    void highPass(float sampleRate, float frequency) {
        float w0 = 2f * PI * clampFrequency(frequency, sampleRate) / sampleRate;
        float cosw = (float) Math.cos(w0);
        float alpha = (float) Math.sin(w0) / (2f * 0.7071f);
        set((1f + cosw) * 0.5f, -(1f + cosw), (1f + cosw) * 0.5f, 1f + alpha, -2f * cosw, 1f - alpha);
    }

    void lowPass(float sampleRate, float frequency) {
        float w0 = 2f * PI * clampFrequency(frequency, sampleRate) / sampleRate;
        float cosw = (float) Math.cos(w0);
        float alpha = (float) Math.sin(w0) / (2f * 0.7071f);
        set((1f - cosw) * 0.5f, 1f - cosw, (1f - cosw) * 0.5f, 1f + alpha, -2f * cosw, 1f - alpha);
    }

    /** Magnitude in dB at a frequency, for tests. */
    double magnitudeDb(double frequency, double sampleRate) {
        double w = 2 * Math.PI * frequency / sampleRate;
        double cr = Math.cos(w), ci = -Math.sin(w), c2r = Math.cos(2 * w), c2i = -Math.sin(2 * w);
        double nr = b0 + b1 * cr + b2 * c2r, ni = b1 * ci + b2 * c2i;
        double dr = 1 + a1 * cr + a2 * c2r, di = a1 * ci + a2 * c2i;
        return 10 * Math.log10((nr * nr + ni * ni) / (dr * dr + di * di));
    }

    static float fastTanh(float x) {
        if (x > 3f) return 1f;
        if (x < -3f) return -1f;
        float x2 = x * x;
        return x * (27f + x2) / (27f + 9f * x2);
    }

    static float softLimit(float x) {
        float a = Math.abs(x);
        if (a <= 0.8f) return x;
        float limited = 0.8f + 0.2f * fastTanh((a - 0.8f) / 0.2f);
        return x < 0f ? -limited : limited;
    }

    static short toInt16(float x) {
        float scaled = x * 32767f;
        if (scaled > 32767f) return 32767;
        if (scaled < -32768f) return -32768;
        return (short) Math.round(scaled);
    }
}
