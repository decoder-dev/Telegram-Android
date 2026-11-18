package org.telegram.messenger.banana;

/** Voice changer presets, the values of BananaGram iOS ForkVoiceChanger.swift. */
public final class VoicePresets {
    public static final int ANONYMOUS = 0, FEMALE = 1, MALE = 2, CHILD = 3, ROBOT = 4, CUSTOM = 5;
    public static final int[] PITCH_CHOICES = {-12, -9, -7, -5, -3, -2, -1, 0, 1, 2, 3, 5, 7, 9, 12};
    public static final int[] AMOUNT_CHOICES = {0, 10, 25, 40, 60, 80};

    private VoicePresets() {}

    /** The effect for these settings, or null when it would change nothing. */
    public static VoiceChain.Params paramsFor(boolean enabled, int preset, int pitch, int reverb, int echo) {
        if (!enabled) return null;
        VoiceChain.Params p = VoiceChain.Params.identity();
        switch (preset) {
            case ANONYMOUS:
                p.pitchSemitones = -3.5f; p.timbre = -0.35f; p.bassDb = 2f; p.trebleDb = -2f; p.distortion = 0.18f;
                p.reverbMix = 0.12f; p.reverbSize = 0.35f; p.jitter = 0.6f; p.breathiness = 0.15f; p.compression = 0.45f;
                break;
            case FEMALE:
                p.pitchSemitones = 5f; p.timbre = 0.45f; p.trebleDb = 2f; p.reverbMix = 0.04f;
                break;
            case MALE:
                p.pitchSemitones = -3.5f; p.timbre = -0.45f; p.bassDb = 3f; p.reverbMix = 0.03f;
                break;
            case CHILD:
                p.pitchSemitones = 7f; p.timbre = 0.65f; p.trebleDb = 3f; p.reverbMix = 0.05f;
                break;
            case ROBOT:
                p.ringModHz = 55f; p.ringModMix = 0.65f; p.distortion = 0.35f; p.bitcrushBits = 10f; p.telephone = 0.3f;
                p.reverbMix = 0.18f; p.reverbSize = 0.3f; p.echoMix = 0.12f; p.echoDelayMs = 70f; p.echoFeedback = 0.25f;
                p.compression = 0.5f;
                break;
            case CUSTOM:
                p.pitchSemitones = Math.max(-12, Math.min(12, pitch));
                p.reverbMix = Math.max(0, Math.min(100, reverb)) / 100f;
                p.reverbSize = 0.5f;
                p.echoMix = Math.max(0, Math.min(100, echo)) / 100f;
                p.echoDelayMs = 180f;
                p.echoFeedback = 0.3f;
                break;
            default:
                return null;
        }
        return p.isIdentity() ? null : p;
    }
}
