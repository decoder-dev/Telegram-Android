package org.telegram.messenger.banana;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class VoiceResamplerTest {
    private short[] feed(int rate, float[] samples, int chunk) {
        VoiceResampler resampler=new VoiceResampler(rate); ArrayList<Short> all=new ArrayList<>();
        for(int offset=0;offset<samples.length;offset+=chunk){float[] input=Arrays.copyOfRange(samples,offset,Math.min(samples.length,offset+chunk));short[] out=new short[resampler.maxOutput(input.length)];int n=resampler.feed(input,input.length,out);for(int i=0;i<n;i++)all.add(out[i]);}
        short[] result=new short[all.size()];for(int i=0;i<result.length;i++)result[i]=all.get(i);return result;
    }
    @Test public void preservesStreamingStateAcrossDecoderChunks() {
        float[] samples=new float[48000];for(int i=0;i<samples.length;i++)samples[i]=(float)Math.sin(i*0.03);
        short[] whole=feed(48000,samples,samples.length);assertEquals(16000,whole.length);assertArrayEquals(whole,feed(48000,samples,137));
    }
    @Test public void handlesUpsamplingAndClipsPcm() {
        float[] samples=new float[8000];Arrays.fill(samples,2);short[] out=feed(8000,samples,71);
        assertTrue(out.length>=15998 && out.length<=16000);assertEquals(32767,out[out.length-1]);
    }
    @Test public void rejectsUnsupportedDecoderRate() {try{new VoiceResampler(0);fail();}catch(IllegalArgumentException expected){}}
}
