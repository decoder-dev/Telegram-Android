package org.telegram.messenger.banana;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Process;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Java/Android regex semantics run outside the UI process, without app permissions. */
public final class IsolatedRegexService extends Service {
    private final LinkedHashMap<String, Pattern> patterns = new LinkedHashMap<>(64, .75f, true);
    private final IBananaRegex.Stub binder = new IBananaRegex.Stub() {
        @Override public int getWorkerUid() { return Process.myUid(); }

        @Override public int[] match(String expression, int flags, String[] inputs) {
            if (!RegexProtocol.valid(expression, inputs)) return new int[]{RegexProtocol.INPUT_LIMIT};
            try {
                Pattern pattern;
                synchronized (patterns) {
                    String key = flags + "\n" + expression;
                    pattern = patterns.get(key);
                    if (pattern == null) {
                        pattern = Pattern.compile(expression, flags);
                        patterns.put(key, pattern);
                        if (patterns.size() > 64) patterns.remove(patterns.keySet().iterator().next());
                    }
                }
                int[] result = new int[inputs.length + 1];
                result[0] = RegexProtocol.OK;
                for (int i = 0; i < inputs.length; i++) result[i + 1] = pattern.matcher(inputs[i]).find() ? 1 : 0;
                return result;
            } catch (PatternSyntaxException error) {
                return new int[]{RegexProtocol.BAD_PATTERN, error.getIndex()};
            } catch (RuntimeException error) {
                return new int[]{RegexProtocol.EVALUATION_ERROR};
            } catch (StackOverflowError | OutOfMemoryError error) {
                Process.killProcess(Process.myPid());
                return new int[]{RegexProtocol.EVALUATION_ERROR};
            }
        }

        @Override public void abort() { Process.killProcess(Process.myPid()); }
    };

    @Override public void onCreate() {
        super.onCreate();
        // Load ICU/compiler tables before publishing the Binder. The match watchdog
        // measures untrusted expressions, not first-use platform initialization.
        Pattern.compile("\\p{L}+", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                .matcher("Warmup").find();
    }
    @Override public IBinder onBind(Intent intent) { return binder; }
}
