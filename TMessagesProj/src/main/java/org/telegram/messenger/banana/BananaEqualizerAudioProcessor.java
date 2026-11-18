package org.telegram.messenger.banana;

import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.audio.BaseAudioProcessor;
import androidx.media3.common.util.UnstableApi;

import java.nio.ByteBuffer;

/** Runs the playback equalizer inside ExoPlayer's audio sink, before the visualizer. */
@OptIn(markerClass = UnstableApi.class)
public final class BananaEqualizerAudioProcessor extends BaseAudioProcessor {
    private final int kind;
    private final EqualizerDsp dsp = new EqualizerDsp();
    private int configuredGeneration = -1;
    private int configuredRate;
    private boolean active;

    public BananaEqualizerAudioProcessor(int kind) {
        this.kind = kind;
    }

    @Override
    protected AudioFormat onConfigure(AudioFormat inputAudioFormat) throws UnhandledAudioFormatException {
        if (kind == EqualizerDsp.KIND_NONE) return AudioFormat.NOT_SET;
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT && inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            return AudioFormat.NOT_SET;
        }
        return inputAudioFormat;
    }

    private void refresh(int sampleRate) {
        EqualizerDsp.Snapshot snapshot = BananaEqualizer.current();
        if (snapshot.generation == configuredGeneration && sampleRate == configuredRate) return;
        boolean wasActive = active;
        configuredGeneration = snapshot.generation;
        configuredRate = sampleRate;
        active = snapshot.activeFor(kind);
        if (active) {
            dsp.configure(snapshot.gains, sampleRate);
            if (!wasActive) dsp.reset();
        }
    }

    @Override
    public void queueInput(ByteBuffer input) {
        int remaining = input.remaining();
        if (remaining == 0) return;
        ByteBuffer output = replaceOutputBuffer(remaining);
        int channels = Math.max(1, inputAudioFormat.channelCount);
        refresh(inputAudioFormat.sampleRate);
        if (!active) {
            output.put(input);
        } else if (inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT) {
            int frames = remaining / (4 * channels);
            for (int i = 0; i < frames; i++) {
                for (int c = 0; c < channels; c++) {
                    output.putFloat(EqualizerDsp.softLimit(dsp.run(c, input.getFloat())));
                }
            }
            while (input.hasRemaining()) output.put(input.get());
        } else {
            int frames = remaining / (2 * channels);
            final float scale = 1f / 32768f;
            for (int i = 0; i < frames; i++) {
                for (int c = 0; c < channels; c++) {
                    output.putShort(EqualizerDsp.toInt16(dsp.run(c, input.getShort() * scale)));
                }
            }
            while (input.hasRemaining()) output.put(input.get());
        }
        output.flip();
    }

    @Override
    protected void onFlush(AudioProcessor.StreamMetadata streamMetadata) {
        dsp.reset();
    }

    @Override
    protected void onReset() {
        dsp.reset();
        configuredGeneration = -1;
        active = false;
    }
}
