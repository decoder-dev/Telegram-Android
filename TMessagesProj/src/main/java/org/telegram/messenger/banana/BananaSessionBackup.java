package org.telegram.messenger.banana;

import android.content.*;
import android.net.Uri;
import android.security.keystore.*;
import android.util.AtomicFile;
import org.telegram.messenger.*;
import org.telegram.tgnet.*;
import java.io.*;
import java.security.KeyStore;
import java.util.*;
import javax.crypto.*;

/** App-private keystore snapshots plus password-encrypted portable export. No plaintext backup files. */
public final class BananaSessionBackup {
    private static final DispatchQueue QUEUE = new DispatchQueue("bananaSessionBackup");
    private static final String ALIAS = "banana-session-backup-v1";
    private static final Map<Integer, Runnable> pending = new HashMap<>();
    /** Digest of the last snapshot written per slot; only touched on QUEUE. */
    private static final Map<Integer, byte[]> written = new HashMap<>();
    private BananaSessionBackup() {}
    private static SharedPreferences preferences(int slot) {
        return ApplicationLoader.applicationContext.getSharedPreferences(slot == 0 ? "userconfing" : "userconfig" + slot, Context.MODE_PRIVATE);
    }
    private static File directory() {
        File dir = new File(ApplicationLoader.applicationContext.getNoBackupFilesDir(), "banana-sessions"); dir.mkdirs(); return dir;
    }
    private static File configDirectory(int slot) {
        File base = ApplicationLoader.getFilesDirFixed(); return slot == 0 ? base : new File(base, "account" + slot);
    }
    private static synchronized javax.crypto.SecretKey deviceKey() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore"); store.load(null);
        if (!store.containsAlias(ALIAS)) {
            KeyGenerator generator = KeyGenerator.getInstance("AES", "AndroidKeyStore");
            generator.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
            generator.generateKey();
        }
        return (javax.crypto.SecretKey) store.getKey(ALIAS, null);
    }
    private static void atomicWrite(File destination, byte[] bytes) throws IOException {
        File parent = destination.getParentFile(); if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("Cannot create backup directory");
        AtomicFile file = new AtomicFile(destination); FileOutputStream output = null;
        try { output = file.startWrite(); output.write(bytes); file.finishWrite(output); }
        catch (IOException failure) { file.failWrite(output); throw failure; }
    }
    private static byte[] read(File file) throws IOException {
        try (InputStream input = new FileInputStream(file)) { return SessionBackupCodec.readBounded(input); }
    }
    private static SessionBackupCodec.Snapshot capture(int slot) throws Exception {
        long owner = UserConfig.getInstance(slot).getClientUserId(); if (owner <= 0) throw new IOException("Account is not active");
        File config = new File(configDirectory(slot), "tgnet.dat");
        byte[] network = read(config), again = read(config);
        if (!Arrays.equals(network, again) || new File(configDirectory(slot), "tgnet.dat.bak").exists()) throw new IOException("Session update in progress; retry");
        SessionBackupCodec.Snapshot snapshot = new SessionBackupCodec.Snapshot(owner, network, SessionBackupCodec.accountPreferences(preferences(slot).getAll()));
        verifyIdentity(snapshot);
        if (UserConfig.getInstance(slot).getClientUserId() != owner) throw new IOException("Account changed");
        return snapshot;
    }
    private static void verifyIdentity(SessionBackupCodec.Snapshot snapshot) throws IOException {
        if (userId(snapshot.preferences.get("user")) != snapshot.owner) throw new IOException("Session owner mismatch");
    }
    /** The id in a serialized "user" preference, or 0. Works before UserConfig is loaded. */
    private static long userId(Object encoded) {
        if (!(encoded instanceof String)) return 0;
        SerializedData data = null;
        try {
            data = new SerializedData(android.util.Base64.decode((String) encoded, android.util.Base64.DEFAULT));
            TLRPC.User user = TLRPC.User.TLdeserialize(data, data.readInt32(true), true);
            return user != null ? user.id : 0;
        } catch (Exception broken) {
            return 0;
        } finally { if (data != null) data.cleanup(); }
    }
    public static void schedule(int account) {
        synchronized (pending) {
            Runnable old = pending.remove(account); if (old != null) QUEUE.cancelRunnable(old);
            Runnable task = () -> {
                try {
                    if (!BananaConfig.get(account, BananaConfig.Option.SESSION_KEYCHAIN_BACKUP) || !UserConfig.getInstance(account).isClientActivated()) return;
                    byte[] plain = SessionBackupCodec.encode(capture(account));
                    try {
                        // saveConfig runs on many unrelated updates; rewrite the encrypted copy only when the session changed.
                        byte[] digest = java.security.MessageDigest.getInstance("SHA-256").digest(plain);
                        File target = new File(directory(), "slot-" + account + ".bin");
                        if (target.isFile() && Arrays.equals(digest, written.get(account))) return;
                        atomicWrite(target, SessionBackupCodec.seal(plain, deviceKey(), new byte[16]));
                        written.put(account, digest);
                    }
                    finally { Arrays.fill(plain, (byte) 0); }
                } catch (Exception unavailable) { /* A failed snapshot must never interrupt login or expose session material in logs. */ }
            };
            pending.put(account, task); QUEUE.postRunnable(task, 1000);
        }
    }
    public static void clear(int account) {
        synchronized (pending) { Runnable task = pending.remove(account); if (task != null) QUEUE.cancelRunnable(task); }
        QUEUE.postRunnable(() -> { written.remove(account); new AtomicFile(new File(directory(), "slot-" + account + ".bin")).delete(); });
    }
    /** Only the stage and exception type: messages can carry paths or decrypted session fields. */
    private static void logFailure(String stage, Exception failure) {
        org.telegram.messenger.FileLog.e("BananaSessionBackup " + stage + " failed: " + failure.getClass().getName());
    }

    public static void export(int account, Uri uri, char[] password, java.util.function.Consumer<Boolean> done) {
        QUEUE.postRunnable(() -> {
            boolean ok = false; byte[] plain = null;
            try {
                SessionBackupCodec.Snapshot snapshot = capture(account); plain = SessionBackupCodec.encode(snapshot);
                byte[] encrypted = SessionBackupCodec.encrypt(plain, password);
                try (OutputStream output = ApplicationLoader.applicationContext.getContentResolver().openOutputStream(uri, "wt")) {
                    if (output == null) throw new IOException("Cannot open destination"); output.write(encrypted);
                }
                ok = true;
            } catch (Exception failure) { logFailure("export", failure); }
            finally { Arrays.fill(password, '\0'); if (plain != null) Arrays.fill(plain, (byte) 0); }
            boolean result = ok; AndroidUtilities.runOnUIThread(() -> done.accept(result));
        });
    }
    public static void importBackup(Uri uri, char[] password, java.util.function.Consumer<Boolean> done) {
        QUEUE.postRunnable(() -> {
            boolean ok = false; byte[] plain = null;
            try {
                try (InputStream input = ApplicationLoader.applicationContext.getContentResolver().openInputStream(uri)) {
                    if (input == null) throw new IOException("Cannot read source"); plain = SessionBackupCodec.decrypt(SessionBackupCodec.readBounded(input), password);
                }
                stage(SessionBackupCodec.decode(plain)); ok = true;
            } catch (Exception failure) { logFailure("import", failure); }
            finally { Arrays.fill(password, '\0'); if (plain != null) Arrays.fill(plain, (byte) 0); }
            boolean result = ok; AndroidUtilities.runOnUIThread(() -> done.accept(result));
        });
    }
    public static void restoreLocal(int sourceSlot, java.util.function.Consumer<Boolean> done) {
        QUEUE.postRunnable(() -> {
            boolean ok = false; byte[] plain = null;
            try { plain = SessionBackupCodec.open(read(new File(directory(), "slot-" + sourceSlot + ".bin")), deviceKey()); stage(SessionBackupCodec.decode(plain)); ok = true; }
            catch (Exception failure) { logFailure("restore", failure); }
            finally { if (plain != null) Arrays.fill(plain, (byte) 0); }
            boolean result = ok; AndroidUtilities.runOnUIThread(() -> done.accept(result));
        });
    }
    private static void stage(SessionBackupCodec.Snapshot snapshot) throws Exception {
        verifyIdentity(snapshot); int slot = -1;
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            if (UserConfig.getInstance(i).getClientUserId() == snapshot.owner) throw new IOException("Account already present");
            if (slot == -1 && !preferences(i).contains("user")) slot = i;
        }
        if (slot < 0) throw new IOException("No free account slot");
        byte[] plain = SessionBackupCodec.encode(snapshot);
        try {
            // One import is staged at a time: the confirmation dialog follows each import at once.
            for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) new AtomicFile(new File(directory(), "pending-" + i + ".bin")).delete();
            atomicWrite(new File(directory(), "pending-" + slot + ".bin"), SessionBackupCodec.seal(plain, deviceKey(), new byte[16]));
        } finally { Arrays.fill(plain, (byte) 0); }
    }
    /** Pending imports cannot affect the next launch until the user confirms restart. */
    public static void cancelPendingRestore() {
        QUEUE.postRunnable(() -> {
            for (int slot = 0; slot < UserConfig.MAX_ACCOUNT_COUNT; slot++) new AtomicFile(new File(directory(), "pending-" + slot + ".bin")).delete();
        });
    }

    public static void confirmPendingRestore(java.util.function.Consumer<Boolean> done) {
        QUEUE.postRunnable(() -> {
            boolean ready = false;
            for (int slot = 0; slot < UserConfig.MAX_ACCOUNT_COUNT; slot++) {
                File pending = new File(directory(), "pending-" + slot + ".bin");
                if (pending.isFile()) ready |= pending.renameTo(new File(directory(), "restore-" + slot + ".bin"));
            }
            boolean result = ready; AndroidUtilities.runOnUIThread(() -> done.accept(result));
        });
    }

    /** Failed applications of one staged restore before it is dropped. */
    private static final int MAX_RESTORE_ATTEMPTS = 3;

    /** Called once before native libraries/UserConfig initialize after a confirmed restart. */
    public static void applyPending() {
        // A pending import is confirmed within the process that staged it; one left from a
        // process that died with the confirmation dialog open can never be confirmed.
        for (int slot = 0; slot < UserConfig.MAX_ACCOUNT_COUNT; slot++) new AtomicFile(new File(directory(), "pending-" + slot + ".bin")).delete();
        SharedPreferences attempts = ApplicationLoader.applicationContext.getSharedPreferences("bananaSessionRestore", Context.MODE_PRIVATE);
        for (int stagedSlot = 0; stagedSlot < UserConfig.MAX_ACCOUNT_COUNT; stagedSlot++) {
            File staged = new File(directory(), "restore-" + stagedSlot + ".bin"); if (!staged.isFile()) continue;
            byte[] plain = null;
            boolean done = false;
            try {
                plain = SessionBackupCodec.open(read(staged), deviceKey());
                SessionBackupCodec.Snapshot snapshot = SessionBackupCodec.decode(plain); verifyIdentity(snapshot);
                int slot = stagedSlot;
                int present = -1;
                for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
                    if (userId(preferences(i).getString("user", null)) == snapshot.owner) { present = i; break; }
                }
                if (present >= 0) {
                    // Already signed in, by an earlier attempt that crashed after committing or by
                    // a manual sign-in since staging. Select it; never add a second copy.
                    preferences(0).edit().putInt("selectedAccount", present).commit();
                    done = true;
                    continue;
                }
                if (preferences(slot).contains("user")) {
                    // A restore never overwrites an account added between staging and restart: use another free slot.
                    slot = -1;
                    for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
                        if (!preferences(i).contains("user") && (i == stagedSlot || !new File(directory(), "restore-" + i + ".bin").isFile())) { slot = i; break; }
                    }
                    if (slot < 0) throw new IOException("No free account slot");
                }
                atomicWrite(new File(configDirectory(slot), "tgnet.dat"), snapshot.network);
                new File(configDirectory(slot), "tgnet.dat.bak").delete();
                // Replace only the account's own keys. Slot 0's file also holds the device's passcode,
                // lock state and push token (SharedConfig); clearing it and writing every key of an
                // older backup replaced them with the source device's.
                SharedPreferences target = preferences(slot);
                SharedPreferences.Editor editor = target.edit();
                for (String key : target.getAll().keySet()) if (SessionBackupCodec.isAccountKey(key)) editor.remove(key);
                for (Map.Entry<String, Object> entry : SessionBackupCodec.accountPreferences(snapshot.preferences).entrySet()) {
                    put(editor, entry.getKey(), entry.getValue());
                }
                if (!editor.commit()) throw new IOException("Cannot restore preferences");
                if (!preferences(0).edit().putInt("selectedAccount", slot).commit()) throw new IOException("Cannot select restored account");
                done = true;
            } catch (Exception failure) {
                // Keep the staged backup for a retry on the next launch, but not forever: a
                // backup that cannot be opened (a lost keystore key) would otherwise be retried on every start.
                String key = "attempts" + stagedSlot;
                int failed = attempts.getInt(key, 0) + 1;
                if (failed >= MAX_RESTORE_ATTEMPTS) done = true;
                else attempts.edit().putInt(key, failed).commit();
            } finally {
                if (plain != null) Arrays.fill(plain, (byte) 0);
                if (done) {
                    new AtomicFile(staged).delete();
                    attempts.edit().remove("attempts" + stagedSlot).commit();
                }
            }
        }
    }
    @SuppressWarnings("unchecked")
    private static void put(SharedPreferences.Editor editor, String key, Object value) {
        if (value instanceof String) editor.putString(key, (String) value);
        else if (value instanceof Integer) editor.putInt(key, (Integer) value);
        else if (value instanceof Long) editor.putLong(key, (Long) value);
        else if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
        else if (value instanceof Float) editor.putFloat(key, (Float) value);
        else if (value instanceof Set) editor.putStringSet(key, (Set<String>) value);
    }
}
