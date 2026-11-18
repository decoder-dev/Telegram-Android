package org.telegram.messenger.banana;

/**
 * Voice changer DSP for voice messages, a Java port of BananaGram iOS ForkVoiceDSP.c (mono, applied
 * while recording, before the waveform and the Opus encoder see the samples). Chorus, flanger and
 * vibrato are left out: no preset uses them.
 */
public final class VoiceChain {
    /** Effect parameters; {@link #identity()} changes nothing. */
    public static final class Params {
        public float pitchSemitones, timbre, bassDb, midDb, trebleDb, telephone, distortion;
        public float bitcrushBits = 16f, ringModHz, ringModMix, echoMix, echoDelayMs = 250f, echoFeedback;
        public float reverbMix, reverbSize = 0.5f, breathiness, compression, gainDb, jitter, wet = 1f;

        public static Params identity() {
            return new Params();
        }

        static float clamp(float value, float min, float max) {
            if (Float.isNaN(value)) return min;
            return Math.max(min, Math.min(max, value));
        }

        Params sanitized() {
            Params p = new Params();
            p.pitchSemitones = clamp(pitchSemitones, -12f, 12f);
            p.timbre = clamp(timbre, -1f, 1f);
            p.bassDb = clamp(bassDb, -12f, 12f);
            p.midDb = clamp(midDb, -12f, 12f);
            p.trebleDb = clamp(trebleDb, -12f, 12f);
            p.telephone = clamp(telephone, 0f, 1f);
            p.distortion = clamp(distortion, 0f, 1f);
            p.bitcrushBits = clamp(bitcrushBits, 2f, 16f);
            p.ringModHz = clamp(ringModHz, 0f, 1000f);
            p.ringModMix = clamp(ringModMix, 0f, 1f);
            p.echoMix = clamp(echoMix, 0f, 1f);
            p.echoDelayMs = clamp(echoDelayMs, 40f, 800f);
            p.echoFeedback = clamp(echoFeedback, 0f, 0.8f);
            p.reverbMix = clamp(reverbMix, 0f, 1f);
            p.reverbSize = clamp(reverbSize, 0f, 1f);
            p.breathiness = clamp(breathiness, 0f, 1f);
            p.compression = clamp(compression, 0f, 1f);
            p.gainDb = clamp(gainDb, -12f, 12f);
            p.jitter = clamp(jitter, 0f, 1f);
            p.wet = clamp(wet, 0f, 1f);
            return p;
        }

        public boolean isIdentity() {
            Params p = sanitized();
            if (p.wet < 0.001f) return Math.abs(p.gainDb) < 0.01f;
            if (Math.abs(p.pitchSemitones) > 0.01f || p.jitter > 0.001f) return false;
            if (Math.abs(p.timbre) > 0.01f || Math.abs(p.bassDb) > 0.01f || Math.abs(p.midDb) > 0.01f || Math.abs(p.trebleDb) > 0.01f) return false;
            if (p.telephone > 0.001f || p.distortion > 0.001f || p.bitcrushBits < 15.99f) return false;
            if (p.ringModHz > 0.01f && p.ringModMix > 0.001f) return false;
            if (p.echoMix > 0.001f || p.reverbMix > 0.001f) return false;
            return !(p.breathiness > 0.001f || p.compression > 0.001f || Math.abs(p.gainDb) > 0.01f);
        }
    }

    private static final int CONTROL_INTERVAL = 32;
    private static final float[] COMB_TUNING = {1116f, 1188f, 1277f, 1356f};
    private static final float[] ALLPASS_TUNING = {556f, 441f};

    private final Params p;
    private final int sampleRate;
    private int rng;

    private final boolean pitchActive;
    private float[] pitchBuffer;
    private int pitchMask, pitchWrite, controlCounter, jitterCounter, jitterPeriod;
    private float pitchWindow, pitchPhase, pitchRatio = 1f, jitterValue, jitterTarget;

