package org.telegram.messenger.banana;

import org.junit.Test;
import static org.junit.Assert.*;

public class CrashReportFormatterTest {
    @Test public void excludesMessagesAndPathsButKeepsActionableFrames() {
        Throwable cause = new IllegalArgumentException("private session token");
        cause.setStackTrace(new StackTraceElement[]{new StackTraceElement("app.Decoder", "decode", "/private/session/file", 42)});
        Throwable error = new RuntimeException("private message", cause);
        error.addSuppressed(new RuntimeException("private suppressed"));
        String report = CrashReportFormatter.stack(error);
        assertTrue(report.contains("java.lang.IllegalArgumentException"));
        assertTrue(report.contains("app.Decoder.decode:42"));
        assertFalse(report.contains("private"));
        assertFalse(report.contains("token"));
    }

    @Test public void cyclicCausesTerminate() {
        Throwable a = new RuntimeException("secret a"), b = new RuntimeException("secret b");
        a.initCause(b); b.initCause(a);
        String report = CrashReportFormatter.stack(a);
        assertTrue(report.length() < 48 * 1024);
        assertFalse(report.contains("secret"));
    }
}
