package org.telegram.messenger.banana;

import android.os.Build;
import android.os.SystemClock;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.text.InputType;
import android.util.Base64;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.INavigationLayout;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.ProfileActivity;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Password lock for Archive and Saved Messages, following BananaGram iOS
 * ArchiveLockHelpers / ArchiveSettingsController: hidden until Settings is tapped
 * ten times, then a password or biometric gate. The session closes when the app
 * backgrounds. Existing archive toggles are left alone.
 */
public final class BananaArchiveLock {
    private static final int ITERATIONS = 100_000;
    private static final int REVEAL_TAPS = 10;
    private static final long TAP_GAP_MS = 3000L;
    private static final Object FILE_LOCK = new Object();
    private static final int ACCOUNTS = UserConfig.MAX_ACCOUNT_COUNT;

    private static final boolean[] unlocked = new boolean[ACCOUNTS];
    private static final boolean[] revealed = new boolean[ACCOUNTS];
    private static final int[] taps = new int[ACCOUNTS];
    private static final long[] lastTap = new long[ACCOUNTS];
    private static int biometricDepth;
    private static boolean promptOpen;
    private static boolean closingSurfaces;
    /** Keeps the recents snapshot blank after a background relock, until the next resume. */
    private static boolean secureLatch;

    private BananaArchiveLock() {}

    public static boolean isConfigured(int account) {
        return state(account).verifier != null;
    }

    public static boolean isSessionOpen(int account) {
        return account >= 0 && account < ACCOUNTS && unlocked[account];
    }

    public static boolean isBiometricsEnabled(int account) {
        return state(account).biometrics && isConfigured(account);
    }

    /**
     * Revealing the folder does not unlock it. While the password is set and the session
     * is locked, Archive and Saved Messages stay title-only: no peer names, previews, or badges.
     */
    public static boolean redactsPreview(int account, long dialogId, int folderId) {
        if (!isConfigured(account) || isSessionOpen(account)) {
            return false;
        }
        if (folderId == 1 || dialogId == DialogObject.makeFolderDialogId(1)) {
            return true;
        }
        return isProtectedDialog(account, dialogId);
    }

    /** Blank the recents snapshot while Archive contents are on screen, including across onPause. */
    public static boolean shouldSecureWindow(int account) {
        if (secureLatch) {
            return anyConfigured();
        }
        return isConfigured(account) && isSessionOpen(account);
    }

    public static boolean isProtected(int account, long dialogId) {
        return isProtectedDialog(account, dialogId);
    }

    /** Hide Archive and Saved Messages rows until the ten-tap reveal or an unlock. */
    public static boolean hidesSurfaces(int account) {
        return isConfigured(account) && !isSessionOpen(account) && !(account >= 0 && account < ACCOUNTS && revealed[account]);
    }

    public static boolean blocksArchiveFolder(int account) {
        return isConfigured(account) && !isSessionOpen(account);
    }

    public static boolean blocksOpening(int account, long dialogId) {
        if (!isConfigured(account) || isSessionOpen(account)) {
            return false;
        }
        return isProtectedDialog(account, dialogId);
    }

    public static boolean shouldRedactNotification(int account, long dialogId) {
        return isConfigured(account) && isProtectedDialog(account, dialogId);
    }

    public static boolean hidesObject(int account, Object obj) {
        if (!hidesSurfaces(account) || obj == null) {
            return false;
        }
        long id = 0;
        if (obj instanceof TLRPC.User) {
            id = ((TLRPC.User) obj).id;
        } else if (obj instanceof TLRPC.Chat) {
            id = -((TLRPC.Chat) obj).id;
        } else if (obj instanceof TLRPC.Dialog) {
            TLRPC.Dialog dialog = (TLRPC.Dialog) obj;
            if (dialog.folder_id == 1) {
                return true;
            }
            id = dialog.id;
        } else if (obj instanceof TLRPC.EncryptedChat) {
            id = DialogObject.makeEncryptedDialogId(((TLRPC.EncryptedChat) obj).id);
        } else if (obj instanceof org.telegram.messenger.MessageObject) {
            id = ((org.telegram.messenger.MessageObject) obj).getDialogId();
        } else {
            return false;
        }
        return isProtectedDialog(account, id);
    }

