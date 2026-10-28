package org.telegram.messenger.banana;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.Build;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Bounded asynchronous bridge. A slow regex terminates its isolated worker, never the client. */
public final class RegexEngine implements AutoCloseable {
    public static final long MATCH_TIMEOUT_MS = 250;
    // Connecting a worker must not wait for a busy/rebuilding UI to dispatch its callback.
    private static final ThreadPoolExecutor BIND_CALLBACKS = new ThreadPoolExecutor(0, 1, 15, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(128), runnable -> {
                Thread thread = new Thread(runnable, "banana-regex-bind"); thread.setDaemon(true); return thread;
            }, new ThreadPoolExecutor.CallerRunsPolicy());
    public static final class Result {
        public final int status, workerUid, errorIndex;
        public final boolean[] matches;
        Result(int status, int uid, int index, boolean[] matches) {
            this.status = status; workerUid = uid; errorIndex = index; this.matches = matches;
        }
    }

    private final Context context;
    private final String instanceName = "banana" + java.util.UUID.randomUUID().toString().replace("-", "");
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ThreadPoolExecutor worker = new ThreadPoolExecutor(1, 1, 15, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(64), runnable -> new Thread(runnable, "banana-regex-client"));
    private final ScheduledExecutorService deadlines = Executors.newSingleThreadScheduledExecutor(runnable -> new Thread(runnable, "banana-regex-deadline"));
    private volatile Connection connection;
    private volatile boolean closed;
    private final Set<Request> pending = ConcurrentHashMap.newKeySet();
    private ScheduledFuture<?> idleRelease;

    private synchronized void releaseWhenIdle() {
        if (closed || !pending.isEmpty()) return;
        if (idleRelease != null) idleRelease.cancel(false);
        try {
            idleRelease = deadlines.schedule(() -> {
                synchronized (RegexEngine.this) {
                    if (!pending.isEmpty()) return;
                    Connection link = connection;
                    if (link != null) discard(link);
                }
            }, 30, TimeUnit.SECONDS);
        } catch (RejectedExecutionException ignored) { }
    }

    private final class Request implements Consumer<Result> {
        final Consumer<Result> callback;
        final AtomicBoolean delivered = new AtomicBoolean();
        Request(Consumer<Result> callback) { this.callback = callback; }
        @Override public void accept(Result result) {
            if (delivered.compareAndSet(false, true)) {
                pending.remove(this);
                try { callback.accept(result); } finally { releaseWhenIdle(); }
            }
        }
    }

    public RegexEngine(Context context) {
        this.context = context.getApplicationContext();
        worker.allowCoreThreadTimeOut(true);
    }

    public void match(String expression, int flags, String[] inputs, Consumer<Result> callback) {
        if (!RegexProtocol.valid(expression, inputs)) { deliver(callback, result(RegexProtocol.INPUT_LIMIT)); return; }
        Request request = new Request(callback);
        synchronized (this) {
            if (closed) { deliver(request, result(RegexProtocol.UNAVAILABLE)); return; }
            if (idleRelease != null) idleRelease.cancel(false);
            pending.add(request);
        }
        String[] snapshot = inputs.clone();
        try {
            worker.execute(() -> evaluate(expression, flags, snapshot, request));
        } catch (RejectedExecutionException error) { deliver(request, result(RegexProtocol.BUSY)); }
    }

