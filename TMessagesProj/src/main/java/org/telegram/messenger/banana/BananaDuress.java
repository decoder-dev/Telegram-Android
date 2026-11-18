package org.telegram.messenger.banana;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;

import java.security.SecureRandom;

/**
 * Duress passcode (ported from BananaGram iOS). Entered on the app lock screen it opens the app as
 * usual, but first hides the locked Archive, logs chosen accounts out, or erases every account from
 * the device. The real passcode always wins, and nothing on screen tells the two apart.
 */
public final class BananaDuress {
    public static final int RESULT_OK = 0, RESULT_SAME_AS_PASSCODE = 1, RESULT_WRONG_KIND = 2, RESULT_ARCHIVE_PASSWORD = 3, RESULT_FAILED = 4;
    private static final String PREFS = "bananaduress";
    private static final String KEY_VERIFIER = "verifier", KEY_TYPE = "type", KEY_ACTION = "action", KEY_IDS = "logoutIds";

    private BananaDuress() {}

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean hasPasscode() {
        return SharedConfig.passcodeHash != null && SharedConfig.passcodeHash.length() > 0;
    }

    public static boolean isSet() {
        return hasPasscode() && prefs().getString(KEY_VERIFIER, null) != null;
    }

    /** The passcode changed between PIN and password since the duress code was set: it can't be entered. */
    public static boolean kindChanged() {
        SharedPreferences prefs = prefs();
        return prefs.getString(KEY_VERIFIER, null) != null && prefs.getInt(KEY_TYPE, -1) != SharedConfig.passcodeType;
    }

    public static int action() {
        return DuressVerifier.clampAction(prefs().getInt(KEY_ACTION, DuressVerifier.ACTION_HIDE));
    }

    public static void setAction(int action) {
        prefs().edit().putInt(KEY_ACTION, DuressVerifier.clampAction(action)).apply();
    }

    public static long[] logoutIds() {
        return DuressVerifier.parseIds(prefs().getString(KEY_IDS, ""));
    }

    public static void setLogoutIds(long[] ids) {
        prefs().edit().putString(KEY_IDS, DuressVerifier.formatIds(ids)).apply();
    }

    /** Stores a new duress code. Reached only after the real passcode was confirmed on its settings screen. */
    public static int store(String code) {
        boolean pin = SharedConfig.passcodeType == SharedConfig.PASSCODE_TYPE_PIN;
        if (!hasPasscode() || !DuressVerifier.fitsType(code, pin)) return RESULT_WRONG_KIND;
        if (SharedConfig.checkPasscode(code)) return RESULT_SAME_AS_PASSCODE;
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (UserConfig.getInstance(a).isClientActivated() && BananaArchiveLock.matchesPassword(a, code)) {
                return RESULT_ARCHIVE_PASSWORD;
            }
        }
        String verifier = DuressVerifier.make(code, new SecureRandom());
        if (verifier == null) return RESULT_FAILED;
        prefs().edit().putString(KEY_VERIFIER, verifier).putInt(KEY_TYPE, SharedConfig.passcodeType).commit();
        return RESULT_OK;
    }

    public static void clear() {
        if (ApplicationLoader.applicationContext == null) return;
        prefs().edit().clear().apply();
    }

    /** Checked only after the real passcode failed. A missing code costs the same time as a wrong one. */
    public static boolean matches(String code) {
        SharedPreferences prefs = prefs();
        String verifier = prefs.getString(KEY_VERIFIER, null);
        if (verifier == null || prefs.getInt(KEY_TYPE, -1) != SharedConfig.passcodeType || !hasPasscode()) {
            DuressVerifier.dummy(code);
            return false;
        }
        return DuressVerifier.matches(code, verifier);
    }

    /** Runs the chosen action. Called on the UI thread just before the lock screen opens the app. */
    public static void perform() {
        BananaArchiveLock.blockReveal();
        BananaArchiveLock.relockEverything();
        int action = action();
        if (action == DuressVerifier.ACTION_HIDE) return;
        long[] slotIds = new long[UserConfig.MAX_ACCOUNT_COUNT];
        for (int a = 0; a < slotIds.length; a++) {
            UserConfig config = UserConfig.getInstance(a);
            slotIds[a] = config.isClientActivated() ? config.getClientUserId() : 0;
        }
        int[] slots = DuressVerifier.slotsToLogOut(action, logoutIds(), slotIds);
        boolean erase = action == DuressVerifier.ACTION_ERASE;
        for (int slot : slots) {
            if (erase) {
                // Before logout clears the owner: the saved history of this account goes too.
                MessageHistory.clear(slot, ok -> { });
            }
            // 1 ends the session on the server; 2 removes it from this device only, without a request.
            MessagesController.getInstance(slot).performLogout(erase ? 2 : 1);
        }
        if (erase) {
            Utilities.globalQueue.postRunnable(() -> {
                try {
                    com.radolyn.ayugram.messages.AyuMessagesController.getInstance().clean();
                } catch (Throwable ignored) {
                    // The rest of the erase must still run.
                }
                int[] types = {FileLoader.MEDIA_DIR_IMAGE, FileLoader.MEDIA_DIR_AUDIO, FileLoader.MEDIA_DIR_VIDEO,
                        FileLoader.MEDIA_DIR_DOCUMENT, FileLoader.MEDIA_DIR_CACHE, FileLoader.MEDIA_DIR_FILES, FileLoader.MEDIA_DIR_STORIES};
                for (int type : types) {
                    try {
                        java.io.File dir = FileLoader.checkDirectory(type);
                        if (dir != null) org.telegram.ui.CacheControlActivity.cleanDirJava(dir.getAbsolutePath(), 0, null, progress -> { });
                    } catch (Throwable ignored) {
                        // Keep erasing the other folders.
                    }
                }
            });
        }
    }
}