    public static ArrayList<TLRPC.Dialog> filterDialogList(int account, int folderId, ArrayList<TLRPC.Dialog> dialogs) {
        if (dialogs == null) {
            return null;
        }
        if (isConfigured(account) && !isSessionOpen(account) && folderId == 1) {
            return new ArrayList<>();
        }
        if (!hidesSurfaces(account) || dialogs.isEmpty()) {
            return dialogs;
        }
        long selfId = selfId(account);
        long archiveId = DialogObject.makeFolderDialogId(1);
        ArrayList<TLRPC.Dialog> filtered = null;
        for (int i = 0; i < dialogs.size(); i++) {
            TLRPC.Dialog dialog = dialogs.get(i);
            boolean hide = false;
            if (dialog != null && !(dialog instanceof DialogsActivity.DialogsHeader)) {
                if ((selfId != 0 && dialog.id == selfId) || dialog.id == archiveId || dialog.folder_id == 1) {
                    hide = true;
                }
            }
            if (hide) {
                if (filtered == null) {
                    filtered = new ArrayList<>(dialogs.size());
                    for (int j = 0; j < i; j++) {
                        filtered.add(dialogs.get(j));
                    }
                }
            } else if (filtered != null) {
                filtered.add(dialog);
            }
        }
        return filtered != null ? filtered : dialogs;
    }

