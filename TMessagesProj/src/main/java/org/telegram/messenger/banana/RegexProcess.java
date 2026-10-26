package org.telegram.messenger.banana;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

/** Must remain independent of Telegram initialization and private app storage. */
public final class RegexProcess {
    public static final String SUFFIX = ":banana_regex";
    private RegexProcess() {}

    public static boolean isWorker(Context context) {
        String name = null;
        if (Build.VERSION.SDK_INT >= 28) name = Application.getProcessName();
        else {
            try (FileInputStream input = new FileInputStream("/proc/self/cmdline")) {
                byte[] data = new byte[512];
                int size = input.read(data), end = 0;
                while (end < size && data[end] != 0) end++;
                name = new String(data, 0, end, StandardCharsets.UTF_8);
            } catch (Exception ignored) { }
        }
        if (context == null || name == null) return false;
        String prefix = context.getPackageName() + SUFFIX;
        // New Android versions append the isolated service's class to its process name.
        return name.equals(prefix) || name.startsWith(prefix + ":");
    }
}