    private final boolean timbreActive, eqActive, phoneActive, ringActive, compressorActive;
    private final Biquad timbreLow = new Biquad(), timbreHigh = new Biquad();
    private final Biquad eqLow = new Biquad(), eqMid = new Biquad(), eqHigh = new Biquad();
    private final Biquad phoneHigh = new Biquad(), phoneLow = new Biquad();
    private final float distortionDrive, distortionNorm, bitcrushLevels, ringIncrement;
    private float ringPhase;

    private float[] echoBuffer;
    private int echoMask, echoWrite, echoDelay;

    private float[][] combBuffers, allpassBuffers;
    private int[] combLength, combIndex, allpassLength, allpassIndex;
    private float[] combStore;
    private float combFeedback, combDamp;

    private final float breathRelease, breathCoefficient;
    private float breathEnvelope, breathHighPass, breathPreviousNoise;

    private float compressorEnvelope, compressorGain = 1f, compressorTargetGain = 1f;
    private float compressorThresholdDb, compressorRatio, compressorAttack, compressorRelease, compressorMakeupDb;
    private int compressorCounter;
    private final float gainLinear;

    public VoiceChain(Params params, int sampleRate, int seed) {
        this.p = params.sanitized();
        this.sampleRate = Math.max(8000, Math.min(96000, sampleRate));
        this.rng = seed == 0 ? 1 : seed;
        float rate = this.sampleRate;

        pitchActive = Math.abs(p.pitchSemitones) > 0.01f || p.jitter > 0.001f;
        if (pitchActive) {
            int size = nextPowerOfTwo((int) (rate * 0.1f));
            pitchBuffer = new float[size];
            pitchMask = size - 1;
            pitchWindow = Math.max(64f, Math.min(rate * 0.045f, pitchMask - 4f));
            pitchRatio = (float) Math.pow(2.0, p.pitchSemitones / 12.0);
            jitterPeriod = (int) (rate * 0.15f / CONTROL_INTERVAL) + 1;
        }
        timbreActive = Math.abs(p.timbre) > 0.01f;
        if (timbreActive) {
            timbreLow.lowShelf(rate, 300f, -6f * p.timbre);
            timbreHigh.highShelf(rate, 2500f, 9f * p.timbre);
        }
        eqActive = Math.abs(p.bassDb) > 0.01f || Math.abs(p.midDb) > 0.01f || Math.abs(p.trebleDb) > 0.01f;
        if (eqActive) {
            eqLow.lowShelf(rate, 200f, p.bassDb);
            eqMid.peak(rate, 1000f, 0.8f, p.midDb);
            eqHigh.highShelf(rate, 4000f, p.trebleDb);
        }
        phoneActive = p.telephone > 0.001f;
        if (phoneActive) {
            phoneHigh.highPass(rate, 300f);
            phoneLow.lowPass(rate, 3400f);
        }
        distortionDrive = 1f + 14f * p.distortion;
        distortionNorm = 1f / Biquad.fastTanh(distortionDrive);
        bitcrushLevels = (float) Math.pow(2.0, p.bitcrushBits - 1.0);
        ringActive = p.ringModHz > 0.01f && p.ringModMix > 0.001f;
        ringIncrement = p.ringModHz / rate;

        if (p.echoMix > 0.001f) {
            int size = nextPowerOfTwo((int) (rate * 0.82f));
            echoBuffer = new float[size];
            echoMask = size - 1;
            echoDelay = Math.max(1, Math.min(echoMask, (int) (p.echoDelayMs * 0.001f * rate)));
        }
        if (p.reverbMix > 0.001f) {
            combBuffers = new float[4][];
            combLength = new int[4];
            combIndex = new int[4];
            combStore = new float[4];
            float sizeScale = 0.75f + 0.5f * p.reverbSize;
            for (int i = 0; i < 4; i++) {
                int capacity = (int) (COMB_TUNING[i] * rate / 44100f * 1.35f) + 2;
                combBuffers[i] = new float[capacity];
                combLength[i] = Math.max(1, Math.min(capacity - 1, (int) (COMB_TUNING[i] * rate / 44100f * sizeScale)));
            }
            allpassBuffers = new float[2][];
            allpassLength = new int[2];
            allpassIndex = new int[2];
            for (int i = 0; i < 2; i++) {
                int capacity = (int) (ALLPASS_TUNING[i] * rate / 44100f) + 2;
                allpassBuffers[i] = new float[capacity];
                allpassLength[i] = Math.max(1, Math.min(capacity - 1, (int) (ALLPASS_TUNING[i] * rate / 44100f)));
            }
            combFeedback = 0.70f + 0.25f * p.reverbSize;
            combDamp = 0.3f;
        }
        breathRelease = (float) Math.exp(-1.0 / (0.03 * rate));
        breathCoefficient = (float) Math.exp(-2.0 * Math.PI * 2000.0 / rate);
        compressorActive = p.compression > 0.001f;
        if (compressorActive) {
            compressorThresholdDb = -8f - 22f * p.compression;
            compressorRatio = 1f + 5f * p.compression;
            compressorAttack = 1f - (float) Math.exp(-1.0 / (0.005 * rate));
            compressorRelease = 1f - (float) Math.exp(-1.0 / (0.08 * rate));
            compressorMakeupDb = -compressorThresholdDb * (1f - 1f / compressorRatio) * 0.5f;
        }
        gainLinear = (float) Math.pow(10.0, p.gainDb / 20.0);
    }

