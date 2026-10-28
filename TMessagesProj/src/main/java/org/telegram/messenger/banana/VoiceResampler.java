/* GPL-2.0-or-later. PCM resampling adapted from GlassgramVoice, b30831c800ab. */
package org.telegram.messenger.banana;

final class VoiceResampler {
        private final double step;
        private final float[] window;
        private int windowPos;
        private float windowSum;
        private double pos;
        private float last;

        VoiceResampler(int rate) {
            if (rate < 8000 || rate > 192000) throw new IllegalArgumentException("Unsupported PCM rate");
            step = rate / (double) 16000;
            window = new float[Math.max(1, (int) Math.round(step))];
        }

        int maxOutput(int frames) {
            return (int) Math.ceil(frames / step) + 2;
        }

        int feed(float[] in, int n, short[] out) {
            // filtered in place: each sample the mean of the last ones
            for (int i = 0; i < n; i++) {
                windowSum += in[i] - window[windowPos];
                window[windowPos] = in[i];
                windowPos = (windowPos + 1) % window.length;
                in[i] = windowSum / window.length;
            }
            int k = 0;
            while (true) {
                final int i0 = (int) Math.floor(pos);
                final int i1 = i0 + 1;
                if (i1 >= n) {
                    break;
                }
                final float a = i0 < 0 ? last : in[i0];
                final float v = a + (in[i1] - a) * (float) (pos - i0);
                out[k++] = (short) Math.max(-32768, Math.min(32767, Math.round(v * 32767)));
                pos += step;
            }
            pos -= n;
            if (n > 0) {
                last = in[n - 1];
            }
            return k;
        }
    }
