package org.telegram.messenger.banana;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.FilterInputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

/** Read-only public sources from AyuGram's allowlist. No Telegram credentials or peer resolving. */
public final class RemoteFilterSource {
    public static final int MAX_URL = 4096;
    private static final String PREFIX = "tg://ayu/filters/import/";
    private static final Set<String> HOSTS = new HashSet<>(Arrays.asList("dpaste.com", "gist.githubusercontent.com", "pastebin.com", "nekobin.com"));
    private static final ThreadPoolExecutor workers = new ThreadPoolExecutor(2, 2, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(4), runnable -> new Thread(runnable, "banana-filter-source"));
    private static final java.util.concurrent.ScheduledExecutorService deadlines = Executors.newSingleThreadScheduledExecutor(
            runnable -> new Thread(runnable, "banana-filter-source-deadline"));
    static { workers.allowCoreThreadTimeOut(true); }
    private RemoteFilterSource() {}

    public static boolean isDeepLink(String link) { return link != null && link.startsWith(PREFIX); }
    public static URI url(String source) {
        if (source == null || source.length() > MAX_URL) throw new IllegalArgumentException("Invalid filter source");
        String text = source.trim();
        if (isDeepLink(text)) text = text.substring(PREFIX.length());
        try {
            URI uri = new URI(text);
            if (uri.getScheme() == null) uri = new URI("https://" + text);
            return checked(uri);
        }
        catch (java.net.URISyntaxException error) { throw new IllegalArgumentException("Invalid filter source"); }
    }
    private static URI checked(URI uri) {
        if (uri == null || uri.toString().length() > MAX_URL || !"https".equalsIgnoreCase(uri.getScheme()) ||
                uri.getHost() == null || !HOSTS.contains(uri.getHost().toLowerCase(Locale.ROOT)) ||
                uri.getRawUserInfo() != null || uri.getFragment() != null || uri.getPort() != -1 && uri.getPort() != 443) {
            throw new IllegalArgumentException("Unsupported filter source");
        }
        return uri;
    }
    public static URI redirect(URI from, String location) {
        if (location == null || location.length() > MAX_URL) throw new IllegalArgumentException("Invalid filter redirect");
        try { return checked(checked(from).resolve(new URI(location))); }
        catch (java.net.URISyntaxException error) { throw new IllegalArgumentException("Invalid filter redirect"); }
    }
    public static final class Result {
        public final String json;
        public final URI source;
        Result(String json, URI source) { this.json = json; this.source = source; }
    }
    public static final class Task {
        private final AtomicBoolean finished = new AtomicBoolean();
        private final Consumer<Result> callback;
        private volatile Future<?> work;
        private volatile ScheduledFuture<?> timeout;
        Task(Consumer<Result> callback) { this.callback = callback; }
        public void cancel() {
            finished.set(true);
            if (work != null) work.cancel(true);
            if (timeout != null) timeout.cancel(false);
        }
        private void complete(Result result) {
            if (finished.compareAndSet(false, true)) {
                if (timeout != null) timeout.cancel(false);
                callback.accept(result);
            }
        }
    }
    /** Callback runs off the UI thread. Cancelling never waits for socket/DNS cleanup on the UI. */
    public static Task fetch(URI source, Consumer<Result> callback) {
        URI target = checked(source);
        Task task = new Task(callback);
        task.timeout = deadlines.schedule(() -> {
            task.complete(new Result(null, target));
            if (task.work != null) task.work.cancel(true);
        }, 20, TimeUnit.SECONDS);
        try {
            task.work = workers.submit(() -> {
                URI current = target;
                try {
                    for (int hop = 0; hop < 5 && !task.finished.get(); hop++) {
                        HttpURLConnection connection = (HttpURLConnection) current.toURL().openConnection();
                        try {
                            connection.setInstanceFollowRedirects(false);
                            connection.setConnectTimeout(5000); connection.setReadTimeout(5000);
                            connection.setUseCaches(false);
                            connection.setRequestProperty("Accept", "application/json, text/plain");
                            connection.setRequestProperty("Accept-Encoding", "gzip");
                            connection.setRequestProperty("User-Agent", "BananaGram");
                            int code = connection.getResponseCode();
                            if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                                current = redirect(current, connection.getHeaderField("Location"));
                                continue;
                            }
                            if (code != 200) throw new IOException("Filter source unavailable");
                            if (connection.getContentLength() > FilterRules.MAX_JSON) throw new IOException("Filter source too large");
                            try (InputStream raw = connection.getInputStream()) {
                                InputStream wire = bounded(raw, task.finished);
                                InputStream input = "gzip".equalsIgnoreCase(connection.getContentEncoding()) ? new GZIPInputStream(wire) : wire;
                                try {
                                    String json = read(input, task.finished);
                                    task.complete(new Result(json, current));
                                    return;
                                } finally { if (input != raw) input.close(); }
                            }
                        } finally { connection.disconnect(); }
                    }
                } catch (Exception ignored) {
                    // Neither URLs, imported private rules nor server response bodies enter app logs.
                }
                task.complete(new Result(null, current));
            });
        } catch (RejectedExecutionException error) { deadlines.execute(() -> task.complete(new Result(null, target))); }
        return task;
    }
    public static String read(InputStream input) throws IOException { return read(input, new AtomicBoolean()); }
    static InputStream bounded(InputStream input) { return bounded(input, new AtomicBoolean()); }
    private static InputStream bounded(InputStream input, AtomicBoolean cancelled) {
        return new FilterInputStream(input) {
            private int used;
            private void check() throws IOException {
                if (used > FilterRules.MAX_JSON) throw new IOException("Filter source too large");
                if (cancelled.get() || Thread.currentThread().isInterrupted()) throw new IOException("Filter read cancelled");
            }
            private int counted(int size) throws IOException {
                if (size > 0) { used += size; check(); }
                return size;
            }
            @Override public int read() throws IOException {
                check();
                int value = in.read();
                if (value != -1) counted(1);
                return value;
            }
            @Override public int read(byte[] data, int offset, int length) throws IOException {
                check();
                return counted(in.read(data, offset, Math.min(length, FilterRules.MAX_JSON - used + 1)));
            }
            @Override public long skip(long size) throws IOException {
                long skipped = 0;
                byte[] buffer = new byte[4096];
                while (skipped < size) {
                    int count = read(buffer, 0, (int) Math.min(buffer.length, size - skipped));
                    if (count == -1) break;
                    skipped += count;
                }
                return skipped;
            }
        };
    }
    private static String read(InputStream input, AtomicBoolean cancelled) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        for (int size; (size = input.read(buffer)) != -1;) {
            if (cancelled.get() || Thread.currentThread().isInterrupted()) throw new IOException("Filter read cancelled");
            if (size > FilterRules.MAX_JSON - bytes.size()) throw new IOException("Filter source too large");
            bytes.write(buffer, 0, size);
        }
        byte[] data = bytes.toByteArray();
        int offset = data.length >= 3 && (data[0] & 255) == 0xef && (data[1] & 255) == 0xbb && (data[2] & 255) == 0xbf ? 3 : 0;
        return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(data, offset, data.length - offset)).toString();
    }
}
