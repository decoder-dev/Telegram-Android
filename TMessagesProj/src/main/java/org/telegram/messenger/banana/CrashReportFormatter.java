package org.telegram.messenger.banana;

/** No exception messages, file paths, thread names or suppressed exceptions enter the report. */
public final class CrashReportFormatter {
    private CrashReportFormatter() { }

    public static String stack(Throwable error) {
        StringBuilder out = new StringBuilder();
        java.util.Set<Throwable> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (int cause = 0; error != null && cause < 8 && seen.add(error); cause++, error = error.getCause()) {
            out.append(cause == 0 ? "Exception: " : "Caused by: ").append(error.getClass().getName()).append('\n');
            StackTraceElement[] frames = error.getStackTrace();
            for (int i = 0; i < Math.min(frames.length, 64); i++) {
                StackTraceElement frame = frames[i];
                out.append("  at ").append(frame.getClassName()).append('.').append(frame.getMethodName())
                        .append(':').append(frame.getLineNumber()).append('\n');
                if (out.length() > 48 * 1024) return out.substring(0, 48 * 1024);
            }
        }
        return out.toString();
    }
}
