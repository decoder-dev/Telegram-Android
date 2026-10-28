package org.telegram.messenger.banana;

import android.content.Context;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.SystemClock;
import com.yausername.ffmpeg.FFmpeg;
import org.telegram.messenger.*;
import org.telegram.tgnet.TLRPC;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import static org.telegram.messenger.banana.MediaConversionPolicy.Kind;

/** Local conversion on a bounded worker. Source files and source TL documents are never modified. */
public final class BananaMediaConverter {
    private static final DispatchQueue QUEUE = new DispatchQueue("bananaMediaConversion");
    private static final AtomicBoolean BUSY = new AtomicBoolean();
    private BananaMediaConverter() { }
    public static final class Result {
        public final File file;
        public final Kind kind;
        public final double duration;
        private volatile boolean adopted;
        private final byte[] waveform;
        private final TLRPC.PhotoSize thumbnail;
        private Result(File file, Kind kind, double duration, byte[] waveform, TLRPC.PhotoSize thumbnail) { this.file = file; this.kind = kind; this.duration = duration; this.waveform = waveform; this.thumbnail = thumbnail; }
        public void adopt() { adopted = true; }
        public void discard() { if (!adopted) { file.delete(); discardThumbnail(thumbnail); } }
        public TLRPC.TL_document document(boolean secret) {
            TLRPC.TL_document doc = new TLRPC.TL_document();
            doc.file_reference = new byte[0]; doc.size = file.length();
            doc.mime_type = kind == Kind.VIDEO_TO_ROUND ? "video/mp4" : "audio/ogg";
            TLRPC.TL_documentAttributeFilename name = new TLRPC.TL_documentAttributeFilename();
            name.file_name = kind == Kind.VIDEO_TO_ROUND ? "video.mp4" : kind == Kind.AUDIO_TO_VOICE ? "voice.ogg" : LocaleController.getString(R.string.BananaConvertedVoiceTitle) + ".ogg";
            doc.attributes.add(name);
            if (thumbnail != null) { doc.thumbs.add(thumbnail); doc.flags |= 1; }
            if (kind == Kind.VIDEO_TO_ROUND) {
                TLRPC.TL_documentAttributeVideo video = secret ? new TLRPC.TL_documentAttributeVideo_layer159() : new TLRPC.TL_documentAttributeVideo();
                video.round_message = true; video.supports_streaming = true;
                video.w = video.h = MediaConversionPolicy.ROUND_SIDE; video.duration = duration;
                doc.attributes.add(video);
            } else {
                TLRPC.TL_documentAttributeAudio audio = new TLRPC.TL_documentAttributeAudio();
                audio.voice = kind == Kind.AUDIO_TO_VOICE; audio.duration = duration;
                if (audio.voice) {
                    audio.waveform = waveform == null ? null : waveform.clone();
                    if (audio.waveform != null && audio.waveform.length > 0) audio.flags |= 4;
                } else { audio.title = LocaleController.getString(R.string.BananaConvertedVoiceTitle); audio.flags |= 1; }
                doc.attributes.add(audio);
            }
            return doc;
        }
    }
    public static final class Job {
        public final AtomicBoolean cancelled = new AtomicBoolean();
        private volatile Process process;
        private volatile Result result;
        public void cancel() {
            cancelled.set(true); Process running = process;
            if (running != null) running.destroyForcibly();
            Result prepared = result; if (prepared != null) prepared.discard();
        }
    }
    /** Missing Telegram media is downloaded using its owning account before local conversion. */
    public static Job convertMessage(MessageObject source, Kind kind, double start, double end, Consumer<Result> done) {
        int account = source.currentAccount;
        long owner = UserConfig.getInstance(account).getClientUserId();
        File file = locate(source);
        if (!file.isFile()) FileLoader.getInstance(account).loadFile(source.getDocument(), source, FileLoader.PRIORITY_HIGH, 0);
        return submit(file, kind, start, end, () -> UserConfig.getInstance(account).getClientUserId() != owner, done);
    }
    public static Job convertFile(File file, Kind kind, double start, double end, Consumer<Result> done) {
        return submit(file, kind, start, end, () -> false, done);
    }
    private static Job submit(File input, Kind kind, double start, double end, java.util.function.BooleanSupplier ownerChanged, Consumer<Result> done) {
        Job job = new Job();
        if (!BUSY.compareAndSet(false, true)) { ApplicationLoader.applicationHandler.post(() -> done.accept(null)); return job; }
        QUEUE.postRunnable(() -> {
            File output = null; Result result = null; TLRPC.PhotoSize thumbnail = null;
            try {
                long deadline = SystemClock.elapsedRealtime() + 120000;
                while (!input.isFile() && SystemClock.elapsedRealtime() < deadline) {
                    check(job, ownerChanged); Thread.sleep(200);
                }
                check(job, ownerChanged);
                if (!input.isFile() || input.length() <= 0 || input.length() > MediaConversionPolicy.maxInputBytes(kind)) throw new IOException("Invalid source size");
                Metadata before = inspect(input);
                double expected = MediaConversionPolicy.outputDuration(kind, before.duration(kind), start, end);
                if (kind == Kind.VIDEO_TO_ROUND ? !before.video : !before.audio) throw new IOException("Required media track missing");
                output = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE), "banana-converted-" + UUID.randomUUID() + (kind == Kind.VIDEO_TO_ROUND ? ".mp4" : ".ogg"));
                if (kind == Kind.VOICE_TO_AUDIO) {
                    if (!"audio/opus".equals(before.audioMime)) throw new IOException("Voice is not Opus");
                    try (InputStream header = new FileInputStream(input)) {
                        if (header.read() != 'O' || header.read() != 'g' || header.read() != 'g' || header.read() != 'S') throw new IOException("Voice is not Ogg");
                    }
                    try (InputStream in = new FileInputStream(input); OutputStream out = new FileOutputStream(output)) {
                        byte[] buffer = new byte[65536]; int size;
                        while ((size = in.read(buffer)) != -1) { check(job, ownerChanged); out.write(buffer, 0, size); }
                    }
                } else execute(job, input, output, kind, before.duration(kind), start, end, ownerChanged);
                check(job, ownerChanged);
                if (!output.isFile() || output.length() <= 0 || output.length() > MediaConversionPolicy.MAX_OUTPUT_BYTES) throw new IOException("Invalid conversion output");
                Metadata after = inspect(output);
                if (Math.abs(after.duration(kind) - expected) > 0.5 || kind == Kind.VIDEO_TO_ROUND &&
                        (!"video/avc".equals(after.videoMime) || after.width != MediaConversionPolicy.ROUND_SIDE || after.height != MediaConversionPolicy.ROUND_SIDE) ||
                        kind == Kind.AUDIO_TO_VOICE && (!"audio/opus".equals(after.audioMime) || after.channels != 1 || after.rate != 48000)) throw new IOException("Incomplete conversion");
                byte[] waveform = kind == Kind.AUDIO_TO_VOICE ? MediaController.getWaveform(output.getAbsolutePath()) : null;
                check(job, ownerChanged);
                if (kind == Kind.VIDEO_TO_ROUND) {
                    android.graphics.Bitmap bitmap = SendMessagesHelper.createVideoThumbnailAtTime(output.getAbsolutePath(), 0);
                    if (bitmap != null) {
                        try { thumbnail = ImageLoader.scaleAndSaveImage(bitmap, 90, 90, 55, true); }
                        finally { bitmap.recycle(); }
                    }
                }
                check(job, ownerChanged);
                result = new Result(output, kind, kind == Kind.VIDEO_TO_ROUND ? Math.min(expected, after.duration(kind)) : after.duration(kind), waveform, thumbnail); job.result = result;
            } catch (Exception | LinkageError failure) {
                // Decoder/process errors may contain private source paths. Keep them out of exported logs.
                if (output != null) output.delete();
                discardThumbnail(thumbnail);
            } finally {
                Process leftover = job.process; if (leftover != null) leftover.destroyForcibly(); job.process = null;
                BUSY.set(false);
            }
            Result answer = result;
            AndroidUtilities.runOnUIThread(() -> {
                if (job.cancelled.get() || ownerChanged.getAsBoolean()) { if (answer != null) answer.discard(); done.accept(null); }
                else done.accept(answer);
            });
        });
        return job;
    }
    private static void discardThumbnail(TLRPC.PhotoSize thumbnail) {
        if (thumbnail != null && thumbnail.location != null && thumbnail.location.volume_id == Integer.MIN_VALUE) {
            new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE), thumbnail.location.volume_id + "_" + thumbnail.location.local_id + ".jpg").delete();
        }
    }
    private static File locate(MessageObject source) {
        String attached = source.messageOwner.attachPath;
        if (attached != null && !attached.isEmpty()) { File local = new File(attached); if (local.isFile()) return local; }
        return FileLoader.getInstance(source.currentAccount).getPathToMessage(source.messageOwner);
    }
    private static void check(Job job, java.util.function.BooleanSupplier ownerChanged) throws InterruptedException {
        if (job.cancelled.get() || ownerChanged.getAsBoolean() || Thread.currentThread().isInterrupted()) throw new InterruptedException();
    }
    private static void execute(Job job, File input, File output, Kind kind, double duration, double start, double end, java.util.function.BooleanSupplier ownerChanged) throws Exception {
        Context context = ApplicationLoader.applicationContext;
        FFmpeg.getInstance().init(context);
        List<String> command = new ArrayList<>();
        command.add(new File(context.getApplicationInfo().nativeLibraryDir, "libffmpeg.so").getAbsolutePath());
        command.addAll(MediaConversionPolicy.arguments(kind, input.getAbsolutePath(), output.getAbsolutePath(), duration, start, end));
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
        File libraries = new File(context.getNoBackupFilesDir(), "youtubedl-android/packages/ffmpeg/usr/lib");
        builder.environment().put("LD_LIBRARY_PATH", libraries.getAbsolutePath() + ":" + new File(context.getNoBackupFilesDir(), "youtubedl-android/packages/python/usr/lib").getAbsolutePath() + ":" + context.getApplicationInfo().nativeLibraryDir);
        builder.environment().put("TMPDIR", context.getCacheDir().getAbsolutePath());
        check(job, ownerChanged);
        Process process = builder.start(); job.process = process; process.getOutputStream().close();
        Thread drain = new Thread(() -> {
            try (InputStream stream = process.getInputStream()) { byte[] bytes = new byte[8192]; while (stream.read(bytes) != -1) { } }
            catch (IOException ignored) { }
        }, "bananaEncoderOutput");
        drain.setDaemon(true); drain.start();
        long deadline = SystemClock.elapsedRealtime() + 10 * 60 * 1000;
        try {
            while (!process.waitFor(200, TimeUnit.MILLISECONDS)) {
                check(job, ownerChanged);
                if (SystemClock.elapsedRealtime() > deadline || output.length() > MediaConversionPolicy.MAX_OUTPUT_BYTES) throw new IOException("Conversion budget exceeded");
            }
            if (process.exitValue() != 0) throw new IOException("Encoder failed");
        } finally {
            process.destroyForcibly(); job.process = null;
            try { process.getInputStream().close(); } catch (IOException ignored) { }
            drain.join(1000);
        }
    }
    private static final class Metadata {
        boolean audio, video; int channels, rate, width, height; double audioDuration, videoDuration;
        double duration(Kind kind) { return kind == Kind.VIDEO_TO_ROUND ? videoDuration : audioDuration; }
        String audioMime, videoMime;
    }
    private static Metadata inspect(File file) throws IOException {
        MediaExtractor extractor = new MediaExtractor(); Metadata metadata = new Metadata();
        try {
            extractor.setDataSource(file.getAbsolutePath());
            for (int n = 0; n < extractor.getTrackCount(); n++) {
                MediaFormat format = extractor.getTrackFormat(n); String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime == null) continue;
                double duration = format.containsKey(MediaFormat.KEY_DURATION) ? format.getLong(MediaFormat.KEY_DURATION) / 1000000.0 : 0;
                if (mime.startsWith("audio/") && !metadata.audio) {
                    metadata.audio = true; metadata.audioMime = mime; metadata.audioDuration = duration;
                    metadata.channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT); metadata.rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                } else if (mime.startsWith("video/") && !metadata.video) {
                    metadata.video = true; metadata.videoMime = mime; metadata.videoDuration = duration;
                    metadata.width = format.getInteger(MediaFormat.KEY_WIDTH); metadata.height = format.getInteger(MediaFormat.KEY_HEIGHT);
                }
            }
        } finally { extractor.release(); }
        return metadata;
    }
}
