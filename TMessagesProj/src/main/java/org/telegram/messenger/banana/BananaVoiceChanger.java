package org.telegram.messenger.banana;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * Changes the voice of voice messages while recording (ported from BananaGram iOS): the real voice is
 * never stored or sent. Round videos and calls are not affected. A new chain per recording.
 */
public final class BananaVoiceChanger {
    private static volatile int armedAccount = -1;

    private BananaVoiceChanger() {}

    public static VoiceChain create(int account, int sampleRate) {
        if (account < 0) return null;
        VoiceChain.Params params = VoicePresets.paramsFor(
                BananaConfig.get(account, BananaConfig.Option.VOICE_CHANGER_ENABLED),
                BananaConfig.getInt(account, BananaConfig.IntValue.VOICE_CHANGER_PRESET),
                BananaConfig.getInt(account, BananaConfig.IntValue.VOICE_CHANGER_PITCH),
                BananaConfig.getInt(account, BananaConfig.IntValue.VOICE_CHANGER_REVERB),
                BananaConfig.getInt(account, BananaConfig.IntValue.VOICE_CHANGER_ECHO));
        if (params == null) return null;
        return new VoiceChain(params, sampleRate, 0x9E3779B9 ^ (int) System.nanoTime());
    }

    /** The recorder v2 capture has no account; MediaController names it when a recording starts. */
    public static void arm(int account) {
        armedAccount = account;
    }

    public static VoiceChain createArmed(int sampleRate) {
        return create(armedAccount, sampleRate);
    }

    /** Processes native-order 16-bit mono PCM in [0, byteCount) in place; the buffer's position and limit stay. */
    public static void process(VoiceChain chain, ByteBuffer buffer, int byteCount) {
        if (chain == null || buffer == null || byteCount < 2) return;
        ByteBuffer view = buffer.duplicate();
        view.order(ByteOrder.nativeOrder());
        view.position(0);
        view.limit(Math.min(byteCount, view.capacity()));
        ShortBuffer samples = view.asShortBuffer();
        int count = samples.remaining();
        short[] scratch = chain.scratch(count);
        samples.get(scratch, 0, count);
        chain.process(scratch, count);
        samples.position(0);
        samples.put(scratch, 0, count);
    }
}