    static int nextPowerOfTwo(int value) {
        int result = 1;
        while (result < value && result < (1 << 30)) result <<= 1;
        return result;
    }

    private int nextRandom() {
        int x = rng;
        x ^= x << 13;
        x ^= x >>> 17;
        x ^= x << 5;
        rng = x;
        return x;
    }

    private float randomBipolar() {
        return (nextRandom() >>> 8) * (1f / 8388608f) - 1f;
    }

    private static float wrap(float phase) {
        if (phase >= 1f || phase < 0f) phase -= (float) Math.floor(phase);
        return phase;
    }

    private static float sine(float phase) {
        return (float) Math.sin(2.0 * Math.PI * phase);
    }

    private static float delayRead(float[] buffer, int mask, int writeIndex, float delay) {
        float position = writeIndex - 1f - delay;
        float size = mask + 1;
        while (position < 0f) position += size;
        int i0 = ((int) position) & mask;
        float frac = position - (float) Math.floor(position);
        int i1 = (i0 + 1) & mask;
        return buffer[i0] + frac * (buffer[i1] - buffer[i0]);
    }

    private void updatePitchControl() {
        float semitones = p.pitchSemitones;
        if (p.jitter > 0.001f) {
            if (++jitterCounter >= jitterPeriod) {
                jitterCounter = 0;
                jitterTarget = randomBipolar() * 0.7f * p.jitter;
            }
            jitterValue += (jitterTarget - jitterValue) * 0.08f;
            semitones += jitterValue;
        }
        pitchRatio = (float) Math.pow(2.0, semitones / 12.0);
    }

    private float pitch(float x) {
        pitchBuffer[pitchWrite] = x;
        pitchWrite = (pitchWrite + 1) & pitchMask;
        float phase = wrap(pitchPhase + (1f - pitchRatio) / pitchWindow);
        pitchPhase = phase;
        float phase2 = phase + 0.5f;
        if (phase2 >= 1f) phase2 -= 1f;
        // sin²(πp) + sin²(π(p + ½)) = 1: each tap fades out exactly where its delay wraps.
        float s1 = sine(phase * 0.5f), s2 = sine(phase2 * 0.5f);
        float y1 = delayRead(pitchBuffer, pitchMask, pitchWrite, phase * pitchWindow);
        float y2 = delayRead(pitchBuffer, pitchMask, pitchWrite, phase2 * pitchWindow);
        return y1 * s1 * s1 + y2 * s2 * s2;
    }

