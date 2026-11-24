package org.telegram.messenger.banana;

import org.junit.Test;

import static org.junit.Assert.*;

public class AudioEffectsTest {
    private static final int RATE = 48000;

    private static double rmsDb(float[] samples, int from) {
        double sum = 0;
        for (int i = from; i < samples.length; i++) sum += samples[i] * samples[i];
        return 10 * Math.log10(sum / (samples.length - from));
    }

    @Test public void peakAndShelfFiltersHaveTheirGainWhereExpected() {
        Biquad peak = new Biquad();
        peak.peak(RATE, 230f, 1f, 6f);
        assertEquals(6.0, peak.magnitudeDb(230, RATE), 0.1);
        assertEquals(0.0, peak.magnitudeDb(10000, RATE), 0.3);
        Biquad low = new Biquad();
        low.lowShelf(RATE, 60f, 12f);
        assertEquals(12.0, low.magnitudeDb(15, RATE), 0.6);
        assertEquals(0.0, low.magnitudeDb(5000, RATE), 0.2);
        Biquad high = new Biquad();
        high.highShelf(RATE, 14000f, -6f);
        assertEquals(-6.0, high.magnitudeDb(21000, RATE), 0.8);
        assertEquals(0.0, high.magnitudeDb(200, RATE), 0.2);
    }

    @Test public void equalizerBoostsItsBandAfterHeadroom() {
        EqualizerDsp dsp = new EqualizerDsp();
        dsp.configure(new float[]{0, 0, 6, 0, 0}, RATE);
        assertEquals(Math.pow(10, -3.0 / 20), dsp.preamp(), 1e-4);
        float[] in = new float[RATE], out = new float[RATE];
        for (int i = 0; i < in.length; i++) {
            in[i] = 0.1f * (float) Math.sin(2 * Math.PI * 910 * i / RATE);
            out[i] = dsp.run(0, in[i]);
        }
        assertEquals(3.0, rmsDb(out, RATE / 4) - rmsDb(in, RATE / 4), 0.5);
    }

    @Test public void cutsNeedNoHeadroomAndFlatSettingsChangeNothing() {
        EqualizerDsp dsp = new EqualizerDsp();
        dsp.configure(new float[]{-6, 0, 0, 0, 0}, RATE);
        assertEquals(1f, dsp.preamp(), 1e-6);
        EqualizerDsp flat = new EqualizerDsp();
        flat.configure(new float[5], RATE);
        for (int i = 0; i < 100; i++) assertEquals(0.25f, flat.run(1, 0.25f), 1e-6);
    }

    @Test public void equalizerAppliesOnlyWhereTheSettingsSay() {
        EqualizerDsp.Snapshot off = new EqualizerDsp.Snapshot(new float[]{3, 0, 0, 0, 0}, false, true, 1);
        assertFalse(off.activeFor(EqualizerDsp.KIND_AUDIO));
        EqualizerDsp.Snapshot flat = new EqualizerDsp.Snapshot(new float[5], true, true, 1);
        assertFalse(flat.activeFor(EqualizerDsp.KIND_AUDIO));
        EqualizerDsp.Snapshot audioOnly = new EqualizerDsp.Snapshot(new float[]{3, 0, 0, 0, 0}, true, false, 1);
        assertTrue(audioOnly.activeFor(EqualizerDsp.KIND_AUDIO));
        assertTrue(audioOnly.activeFor(EqualizerDsp.KIND_ROUND));
        assertFalse(audioOnly.activeFor(EqualizerDsp.KIND_VIDEO));
        assertFalse(audioOnly.activeFor(EqualizerDsp.KIND_NONE));
        EqualizerDsp.Snapshot videos = new EqualizerDsp.Snapshot(new float[]{3, 0, 0, 0, 0}, true, true, 1);
        assertTrue(videos.activeFor(EqualizerDsp.KIND_VIDEO));
    }