    private void evaluate(String expression, int flags, String[] inputs, Consumer<Result> callback) {
        Connection connected = null;
        for (int attempt = 0; attempt < 2 && !closed && connected == null; attempt++) {
            try { connected = connect(); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); break; }
            catch (Exception unavailable) { /* Retry a fresh binding once, not the expression itself. */ }
        }
        if (connected == null || closed) { deliver(callback, result(RegexProtocol.UNAVAILABLE)); return; }
        final Connection link = connected;
        IBananaRegex remote = link.remote;
        if (remote == null) { discard(link); deliver(callback, result(RegexProtocol.UNAVAILABLE)); return; }
        int uid;
        try {
            uid = remote.getWorkerUid();
            // In-process or non-isolated misconfiguration must never reach abort().
            if (uid == Process.myUid()) { discard(link); deliver(callback, result(RegexProtocol.UNAVAILABLE)); return; }
            link.verifiedIsolated = true;
        } catch (Exception error) { discard(link); deliver(callback, result(RegexProtocol.UNAVAILABLE)); return; }
        final int workerUid = uid;
        AtomicBoolean completed = new AtomicBoolean();
        ScheduledFuture<?> timeout;
        try {
            timeout = deadlines.schedule(() -> {
                if (completed.compareAndSet(false, true)) {
                    deliver(callback, new Result(RegexProtocol.TIMEOUT, workerUid, -1, null));
                    try { remote.abort(); } catch (Exception ignored) { }
                    discard(link);
                }
            }, MATCH_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException error) { deliver(callback, result(RegexProtocol.UNAVAILABLE)); return; }
        try {
            int[] answer = remote.match(expression, flags, inputs);
            if (completed.compareAndSet(false, true)) {
                if (answer == null || answer.length == 0 || answer[0] == RegexProtocol.OK && answer.length != inputs.length + 1) {
                    deliver(callback, result(RegexProtocol.UNAVAILABLE));
                } else if (answer[0] != RegexProtocol.OK) {
                    deliver(callback, new Result(answer[0], workerUid, answer.length > 1 ? answer[1] : -1, null));
                } else {
                    boolean[] matches = new boolean[inputs.length];
                    for (int i = 0; i < matches.length; i++) matches[i] = answer[i + 1] == 1;
                    deliver(callback, new Result(RegexProtocol.OK, workerUid, -1, matches));
                }
            }
        } catch (Exception error) {
            discard(link);
            if (completed.compareAndSet(false, true)) deliver(callback, result(RegexProtocol.UNAVAILABLE));
        } finally { timeout.cancel(false); }
    }

    private Connection connect() throws Exception {
        Connection active = connection;
        if (active != null && active.remote != null && active.remote.asBinder().isBinderAlive()) return active;
        if (active != null) discard(active);
        if (closed) return null;
        Connection created = new Connection();
        synchronized (this) {
            if (closed) return null;
            connection = created;
        }
        boolean keep = false;
        try {
            Intent intent = new Intent(context, IsolatedRegexService.class);
            boolean bound = Build.VERSION.SDK_INT >= 29 ? context.bindIsolatedService(intent, Context.BIND_AUTO_CREATE,
                    instanceName, BIND_CALLBACKS, created) : context.bindService(intent, created, Context.BIND_AUTO_CREATE);
            if (!bound) return null;
            if (!created.ready.await(5, TimeUnit.SECONDS) || created.remote == null || closed) return null;
            keep = true;
            return created;
        } finally { if (!keep) discard(created); }
    }

    private synchronized void discard(Connection link) {
        if (connection == link) connection = null;
        try { context.unbindService(link); } catch (IllegalArgumentException ignored) { }
    }

    private final class Connection implements ServiceConnection {
        final CountDownLatch ready = new CountDownLatch(1);
        volatile IBananaRegex remote;
        volatile boolean verifiedIsolated;
        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            remote = IBananaRegex.Stub.asInterface(binder); ready.countDown();
        }
        @Override public void onServiceDisconnected(ComponentName name) { remote = null; ready.countDown(); }
        @Override public void onNullBinding(ComponentName name) { ready.countDown(); }
        @Override public void onBindingDied(ComponentName name) { remote = null; ready.countDown(); }
    }

    private Result result(int status) { return new Result(status, -1, -1, null); }
    private void deliver(Consumer<Result> callback, Result result) { main.post(() -> callback.accept(result)); }

    @Override public void close() {
        Connection active;
        synchronized (this) {
            closed = true;
            active = connection;
        }
        if (active != null) {
            IBananaRegex remote = active.remote;
            if (active.verifiedIsolated && remote != null) try { remote.abort(); } catch (Exception ignored) { }
            discard(active);
        }
        worker.shutdownNow();
        deadlines.shutdownNow();
        for (Request request : pending) deliver(request, result(RegexProtocol.UNAVAILABLE));
    }
}
