package org.telegram.banana;

import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import org.junit.Test;
import org.telegram.messenger.banana.RegexEngine;
import org.telegram.messenger.banana.RegexProtocol;
import static org.junit.Assert.*;

/** Real Binder/ICU checks; synthetic strings only, no account or server access. */
public class RegexEngineTest {
    private RegexEngine engine() {
        return new RegexEngine(InstrumentationRegistry.getInstrumentation().getTargetContext());
    }
    private RegexEngine.Result match(RegexEngine engine, String pattern, int flags, String... text) throws Exception {
        AtomicReference<RegexEngine.Result> result = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        engine.match(pattern, flags, text, value -> {
            assertSame(Looper.getMainLooper(), Looper.myLooper());
            result.set(value); done.countDown();
        });
        assertTrue("Isolated regex must complete", done.await(8, TimeUnit.SECONDS));
        return result.get();
    }
    @Test public void fullRegexSyntaxRunsUnderDifferentUid() throws Exception {
        try (RegexEngine engine = engine()) {
            RegexEngine.Result result = match(engine, "(?<=code:) (\\p{L}+) \\1$", Pattern.CASE_INSENSITIVE,
                    "code: Привет привет", "code: hello world");
            assertEquals(RegexProtocol.OK, result.status);
            assertNotEquals("Worker must have no application permissions", Process.myUid(), result.workerUid);
            assertArrayEquals(new boolean[]{true, false}, result.matches);
            assertArrayEquals(new boolean[]{true}, match(engine, "^offer$", Pattern.MULTILINE, "hello\noffer\nend").matches);
        }
    }
    @Test public void malformedAndOversizedInputsFailWithoutKillingClient() throws Exception {
        try (RegexEngine engine = engine()) {
            assertEquals(RegexProtocol.BAD_PATTERN, match(engine, "[", 0, "input").status);
            assertEquals(RegexProtocol.INPUT_LIMIT, match(engine, "a", 0, new String(new char[RegexProtocol.MAX_INPUT + 1])).status);
            assertEquals(RegexProtocol.OK, match(engine, "a", 0, "a").status);
        }
    }
    @Test public void catastrophicPatternIsBoundedAndWorkerRecovers() throws Exception {
        try (RegexEngine engine = engine()) {
            assertEquals(RegexProtocol.OK, match(engine, "warm", 0, "warm").status);
            AtomicInteger beats = new AtomicInteger();
            Handler main = new Handler(Looper.getMainLooper());
            Runnable heartbeat = new Runnable() {
                @Override public void run() { beats.incrementAndGet(); main.postDelayed(this, 10); }
            };
            main.post(heartbeat);
            try {
                String input = new String(new char[100000]).replace('\0', 'a') + "!";
                long start = android.os.SystemClock.elapsedRealtime();
                RegexEngine.Result result = match(engine, "^(a+)+$", 0, input);
                assertTrue("Untrusted regex must not block the application", android.os.SystemClock.elapsedRealtime() - start < 2500);
                assertTrue("Catastrophic regex must not succeed", result.status == RegexProtocol.TIMEOUT ||
                        result.status == RegexProtocol.EVALUATION_ERROR || result.status == RegexProtocol.UNAVAILABLE);
                assertTrue("Main looper must keep running", beats.get() > 1);
                assertEquals(RegexProtocol.OK, match(engine, "recovered", 0, "recovered").status);
            } finally { main.removeCallbacks(heartbeat); }
        }
    }
    @Test public void closingDuringBindCompletesEveryQueuedRequestOnce() throws Exception {
        RegexEngine engine = engine();
        int count = 12;
        CountDownLatch done = new CountDownLatch(count);
        AtomicInteger callbacks = new AtomicInteger();
        for (int i = 0; i < count; i++) engine.match("a", 0, new String[]{"a"}, result -> { callbacks.incrementAndGet(); done.countDown(); });
        engine.close();
        assertTrue(done.await(8, TimeUnit.SECONDS));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        assertEquals(count, callbacks.get());
        assertEquals(RegexProtocol.UNAVAILABLE, match(engine, "a", 0, "a").status);
    }
    @Test public void oneClientCannotAbortAnotherClientsWorkerOnModernAndroid() throws Exception {
        if (android.os.Build.VERSION.SDK_INT < 29) return;
        try (RegexEngine first = engine(); RegexEngine second = engine()) {
            RegexEngine.Result a = match(first, "a", 0, "a"), b = match(second, "b", 0, "b");
            assertEquals(RegexProtocol.OK, a.status); assertEquals(RegexProtocol.OK, b.status);
            assertNotEquals(a.workerUid, b.workerUid);
            first.close();
            RegexEngine.Result stillAlive = match(second, "b", 0, "b");
            assertEquals(RegexProtocol.OK, stillAlive.status);
            assertEquals("Closing another editor must not restart this account worker", b.workerUid, stillAlive.workerUid);
        }
    }
}