    @Test public void sampleConversionSaturatesAndTheLimiterStaysBelowFullScale() {
        assertEquals(32767, EqualizerDsp.toInt16(2f));
        assertEquals(-32768, EqualizerDsp.toInt16(-2f));
        assertEquals(0.5f, EqualizerDsp.softLimit(0.5f), 0f);
        for (float x = -10f; x <= 10f; x += 0.25f) assertTrue(Math.abs(EqualizerDsp.softLimit(x)) <= 1f);
    }

    @Test public void presetsFollowTheIosTable() {
        assertNull(VoicePresets.paramsFor(false, VoicePresets.FEMALE, 0, 0, 0));
        assertNull(VoicePresets.paramsFor(true, VoicePresets.CUSTOM, 0, 0, 0));
        for (int preset = 0; preset <= 4; preset++) assertNotNull(VoicePresets.paramsFor(true, preset, 0, 0, 0));
        VoiceChain.Params female = VoicePresets.paramsFor(true, VoicePresets.FEMALE, 0, 0, 0);
        assertEquals(5f, female.pitchSemitones, 0f);
        VoiceChain.Params robot = VoicePresets.paramsFor(true, VoicePresets.ROBOT, 0, 0, 0);
        assertEquals(10f, robot.bitcrushBits, 0f);
        VoiceChain.Params custom = VoicePresets.paramsFor(true, VoicePresets.CUSTOM, 40, 60, 25);
        assertEquals(12f, custom.pitchSemitones, 0f);
        assertEquals(0.6f, custom.reverbMix, 1e-6f);
        assertEquals(0.25f, custom.echoMix, 1e-6f);
    }

    @Test public void liveseyPresetMatchesTheTunedIosValues() {
        VoiceChain.Params livesey = VoicePresets.paramsFor(true, VoicePresets.LIVESEY, 0, 0, 0);
        assertNotNull(livesey);
        assertEquals(198f, livesey.referencePitchHz, 0f);
        assertEquals(0f, livesey.pitchSemitones, 0f);
        assertEquals(0.1f, livesey.timbre, 1e-6f);
        assertEquals(0f, livesey.bassDb, 1e-6f);
        assertEquals(-1f, livesey.midDb, 1e-6f);
        assertEquals(0.75f, livesey.trebleDb, 1e-6f);
        assertEquals(0.1f, livesey.distortion, 1e-6f);
        assertEquals(0.2f, livesey.compression, 1e-6f);
        assertEquals(0.07f, livesey.reverbMix, 1e-6f);
        assertEquals(0.2f, livesey.reverbSize, 1e-6f);
    }

    @Test public void referencePitchPullsAVoiceTowardsTheReference() {
        // A 120 Hz voice needs 12*log2(198/120) = 8.68 semitones, inside the +/-9 cap, so the
        // settled output must sit at the reference itself rather than at a fixed offset.
        short[] pcm = sine(120, RATE * 3);
        new VoiceChain(VoicePresets.paramsFor(true, VoicePresets.LIVESEY, 0, 0, 0), RATE, 7).process(pcm, pcm.length);
        assertEquals(198, zeroCrossingHz(pcm, 2 * RATE), 198 * 0.08);
    }

    @Test public void referencePitchCapStopsAtNineSemitones() {
        // A 100 Hz voice would need 11.8 semitones to reach 198 Hz; the cap leaves it at +9.
        short[] pcm = sine(100, RATE * 3);
        new VoiceChain(VoicePresets.paramsFor(true, VoicePresets.LIVESEY, 0, 0, 0), RATE, 7).process(pcm, pcm.length);
        double shifted = 100 * Math.pow(2.0, 9.0 / 12.0);
        assertEquals(shifted, zeroCrossingHz(pcm, 2 * RATE), shifted * 0.08);
    }

