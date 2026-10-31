package org.telegram.messenger.banana;

import android.app.Activity;
import android.content.Context;
import android.os.Debug;
import android.util.AtomicFile;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.TreeMap;

/** One bounded, app-private crash report, copied only by an explicit user action. */
public final class BananaCrashReporter {
    private static final ArrayList<Screen> screens = new ArrayList<>();
    private static boolean installed, shown;
    private static final class Screen {
        final WeakReference<Object> ref;
        final String name;
        boolean active;
        Screen(Object screen) { ref = new WeakReference<>(screen); name = screen.getClass().getSimpleName(); }
    }
    private BananaCrashReporter() { }
    private static AtomicFile file(Context context) {
        return new AtomicFile(new File(context.getNoBackupFilesDir(), "banana-last-crash.txt"));
    }
    public static synchronized void track(Object screen) {
        screens.removeIf(item -> item.ref.get() == null);
        // Diagnostic bookkeeping must stay bounded even if the application leaks screens.
        if (screens.size() < 2048) screens.add(new Screen(screen));
    }
    public static synchronized void active(Object screen, boolean active) {
        for (Screen item : screens) if (item.ref.get() == screen) { item.active = active; break; }
    }
    static synchronized String memory() {
        TreeMap<String, Integer> alive = new TreeMap<>(), active = new TreeMap<>();
        for (Screen item : screens) if (item.ref.get() != null) {
            alive.put(item.name, alive.getOrDefault(item.name, 0) + 1);
            if (item.active) active.put(item.name, active.getOrDefault(item.name, 0) + 1);
        }
        Runtime runtime = Runtime.getRuntime();
        return "Java bytes: " + (runtime.totalMemory() - runtime.freeMemory()) + "/" + runtime.maxMemory()
                + "\nNative bytes: " + Debug.getNativeHeapAllocatedSize()
                + "\nScreens alive (sample, max 2048): " + alive + "\nScreens not destroyed (sample): " + active + "\n";
    }
    public static synchronized void install(Context context) {
        if (installed || context == null) return;
        installed = true;
        Context app = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            AtomicFile target = null;
            FileOutputStream output = null;
            try {
                target = file(app);
                String report = "BananaGram " + BuildVars.BUILD_VERSION_STRING + "\nAndroid API "
                        + android.os.Build.VERSION.SDK_INT + "\n" + CrashReportFormatter.stack(error);
                // Preserve the stack even when OOM prevents the optional memory snapshot.
                try { report += memory(); } catch (Throwable ignored) { }
                byte[] bytes = report.getBytes(StandardCharsets.UTF_8);
                output = target.startWrite();
                output.write(bytes, 0, Math.min(bytes.length, 64 * 1024));
                target.finishWrite(output);
            } catch (Throwable ignored) {
                try { if (target != null) target.failWrite(output); } catch (Throwable ignoredAgain) { }
            } finally {
                if (previous != null) previous.uncaughtException(thread, error);
                else { android.os.Process.killProcess(android.os.Process.myPid()); System.exit(10); }
            }
        });
    }
    public static void showIfNeeded(Activity activity) {
        if (shown || activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        AtomicFile target = file(activity);
        if (!target.getBaseFile().isFile()) return;
        final String report;
        try {
            if (target.getBaseFile().length() > 64 * 1024) { target.delete(); return; }
            report = new String(target.readFully(), StandardCharsets.UTF_8);
        } catch (Exception unavailable) { return; }
        shown = true;
        new AlertDialog.Builder(activity)
                .setTitle(LocaleController.getString(R.string.BananaCrashTitle))
                .setMessage(LocaleController.getString(R.string.BananaCrashInfo))
                .setPositiveButton(LocaleController.getString(R.string.Copy), (dialog, which) -> {
                    AndroidUtilities.addToClipboard(report);
                    target.delete();
                })
                .setNegativeButton(LocaleController.getString(R.string.Delete), (dialog, which) -> target.delete())
                .show();
    }
}
