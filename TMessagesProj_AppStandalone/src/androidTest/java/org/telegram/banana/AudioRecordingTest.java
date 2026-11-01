package org.telegram.banana;

import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioDeviceInfo;
import android.media.AudioRecord;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.banana.BananaConfig;
import org.telegram.messenger.banana.BananaHooks;
import org.telegram.utils.recorder.audio.AudioRecordingSession;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import static org.junit.Assert.*;

/** Real capture/Opus on a disposable emulator only; no account, upload or send. */
public class AudioRecordingTest {
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private final AtomicReference<Exception> failure = new AtomicReference<>();
    private final AtomicReference<AudioRecordingSession.Result> result = new AtomicReference<>();
    private final CountDownLatch encoderBlocked = new CountDownLatch(1), releaseEncoder = new CountDownLatch(1);
    private AudioRecordingSession session;
    private File directory;
    private Object oldMicrophoneSetting;
    private boolean configured;

    @Before
    public void setup() throws Exception {
        assertTrue("Use an emulator", Build.PRODUCT.contains("sdk") || Build.FINGERPRINT.startsWith("generic"));
        Context context = instrumentation.getTargetContext();
        instrumentation.runOnMainSync(() -> {
            ApplicationLoader.postInitApplication();
            assertFalse("Never record with a real account", UserConfig.getInstance(0).isClientActivated());
            oldMicrophoneSetting = MessagesController.getMainSettings(0).getAll().get(BananaConfig.Option.FORCE_BUILT_IN_MIC.key);
            BananaConfig.set(0, BananaConfig.Option.FORCE_BUILT_IN_MIC, true);
            configured = true;
        });
        if (context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            try (ParcelFileDescriptor fd = instrumentation.getUiAutomation().executeShellCommand(
                    "pm grant " + context.getPackageName() + " android.permission.RECORD_AUDIO");
                 java.io.InputStream input = new ParcelFileDescriptor.AutoCloseInputStream(fd)) {
                while (input.read() != -1) { }
            }
        }
        assertEquals(PackageManager.PERMISSION_GRANTED, context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO));
        directory = new File(context.getCacheDir(), "audio-regression-" + java.util.UUID.randomUUID());
        assertTrue(directory.mkdirs());
        instrumentation.runOnMainSync(() -> {
            AudioDeviceInfo microphone = BananaHooks.preferredMicrophone(0);
            assertNotNull(microphone);
            assertEquals(AudioDeviceInfo.TYPE_BUILTIN_MIC, microphone.getType());
            session = new AudioRecordingSession.Builder(context)
                    .setOutputDirectory(directory).setMinimumDurationMs(200)
                    .setNoiseSuppression(AudioRecordingSession.NoiseSuppression.RNNOISE)
                    .setPreferredInputDevice(microphone).setPreviewPlaybackEnabled(false)
                    .setListener(new AudioRecordingSession.Listener() {
                        @Override public void onCompleted(AudioRecordingSession.Result completed) { result.set(completed); }
                        @Override public void onError(Exception error) { failure.set(error); }
                    }).setOutputListener(new AudioRecordingSession.OutputListener() { }).build();
            assertTrue(session.start());
        });
        await(() -> session.getStateInfo().getState() == AudioRecordingSession.State.RECORDING, "Capture must start", 15000);
        assertNull(failure.get());
    }

    @After
    public void cleanup() {
        releaseEncoder.countDown();
        instrumentation.runOnMainSync(() -> {
            if (session != null) session.release();
            if (!configured) return;
            android.content.SharedPreferences.Editor restore = MessagesController.getMainSettings(0).edit()
                    .remove(BananaConfig.Option.FORCE_BUILT_IN_MIC.key);
            if (oldMicrophoneSetting instanceof Boolean) restore.putBoolean(BananaConfig.Option.FORCE_BUILT_IN_MIC.key, (Boolean) oldMicrophoneSetting);
            restore.apply();
        });
        erase(directory);
    }

    @Test
    public void builtInCaptureSurvivesPauseTrimResumeAndProducesOpus() throws Exception {
        Object capture = field(session, "capture");
        AudioRecord recorder = (AudioRecord) field(capture, "audioRecord");
        assertNotNull(recorder.getPreferredDevice());
        assertEquals(AudioDeviceInfo.TYPE_BUILTIN_MIC, recorder.getPreferredDevice().getType());
        await(() -> recorder.getRoutedDevice() != null, "AudioRecord must expose its actual route", 5000);
        assertEquals(AudioDeviceInfo.TYPE_BUILTIN_MIC, recorder.getRoutedDevice().getType());
        await(() -> session.getRecordedDurationMs() >= 500, "PCM must arrive", 10000);
        instrumentation.runOnMainSync(() -> assertTrue(session.pauseRecording()));
        await(() -> session.getStateInfo().getState() == AudioRecordingSession.State.PREVIEWING, "Pause must finalize the segment", 15000);
        instrumentation.runOnMainSync(() -> {
            assertTrue(session.setTrimRange(100, 350));
            assertTrue(session.resumeRecording());
        });
        await(() -> session.getStateInfo().getState() == AudioRecordingSession.State.RECORDING &&
                session.getRecordedDurationMs() >= 600, "Resume must retain the trimmed prefix", 15000);
        instrumentation.runOnMainSync(() -> assertTrue(session.finish()));
        await(() -> result.get() != null || failure.get() != null, "Finish must report a result", 15000);
        assertNull(failure.get());
        assertNotNull(result.get());
        assertTrue(result.get().getDurationMs() >= 600);
        try (FileInputStream input = new FileInputStream(result.get().getFile())) {
            assertEquals('O', input.read()); assertEquals('g', input.read());
            assertEquals('g', input.read()); assertEquals('S', input.read());
        }
        android.media.MediaExtractor extractor = new android.media.MediaExtractor();
        try {
            extractor.setDataSource(result.get().getFile().getAbsolutePath());
            assertEquals(1, extractor.getTrackCount());
            assertEquals("audio/opus", extractor.getTrackFormat(0).getString(android.media.MediaFormat.KEY_MIME));
        } finally { extractor.release(); }
    }

    @Test
    public void stalledEncoderStopsCaptureBeforeThePcmQueueCanGrowWithoutLimit() throws Exception {
        Object capture = field(session, "capture");
        Handler encoder = (Handler) field(capture, "encoderHandler");
        encoder.post(() -> {
            encoderBlocked.countDown();
            try { releaseEncoder.await(30, TimeUnit.SECONDS); }
            catch (InterruptedException error) { Thread.currentThread().interrupt(); }
        });
        assertTrue(encoderBlocked.await(5, TimeUnit.SECONDS));
        Field stopped = capture.getClass().getDeclaredField("captureStopped"); stopped.setAccessible(true);
        await(() -> {
            try { return stopped.getBoolean(capture); }
            catch (IllegalAccessException error) { throw new AssertionError(error); }
        }, "An encoder stall must stop capture with bounded memory", 20000);
        java.util.concurrent.atomic.AtomicInteger queued = (java.util.concurrent.atomic.AtomicInteger) field(capture, "queuedChunks");
        assertTrue("The queue must stay bounded while the consumer is blocked", queued.get() <= 1024);
        releaseEncoder.countDown();
        await(() -> failure.get() != null, "An encoder stall must report its failure", 15000);
        assertNull("A failed capture must never be presented as a completed voice", result.get());
    }

    private void await(BooleanSupplier condition, String message, long timeout) {
        long deadline = SystemClock.elapsedRealtime() + timeout;
        while (SystemClock.elapsedRealtime() < deadline) {
            AtomicBoolean ready = new AtomicBoolean();
            instrumentation.runOnMainSync(() -> ready.set(condition.getAsBoolean()));
            if (ready.get()) return;
            SystemClock.sleep(40);
        }
        fail(message + (failure.get() == null ? "" : ": " + failure.get().getClass().getSimpleName()));
    }

    private static Object field(Object object, String name) throws Exception {
        assertNotNull(object);
        Field field = object.getClass().getDeclaredField(name); field.setAccessible(true);
        return field.get(object);
    }

    private static void erase(File file) {
        if (file == null) return;
        File[] children = file.listFiles();
        if (children != null) for (File child : children) erase(child);
        file.delete();
    }
}