    private static double zeroCrossingHz(short[] pcm, int from) {
        int crossings = 0;
        for (int i = from + 1; i < pcm.length; i++) if ((pcm[i - 1] < 0) != (pcm[i] < 0)) crossings++;
        return crossings / 2.0 / ((pcm.length - from) / (double) RATE);
    }

    private static short[] sine(double hz, int samples) {
        short[] pcm = new short[samples];
        for (int i = 0; i < samples; i++) pcm[i] = (short) (8000 * Math.sin(2 * Math.PI * hz * i / RATE));
        return pcm;
    }

    @Test public void pitchShiftMovesAnOctave() {
        short[] up = sine(440, RATE);
        new VoiceChain(VoicePresets.paramsFor(true, VoicePresets.CUSTOM, 12, 0, 0), RATE, 7).process(up, up.length);
        assertEquals(880, zeroCrossingHz(up, RATE / 5), 880 * 0.05);
        short[] down = sine(440, RATE);
        new VoiceChain(VoicePresets.paramsFor(true, VoicePresets.CUSTOM, -12, 0, 0), RATE, 7).process(down, down.length);
        assertEquals(220, zeroCrossingHz(down, RATE / 5), 220 * 0.05);
    }

    @Test public void echoRepeatsAnImpulseAfterItsDelay() {
        VoiceChain chain = new VoiceChain(VoicePresets.paramsFor(true, VoicePresets.CUSTOM, 0, 0, 60), RATE, 7);
        float[] out = new float[RATE];
        for (int i = 0; i < out.length; i++) out[i] = chain.processSample(i == 0 ? 0.5f : 0f);
        int delay = (int) (0.18f * RATE);
        assertEquals(0.5f * 0.6f, out[delay], 0.01f);
        assertEquals(0.5f * 0.3f * 0.6f, out[2 * delay], 0.01f);
        assertEquals(0f, out[delay / 2], 1e-6f);
    }

    @Test public void reverbLeavesATailAndSilenceStaysSilent() {
        VoiceChain reverb = new VoiceChain(VoicePresets.paramsFor(true, VoicePresets.CUSTOM, 0, 60, 0), RATE, 7);
        float tail = 0f;
        for (int i = 0; i < RATE / 2; i++) {
            float y = reverb.processSample(i == 0 ? 0.5f : 0f);
            if (i > RATE / 5) tail = Math.max(tail, Math.abs(y));
        }
        assertTrue(tail > 0f);
        for (int preset : new int[]{VoicePresets.ANONYMOUS, VoicePresets.FEMALE, VoicePresets.MALE, VoicePresets.CHILD}) {
            VoiceChain chain = new VoiceChain(VoicePresets.paramsFor(true, preset, 0, 0, 0), RATE, 7);
            short[] silence = new short[RATE / 10];
            chain.process(silence, silence.length);
            for (short s : silence) assertEquals(0, s);
        }
    }

    @Test public void fullScaleInputNeverOverflowsAndOutputIsDeterministic() {
        short[] a = new short[RATE / 4], b = new short[RATE / 4];
        for (int i = 0; i < a.length; i++) a[i] = b[i] = (i / 50) % 2 == 0 ? Short.MAX_VALUE : Short.MIN_VALUE;
        new VoiceChain(VoicePresets.paramsFor(true, VoicePresets.ROBOT, 0, 0, 0), RATE, 42).process(a, a.length);
        new VoiceChain(VoicePresets.paramsFor(true, VoicePresets.ROBOT, 0, 0, 0), RATE, 42).process(b, b.length);
        assertArrayEquals(a, b);
    }

    @Test public void roundVideoBitratesMatchIos() {
        assertEquals(1_200_000, RoundVideoProfile.bitrate(1_200_000, false, false));
        assertEquals(1_500_000, RoundVideoProfile.bitrate(1_200_000, true, false));
        assertEquals(5_000_000, RoundVideoProfile.bitrate(1_200_000, false, true));
        assertEquals(7_500_000, RoundVideoProfile.bitrate(1_200_000, true, true));
    }
}