    public float processSample(float dry) {
        float x = dry;
        if (pitchActive) {
            if (controlCounter == 0) updatePitchControl();
            controlCounter = (controlCounter + 1) % CONTROL_INTERVAL;
            x = pitch(x);
        }
        if (timbreActive) x = timbreHigh.run(timbreLow.run(x));
        if (eqActive) x = eqHigh.run(eqMid.run(eqLow.run(x)));
        if (phoneActive) {
            float band = phoneLow.run(phoneHigh.run(x));
            x = x + (band * 1.4f - x) * p.telephone;
        }
        if (p.distortion > 0.001f) {
            float saturated = Biquad.fastTanh(x * distortionDrive) * distortionNorm;
            x = x + (saturated - x) * p.distortion;
        }
        if (p.bitcrushBits < 15.99f) x = (float) Math.floor(x * bitcrushLevels + 0.5f) / bitcrushLevels;
        if (ringActive) {
            ringPhase = wrap(ringPhase + ringIncrement);
            x = x + (x * sine(ringPhase) - x) * p.ringModMix;
        }
        if (echoBuffer != null) {
            float delayed = echoBuffer[(echoWrite - echoDelay) & echoMask];
            echoBuffer[echoWrite] = x + delayed * p.echoFeedback;
            echoWrite = (echoWrite + 1) & echoMask;
            x = x + delayed * p.echoMix;
        }
        if (combBuffers != null) {
            float input = x * 0.3f, sum = 0f;
            for (int c = 0; c < 4; c++) {
                float[] buffer = combBuffers[c];
                int index = combIndex[c];
                float output = buffer[index];
                combStore[c] = output * (1f - combDamp) + combStore[c] * combDamp;
                buffer[index] = input + combStore[c] * combFeedback;
                if (++index >= combLength[c]) index = 0;
                combIndex[c] = index;
                sum += output;
            }
            float reverb = sum * 0.25f;
            for (int a = 0; a < 2; a++) {
                float[] buffer = allpassBuffers[a];
                int index = allpassIndex[a];
                float buffered = buffer[index];
                float output = buffered - reverb;
                buffer[index] = reverb + buffered * 0.5f;
                if (++index >= allpassLength[a]) index = 0;
                allpassIndex[a] = index;
                reverb = output;
            }
            x = x + reverb * p.reverbMix * 1.5f;
        }
        if (p.breathiness > 0.001f) {
            float level = Math.abs(x);
            breathEnvelope = level > breathEnvelope ? level : breathEnvelope * breathRelease;
            float noise = randomBipolar();
            breathHighPass = breathCoefficient * (breathHighPass + noise - breathPreviousNoise);
            breathPreviousNoise = noise;
            x += breathHighPass * breathEnvelope * p.breathiness * 0.6f;
        }
        if (compressorActive) {
            float level = Math.abs(x);
            float coefficient = level > compressorEnvelope ? compressorAttack : compressorRelease;
            compressorEnvelope += (level - compressorEnvelope) * coefficient;
            if (compressorCounter == 0) {
                float envelopeDb = 20f * (float) Math.log10(compressorEnvelope + 1e-6f);
                float over = envelopeDb - compressorThresholdDb;
                float gainDb = compressorMakeupDb;
                if (over > 0f) gainDb -= over * (1f - 1f / compressorRatio);
                compressorTargetGain = (float) Math.pow(10.0, gainDb / 20.0);
            }
            compressorCounter = (compressorCounter + 1) % 16;
            compressorGain += (compressorTargetGain - compressorGain) * 0.01f;
            x *= compressorGain;
        }
        x *= gainLinear;
        if (p.wet < 0.999f) x = dry + (x - dry) * p.wet;
        return Biquad.softLimit(x);
    }

    /** In place, 16-bit mono; the sample count never changes. */
    public void process(short[] pcm, int count) {
        final float scale = 1f / 32768f;
        for (int i = 0; i < count; i++) pcm[i] = Biquad.toInt16(processSample(pcm[i] * scale));
    }

    public int sampleRate() {
        return sampleRate;
    }

    private short[] scratch = new short[0];

    /** A reusable buffer of at least {@code count} samples; a chain belongs to one recording thread. */
    short[] scratch(int count) {
        if (scratch.length < count) scratch = new short[count];
        return scratch;
    }
}