    /** Counts taps on the Settings tab or Settings row. Returns true when access is revealed. */
    public static boolean noteSettingsTap(int account) {
        if (!isConfigured(account) || isSessionOpen(account) || account < 0 || account >= ACCOUNTS) {
            return false;
        }
        if (revealed[account]) {
            return false;
        }
        long now = SystemClock.elapsedRealtime();
        if (now - lastTap[account] > TAP_GAP_MS) {
            taps[account] = 0;
        }
        lastTap[account] = now;
        taps[account]++;
        if (taps[account] < REVEAL_TAPS) {
            return false;
        }
        taps[account] = 0;
        revealed[account] = true;
        NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.dialogsNeedReload);
        return true;
    }

    public static void onHostPause() {
        if (biometricDepth > 0) {
            return;
        }
        if (anySessionOpen()) {
            secureLatch = true;
        }
        closeOpenSurfaces();
        relockAll();
        notifySecureChanged();
    }

    public static void onHostResume() {
        secureLatch = false;
        notifySecureChanged();
    }

    public static void lockNow(int account) {
        if (account < 0 || account >= ACCOUNTS) {
            return;
        }
        unlocked[account] = false;
        revealed[account] = false;
        taps[account] = 0;
        closeOpenSurfaces();
        NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.dialogsNeedReload);
        notifySecureChanged();
        try {
            NotificationsController.getInstance(account).showNotifications();
        } catch (Throwable ignore) {
        }
    }

    /**
     * Relock when the last Archive folder, archived chat, Saved Messages, or archived profile leaves the stack.
     * Opening a chat from inside the Archive does not relock until that folder is gone too.
     */
    public static void noteSurfaceClosed(int account, INavigationLayout layout, BaseFragment leaving) {
        if (closingSurfaces || !isConfigured(account) || !isSessionOpen(account)) {
            return;
        }
        if (layout != null) {
            List<BaseFragment> stack;
            try {
                stack = layout.getFragmentStack();
            } catch (Throwable t) {
                stack = null;
            }
            if (stack != null) {
                for (int i = 0; i < stack.size(); i++) {
                    BaseFragment fragment = stack.get(i);
                    if (fragment == null || fragment == leaving) {
                        continue;
                    }
                    if (isProtectedSurface(fragment)) {
                        return;
                    }
                }
            }
        }
        lockNow(account);
    }

    public static void promptUnlock(BaseFragment fragment, Runnable onSuccess) {
        if (fragment == null) {
            return;
        }
        int account = fragment.getCurrentAccount();
        if (!isConfigured(account) || isSessionOpen(account)) {
            if (onSuccess != null) {
                onSuccess.run();
            }
            return;
        }
        if (isBiometricsEnabled(account) && fragment.getParentActivity() instanceof FragmentActivity) {
            authenticateBiometric(fragment, () -> unlock(account, onSuccess), () -> askUnlockPassword(fragment, onSuccess));
            return;
        }
        askUnlockPassword(fragment, onSuccess);
    }

    public static void promptSet(BaseFragment fragment, Runnable onDone) {
        ask(fragment, LocaleController.getString(R.string.BananaArchiveSetTitle), LocaleController.getString(R.string.BananaArchiveSetText), LocaleController.getString(R.string.BananaArchiveContinue), first -> {
            if (first == null) {
                return;
            }
            ask(fragment, LocaleController.getString(R.string.BananaArchiveConfirmTitle), LocaleController.getString(R.string.BananaArchiveConfirmText), LocaleController.getString(R.string.BananaArchiveContinue), second -> {
                if (second == null) {
                    return;
                }
                if (!first.equals(second)) {
                    showMessage(fragment, LocaleController.getString(R.string.BananaArchiveMismatch));
                    return;
                }
                int account = fragment.getCurrentAccount();
                Utilities.globalQueue.postRunnable(() -> {
                    boolean ok = storePassword(account, first);
                    AndroidUtilities.runOnUIThread(() -> {
                        if (!ok) {
                            showMessage(fragment, LocaleController.getString(R.string.BananaArchiveStorageError));
                            return;
                        }
                        muteArchived(account);
                        alignKeepArchived(account);
                        clearProtectedNotifications(account);
                        unlock(account, null);
                        if (onDone != null) {
                            onDone.run();
                        }
                    });
                });
            });
        });
    }

    public static void promptRemove(BaseFragment fragment, Runnable onDone) {
        verifyCurrent(fragment, LocaleController.getString(R.string.BananaArchiveRemoveTitle), LocaleController.getString(R.string.BananaArchiveRemoveText), LocaleController.getString(R.string.BananaArchiveRemove), () -> {
            int account = fragment.getCurrentAccount();
            clearPassword(account);
            lockNow(account);
            if (onDone != null) {
                onDone.run();
            }
        });
    }

    public static void promptChange(BaseFragment fragment, Runnable onDone) {
        verifyCurrent(fragment, LocaleController.getString(R.string.BananaArchiveChangePassword), LocaleController.getString(R.string.BananaArchiveEnterText), LocaleController.getString(R.string.BananaArchiveContinue), () -> promptSet(fragment, () -> {
            showMessage(fragment, LocaleController.getString(R.string.BananaArchiveChanged));
            if (onDone != null) {
                onDone.run();
            }
        }));
    }

    public static void promptEnableBiometrics(BaseFragment fragment, boolean enable, Runnable onDone) {
        int account = fragment.getCurrentAccount();
        if (!enable) {
            State state = state(account);
            state.biometrics = false;
            write(account, state);
            deleteKey(account);
            if (onDone != null) {
                onDone.run();
            }
            return;
        }
        if (!(fragment.getParentActivity() instanceof FragmentActivity) || !biometricAvailable(fragment.getParentActivity())) {
            showMessage(fragment, LocaleController.getString(R.string.BananaArchiveStorageError));
            return;
        }
        if (!prepareKey(account)) {
            showMessage(fragment, LocaleController.getString(R.string.BananaArchiveStorageError));
            return;
        }
        authenticateBiometric(fragment, () -> {
            State state = state(account);
            state.biometrics = true;
            write(account, state);
            if (onDone != null) {
                onDone.run();
            }
        }, () -> {
            if (onDone != null) {
                onDone.run();
            }
        });
    }

    public static void showRevealedHint(BaseFragment fragment) {
        if (fragment == null) {
            return;
        }
        try {
            BulletinFactory.of(fragment).createSimpleBulletin(R.raw.chats_infotip, LocaleController.getString(R.string.BananaArchiveRevealed)).show();
        } catch (Throwable ignore) {
        }
    }

    private static void askUnlockPassword(BaseFragment fragment, Runnable onSuccess) {
        int account = fragment.getCurrentAccount();
        long wait = remainingCooldownSeconds(account);
        if (wait > 0) {
            showMessage(fragment, LocaleController.formatString(R.string.BananaArchiveCooldown, (int) wait));
            return;
        }
        ask(fragment, LocaleController.getString(R.string.BananaArchiveEnterTitle), LocaleController.getString(R.string.BananaArchiveEnterText), LocaleController.getString(R.string.BananaArchiveUnlock), password -> {
            if (password == null) {
                return;
            }
            Utilities.globalQueue.postRunnable(() -> {
                boolean ok = matches(account, password);
                AndroidUtilities.runOnUIThread(() -> {
                    if (ok) {
                        clearFailures(account);
                        unlock(account, onSuccess);
                    } else {
                        showFailure(fragment, account);
                    }
                });
            });
        });
    }

    private static void verifyCurrent(BaseFragment fragment, String title, String message, String positive, Runnable onMatch) {
        int account = fragment.getCurrentAccount();
        long wait = remainingCooldownSeconds(account);
        if (wait > 0) {
            showMessage(fragment, LocaleController.formatString(R.string.BananaArchiveCooldown, (int) wait));
            return;
        }
        ask(fragment, title, message, positive, password -> {
            if (password == null) {
                return;
            }
            Utilities.globalQueue.postRunnable(() -> {
                boolean ok = matches(account, password);
                AndroidUtilities.runOnUIThread(() -> {
                    if (ok) {
                        clearFailures(account);
                        if (onMatch != null) {
                            onMatch.run();
                        }
                    } else {
                        showFailure(fragment, account);
                    }
                });
            });
        });
    }

    /** Records a wrong password and shows the cooldown once it starts, like the unlock prompt. */
    private static void showFailure(BaseFragment fragment, int account) {
        recordFailure(account);
        long again = remainingCooldownSeconds(account);
        if (again > 0) {
            showMessage(fragment, LocaleController.formatString(R.string.BananaArchiveCooldown, (int) again));
        } else {
            int left = Math.max(0, 5 - state(account).failures);
            showMessage(fragment, LocaleController.formatString(R.string.BananaArchiveIncorrect, left));
        }
    }

    private interface PasswordCallback {
        void onResult(String password);
    }

    private static void ask(BaseFragment fragment, String title, String message, String positive, PasswordCallback callback) {
        if (fragment == null || fragment.getParentActivity() == null) {
            return;
        }
        if (promptOpen) {
            return;
        }
        promptOpen = true;
        PasswordCallback wrapped = password -> {
            promptOpen = false;
            if (callback != null) {
                callback.onResult(password);
            }
        };
        EditTextBoldCursor field = new EditTextBoldCursor(fragment.getParentActivity());
        field.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 18);
        field.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        field.setHintColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        field.setHint(LocaleController.getString(R.string.BananaArchivePasswordHint));
        field.setSingleLine(true);
        field.setBackgroundDrawable(null);
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        field.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        FrameLayout box = new FrameLayout(fragment.getParentActivity());
        box.addView(field, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 20, 6, 20, 10));
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity());
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setView(box);
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), (dialog, which) -> wrapped.onResult(null));
        builder.setPositiveButton(positive, (dialog, which) -> {
            String value = field.getText() == null ? "" : field.getText().toString().trim();
            if (value.isEmpty() || value.length() > 128) {
                wrapped.onResult(null);
                showMessage(fragment, LocaleController.getString(R.string.BananaArchiveMismatch));
                return;
            }
            wrapped.onResult(value);
        });
        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(d -> promptOpen = false);
        try {
            fragment.showDialog(dialog);
        } catch (Throwable t) {
            promptOpen = false;
            FileLog.e(t);
            return;
        }
        AndroidUtilities.runOnUIThread(() -> {
            field.requestFocus();
            AndroidUtilities.showKeyboard(field);
        }, 150);
    }

    private static void showMessage(BaseFragment fragment, String text) {
        if (fragment == null || fragment.getParentActivity() == null || text == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity());
        builder.setMessage(text);
        builder.setPositiveButton(LocaleController.getString(R.string.OK), null);
        fragment.showDialog(builder.create());
    }

    private static void unlock(int account, Runnable onSuccess) {
        if (account >= 0 && account < ACCOUNTS) {
            unlocked[account] = true;
            revealed[account] = true;
        }
        NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.dialogsNeedReload);
        notifySecureChanged();
        if (onSuccess != null) {
            onSuccess.run();
        }
    }

    private static void relockAll() {
        for (int account = 0; account < ACCOUNTS; account++) {
            if (!unlocked[account] && !revealed[account]) {
                continue;
            }
            unlocked[account] = false;
            revealed[account] = false;
            taps[account] = 0;
            if (isConfigured(account)) {
                NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.dialogsNeedReload);
                try {
                    NotificationsController.getInstance(account).showNotifications();
                } catch (Throwable ignore) {
                }
            }
        }
    }

    private static boolean isProtectedDialog(int account, long dialogId) {
        long self = selfId(account);
        if (self != 0 && dialogId == self) {
            return true;
        }
        if (dialogId == DialogObject.makeFolderDialogId(1)) {
            return true;
        }
        try {
            TLRPC.Dialog dialog = MessagesController.getInstance(account).dialogs_dict.get(dialogId);
            return dialog != null && dialog.folder_id == 1;
        } catch (Throwable ignore) {
            return false;
        }
    }

    private static long selfId(int account) {
        try {
            return UserConfig.getInstance(account).getClientUserId();
        } catch (Throwable ignore) {
            return 0;
        }
    }

    private static void muteArchived(int account) {
        try {
            ArrayList<TLRPC.Dialog> archived = MessagesController.getInstance(account).getDialogs(1);
            if (archived == null) {
                return;
            }
            for (int i = 0; i < archived.size(); i++) {
                TLRPC.Dialog dialog = archived.get(i);
                if (dialog == null || DialogObject.isFolderDialogId(dialog.id)) {
                    continue;
                }
                NotificationsController.getInstance(account).setDialogNotificationsSettings(dialog.id, 0, NotificationsController.SETTING_MUTE_FOREVER);
            }
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    private static void authenticateBiometric(BaseFragment fragment, Runnable onSuccess, Runnable onFallback) {
        if (!(fragment.getParentActivity() instanceof FragmentActivity)) {
            if (onFallback != null) {
                onFallback.run();
            }
            return;
        }
        FragmentActivity activity = (FragmentActivity) fragment.getParentActivity();
        if (activity == null) {
            if (onFallback != null) {
                onFallback.run();
            }
            return;
        }
        Cipher cipher;
        try {
            cipher = cipher(fragment.getCurrentAccount());
        } catch (KeyPermanentlyInvalidatedException invalidated) {
            State state = state(fragment.getCurrentAccount());
            state.biometrics = false;
            write(fragment.getCurrentAccount(), state);
            deleteKey(fragment.getCurrentAccount());
            if (onFallback != null) {
                onFallback.run();
            }
            return;
        } catch (Throwable t) {
            if (onFallback != null) {
                onFallback.run();
            }
            return;
        }
        if (cipher == null) {
            if (onFallback != null) {
                onFallback.run();
            }
            return;
        }
        biometricDepth++;
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(LocaleController.getString(R.string.BananaArchiveEnterTitle))
                .setSubtitle(LocaleController.getString(R.string.BananaArchiveBiometricReason))
                .setNegativeButtonText(LocaleController.getString(R.string.BananaArchivePasswordHint))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build();
        BiometricPrompt prompt = new BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), new BiometricPrompt.AuthenticationCallback() {
            private boolean finished;

            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                finish(true);
            }

            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                finish(false);
            }

            private void finish(boolean success) {
                if (finished) {
                    return;
                }
                finished = true;
                biometricDepth = Math.max(0, biometricDepth - 1);
                if (success) {
                    clearFailures(fragment.getCurrentAccount());
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                } else if (onFallback != null) {
                    onFallback.run();
                }
            }
        });
        try {
            prompt.authenticate(info, new BiometricPrompt.CryptoObject(cipher));
        } catch (Throwable t) {
            biometricDepth = Math.max(0, biometricDepth - 1);
            if (onFallback != null) {
                onFallback.run();
            }
        }
    }

    private static boolean biometricAvailable(android.content.Context context) {
        try {
            return BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean prepareKey(int account) {
        try {
            KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(alias(account), KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .setUserAuthenticationRequired(true);
            if (Build.VERSION.SDK_INT >= 30) {
                builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG);
            } else {
                builder.setUserAuthenticationValidityDurationSeconds(-1);
            }
            if (Build.VERSION.SDK_INT >= 24) {
                builder.setInvalidatedByBiometricEnrollment(true);
            }
            generator.init(builder.build());
            generator.generateKey();
            return true;
        } catch (Throwable t) {
            FileLog.e(t);
            return false;
        }
    }

    private static Cipher cipher(int account) throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        SecretKey key = (SecretKey) store.getKey(alias(account), null);
        if (key == null) {
            return null;
        }
        Cipher cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher;
    }

    private static void deleteKey(int account) {
        try {
            KeyStore store = KeyStore.getInstance("AndroidKeyStore");
            store.load(null);
            store.deleteEntry(alias(account));
        } catch (Throwable ignore) {
        }
    }

    private static String alias(int account) {
        return "banana_archive_bio_" + account;
    }

    private static final class State {
        String verifier;
        int failures;
        long failureElapsed;
        boolean biometrics;
    }

    private static State state(int account) {
        State state = new State();
        File file = file(account);
        if (file == null || !file.isFile()) {
            return state;
        }
        synchronized (FILE_LOCK) {
            try {
                byte[] raw = java.nio.file.Files.readAllBytes(file.toPath());
                String text = new String(raw, StandardCharsets.UTF_8);
                String[] lines = text.split("\n");
                for (String line : lines) {
                    int eq = line.indexOf('=');
                    if (eq <= 0) {
                        continue;
                    }
                    String key = line.substring(0, eq);
                    String value = line.substring(eq + 1).trim();
                    if ("verifier".equals(key) && value.startsWith("pbkdf2$")) {
                        state.verifier = value;
                    } else if ("failures".equals(key)) {
                        // A damaged counter must not abort the loop and drop the stored verifier,
                        // which would leave the archive silently unlocked.
                        try { state.failures = Integer.parseInt(value); } catch (NumberFormatException e) { FileLog.e(e); }
                    } else if ("failureElapsed".equals(key)) {
                        try { state.failureElapsed = Long.parseLong(value); } catch (NumberFormatException e) { FileLog.e(e); }
                    } else if ("biometrics".equals(key)) {
                        state.biometrics = "1".equals(value);
                    }
                }
            } catch (Throwable t) {
                FileLog.e(t);
            }
        }
        return state;
    }

    private static void write(int account, State state) {
        File file = file(account);
        if (file == null) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        if (state.verifier != null) {
            builder.append("verifier=").append(state.verifier).append('\n');
        }
        builder.append("failures=").append(state.failures).append('\n');
        builder.append("failureElapsed=").append(state.failureElapsed).append('\n');
        builder.append("biometrics=").append(state.biometrics ? "1" : "0").append('\n');
        synchronized (FILE_LOCK) {
            try {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                java.nio.file.Files.write(file.toPath(), builder.toString().getBytes(StandardCharsets.UTF_8));
            } catch (Throwable t) {
                FileLog.e(t);
            }
        }
    }

    private static File file(int account) {
        if (ApplicationLoader.applicationContext == null || account < 0) {
            return null;
        }
        return new File(ApplicationLoader.applicationContext.getNoBackupFilesDir(), "banana_archive_" + account + ".lock");
    }

    private static boolean storePassword(int account, String password) {
        try {
            byte[] salt = new byte[16];
            Utilities.random.nextBytes(salt);
            byte[] hash = pbkdf2(password, salt, ITERATIONS);
            if (hash == null) {
                return false;
            }
            State state = state(account);
            state.verifier = "pbkdf2$" + ITERATIONS + "$" + Base64.encodeToString(salt, Base64.NO_WRAP) + "$" + Base64.encodeToString(hash, Base64.NO_WRAP);
            state.failures = 0;
            state.failureElapsed = 0;
            write(account, state);
            return state(account).verifier != null;
        } catch (Throwable t) {
            FileLog.e(t);
            return false;
        }
    }

    private static void clearPassword(int account) {
        State state = new State();
        write(account, state);
        deleteKey(account);
        if (account >= 0 && account < ACCOUNTS) {
            unlocked[account] = false;
            revealed[account] = false;
        }
    }

    private static boolean matches(int account, String password) {
        State state = state(account);
        if (state.verifier == null || password == null) {
            return false;
        }
        String[] parts = state.verifier.split("\\$");
        if (parts.length != 4 || !"pbkdf2".equals(parts[0])) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < 10_000 || iterations > 2_000_000) {
                return false;
            }
            byte[] salt = Base64.decode(parts[2], Base64.NO_WRAP);
            byte[] expected = Base64.decode(parts[3], Base64.NO_WRAP);
            byte[] actual = pbkdf2(password, salt, iterations);
            return actual != null && MessageDigest.isEqual(expected, actual);
        } catch (Throwable t) {
            return false;
        }
    }

    private static byte[] pbkdf2(String password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        } catch (Throwable t) {
            FileLog.e(t);
            return null;
        }
    }

    private static void recordFailure(int account) {
        State state = state(account);
        state.failures++;
        state.failureElapsed = SystemClock.elapsedRealtime();
        write(account, state);
    }

    private static void clearFailures(int account) {
        State state = state(account);
        state.failures = 0;
        state.failureElapsed = 0;
        write(account, state);
    }

    private static long remainingCooldownSeconds(int account) {
        State state = state(account);
        if (state.failures < 5) {
            return 0;
        }
        double cooldown = Math.min(3600d, 5d * Math.pow(2d, state.failures - 5));
        long now = SystemClock.elapsedRealtime();
        if (state.failureElapsed <= 0 || now < state.failureElapsed) {
            state.failureElapsed = now;
            write(account, state);
            return (long) cooldown;
        }
        double elapsed = (now - state.failureElapsed) / 1000d;
        double left = cooldown - elapsed;
        if (left < 0) {
            return 0;
        }
        return (long) Math.ceil(left);
    }

    private static boolean anyConfigured() {
        for (int account = 0; account < ACCOUNTS; account++) {
            if (isConfigured(account)) {
                return true;
            }
        }
        return false;
    }

    private static boolean anySessionOpen() {
        for (int account = 0; account < ACCOUNTS; account++) {
            if (unlocked[account]) {
                return true;
            }
        }
        return false;
    }

    private static boolean isProtectedSurface(BaseFragment fragment) {
        if (fragment == null) {
            return false;
        }
        int account = fragment.getCurrentAccount();
        if (!isConfigured(account)) {
            return false;
        }
        if (fragment instanceof DialogsActivity) {
            return ((DialogsActivity) fragment).isArchiveFolder();
        }
        if (fragment instanceof ChatActivity) {
            return isProtectedDialog(account, ((ChatActivity) fragment).getDialogId());
        }
        if (fragment instanceof ProfileActivity) {
            long id = ((ProfileActivity) fragment).getArchiveGateId();
            return id != 0 && isProtectedDialog(account, id);
        }
        return false;
    }

    private static void closeOpenSurfaces() {
        if (closingSurfaces) {
            return;
        }
        LaunchActivity activity = LaunchActivity.instance;
        if (activity == null) {
            return;
        }
        closingSurfaces = true;
        try {
            closeLayout(activity.actionBarLayout);
            closeLayout(activity.rightActionBarLayout);
        } catch (Throwable t) {
            FileLog.e(t);
        } finally {
            closingSurfaces = false;
        }
    }

    private static void closeLayout(INavigationLayout layout) {
        if (layout == null) {
            return;
        }
        ArrayList<BaseFragment> stack;
        try {
            stack = new ArrayList<>(layout.getFragmentStack());
        } catch (Throwable t) {
            return;
        }
        for (int i = stack.size() - 1; i >= 1; i--) {
            BaseFragment fragment = stack.get(i);
            if (!isProtectedSurface(fragment)) {
                continue;
            }
            try {
                layout.removeFragmentFromStack(fragment, true);
            } catch (Throwable t) {
                FileLog.e(t);
            }
        }
    }

    private static void notifySecureChanged() {
        try {
            LaunchActivity activity = LaunchActivity.instance;
            if (activity != null) {
                activity.invalidateArchiveSecure();
            }
        } catch (Throwable ignore) {
        }
    }

    private static void alignKeepArchived(int account) {
        try {
            TLRPC.GlobalPrivacySettings settings = ContactsController.getInstance(account).getGlobalPrivacySettings();
            if (settings == null || settings.keep_archived_unmuted) {
                return;
            }
            settings.keep_archived_unmuted = true;
            TL_account.setGlobalPrivacySettings req = new TL_account.setGlobalPrivacySettings();
            req.settings = settings;
            ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> { });
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    private static void clearProtectedNotifications(int account) {
        try {
            NotificationsController controller = NotificationsController.getInstance(account);
            long self = selfId(account);
            if (self != 0) {
                controller.removeNotificationsForDialog(self);
            }
            ArrayList<TLRPC.Dialog> archived = MessagesController.getInstance(account).getDialogs(1);
            if (archived == null) {
                return;
            }
            for (int i = 0; i < archived.size(); i++) {
                TLRPC.Dialog dialog = archived.get(i);
                if (dialog == null || DialogObject.isFolderDialogId(dialog.id)) {
                    continue;
                }
                controller.removeNotificationsForDialog(dialog.id);
            }
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }
}
