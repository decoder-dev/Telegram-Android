package org.telegram.banana;

import android.os.Build;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.telegram.messenger.*;
import org.telegram.messenger.banana.BananaMediaConverter;
import org.telegram.messenger.banana.MediaConversionPolicy.Kind;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;
import java.io.*;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/** Real on-device encoder/container/MTProto checks. Public generated fixtures, no message transmission. */
public class MediaConversionAndroidTest {
    private void initialize() {
        assertTrue("Fresh emulator only", "ranchu".equals(Build.HARDWARE) || "goldfish".equals(Build.HARDWARE));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(ApplicationLoader::postInitApplication);
        for (int n = 0; n < UserConfig.MAX_ACCOUNT_COUNT; n++) assertFalse(UserConfig.getInstance(n).isClientActivated());
    }
    @Test public void musicBecomesOpusVoiceAndVoiceBecomesMusicWithoutChangingBytes() throws Exception {
        initialize(); File music = fixture("conversion-music.mp3");
        BananaMediaConverter.Result voice = null, audio = null;
        try {
            byte[] original = digest(music);
            voice = convert(music, Kind.AUDIO_TO_VOICE, 0, 0);
            TLRPC.TL_document voiceDoc = roundTrip(voice.document(false));
            assertEquals("audio/ogg", voiceDoc.mime_type);
            assertTrue(MessageObject.isVoiceDocument(voiceDoc));
            assertFalse(MessageObject.isMusicDocument(voiceDoc));
            TLRPC.TL_documentAttributeAudio voiceAttr = audioAttribute(voiceDoc);
            assertTrue(voiceAttr.voice); assertNotNull(voiceAttr.waveform); assertTrue(voiceAttr.waveform.length > 0);
            assertTrue(voiceAttr.duration >= 1.9 && voiceAttr.duration < 2.6);
            assertArrayEquals("Converting must not overwrite source music", original, digest(music));
            byte[] originalVoice = digest(voice.file);
            audio = convert(voice.file, Kind.VOICE_TO_AUDIO, 0, 0);
            TLRPC.TL_document audioDoc = roundTrip(audio.document(false));
            assertTrue(MessageObject.isMusicDocument(audioDoc));
            assertFalse(MessageObject.isVoiceDocument(audioDoc));
            TLRPC.TL_documentAttributeAudio audioAttr = audioAttribute(audioDoc);
            assertFalse(audioAttr.voice); assertNull(audioAttr.waveform); assertNotNull(audioAttr.title);
            assertArrayEquals("Voice-to-music must preserve the Opus stream exactly", originalVoice, digest(audio.file));
            assertArrayEquals(originalVoice, digest(voice.file));
        } finally { music.delete(); if (voice != null) voice.discard(); if (audio != null) audio.discard(); }
    }
    @Test public void nonSquareVideoBecomesAnH264RoundMessageWithSelectedTrim() throws Exception {
        initialize(); File video = fixture("conversion-video.mp4"); BananaMediaConverter.Result result = null;
        try {
            byte[] original = digest(video);
            result = convert(video, Kind.VIDEO_TO_ROUND, 0.5, 1.5);
            TLRPC.TL_document document = roundTrip(result.document(false));
            assertEquals("video/mp4", document.mime_type);
            assertTrue(MessageObject.isRoundVideoDocument(document));
            TLRPC.TL_documentAttributeVideo attribute = null;
            for (TLRPC.DocumentAttribute value : document.attributes) if (value instanceof TLRPC.TL_documentAttributeVideo) attribute = (TLRPC.TL_documentAttributeVideo) value;
            assertNotNull(attribute); assertTrue(attribute.round_message); assertEquals(384, attribute.w); assertEquals(384, attribute.h);
            assertTrue(attribute.duration >= 0.9 && attribute.duration < 1.5);
            assertFalse("A round video needs a usable preview", document.thumbs.isEmpty());
            assertArrayEquals(original, digest(video));
        } finally { video.delete(); if (result != null) result.discard(); }
    }
    @Test public void cancellingConversionNeverProducesAnAdoptableResult() throws Exception {
        initialize(); File music = fixture("conversion-music.mp3");
        try {
            byte[] original = digest(music); CountDownLatch done = new CountDownLatch(1);
            AtomicReference<BananaMediaConverter.Result> answer = new AtomicReference<>();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                BananaMediaConverter.Job job = BananaMediaConverter.convertFile(music, Kind.AUDIO_TO_VOICE, 0, 0, result -> { answer.set(result); done.countDown(); });
                job.cancel();
            });
            assertTrue(done.await(30, TimeUnit.SECONDS)); assertNull(answer.get()); assertArrayEquals(original, digest(music));
        } finally { music.delete(); }
    }
    @Test public void missingLocalFileFailsPromptlyWithoutWaitingForANetworkDownload() throws Exception {
        initialize(); File absent = new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir(), "absent-" + UUID.randomUUID());
        CountDownLatch done = new CountDownLatch(1); AtomicReference<BananaMediaConverter.Result> answer = new AtomicReference<>();
        BananaMediaConverter.Job job = BananaMediaConverter.convertFile(absent, Kind.AUDIO_TO_VOICE, 0, 0, result -> { answer.set(result); done.countDown(); });
        try { assertTrue("Local files must not wait for Telegram downloads", done.await(10, TimeUnit.SECONDS)); assertNull(answer.get()); }
        finally { job.cancel(); }
    }
    private BananaMediaConverter.Result convert(File source, Kind kind, double start, double end) throws Exception {
        CountDownLatch done = new CountDownLatch(1); AtomicReference<BananaMediaConverter.Result> answer = new AtomicReference<>();
        BananaMediaConverter.Job job = BananaMediaConverter.convertFile(source, kind, start, end, result -> { answer.set(result); done.countDown(); });
        try { assertTrue("Local conversion must finish", done.await(120, TimeUnit.SECONDS)); assertNotNull("Encoder/container validation failed: " + job.diagnosticCode(), answer.get()); return answer.get(); }
        catch (Throwable failure) { job.cancel(); throw failure; }
    }
    private File fixture(String name) throws IOException {
        File file = new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir(), UUID.randomUUID() + name);
        try (InputStream in = InstrumentationRegistry.getInstrumentation().getContext().getAssets().open(name); OutputStream out = new FileOutputStream(file)) {
            byte[] bytes = new byte[8192]; int n; while ((n = in.read(bytes)) != -1) out.write(bytes, 0, n);
        }
        return file;
    }
    private byte[] digest(File file) throws Exception {
        MessageDigest hash = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new FileInputStream(file)) { byte[] bytes = new byte[8192]; int n; while ((n = in.read(bytes)) != -1) hash.update(bytes, 0, n); }
        return hash.digest();
    }
    private TLRPC.TL_document roundTrip(TLRPC.TL_document document) {
        SerializedData output = new SerializedData(); document.serializeToStream(output);
        SerializedData input = new SerializedData(output.toByteArray());
        try { return (TLRPC.TL_document) TLRPC.Document.TLdeserialize(input, input.readInt32(true), true); }
        finally { input.cleanup(); output.cleanup(); }
    }
    private TLRPC.TL_documentAttributeAudio audioAttribute(TLRPC.TL_document document) {
        for (TLRPC.DocumentAttribute value : document.attributes) if (value instanceof TLRPC.TL_documentAttributeAudio) return (TLRPC.TL_documentAttributeAudio) value;
        throw new AssertionError("Missing audio attribute");
    }
}
