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
        SessionBackupCodec.Snapshot snapshot = new SessionBackupCodec.Snapshot(owner, network, preferences(slot).getAll());
        verifyIdentity(snapshot);
        if (UserConfig.getInstance(slot).getClientUserId() != owner) throw new IOException("Account changed");
        return snapshot;
    }
    private static void verifyIdentity(SessionBackupCodec.Snapshot snapshot) throws IOException {
        Object encoded = snapshot.preferences.get("user"); if (!(encoded instanceof String)) throw new IOException("User missing");
        SerializedData data = new SerializedData(android.util.Base64.decode((String) encoded, android.util.Base64.DEFAULT));
        try {
            TLRPC.User user = TLRPC.User.TLdeserialize(data, data.readInt32(true), true);
            if (user == null || user.id != snapshot.owner) throw new IOException("Session owner mismatch");
        } finally { data.cleanup(); }
    }
    public static void schedule(int account) {
        synchronized (pending) {
            Runnable old = pending.remove(account); if (old != null) QUEUE.cancelRunnable(old);
            Runnable task = () -> {
                try {
                    if (!BananaConfig.get(account, BananaConfig.Option.SESSION_KEYCHAIN_BACKUP) || !UserConfig.getInstance(account).isClientActivated()) return;
                    byte[] plain = SessionBackupCodec.encode(capture(account));
                    try { atomicWrite(new File(directory(), "slot-" + account + ".bin"), SessionBackupCodec.seal(plain, deviceKey(), new byte[16])); }
                    finally { Arrays.fill(plain, (byte) 0); }
                } catch (Exception unavailable) { /* A failed snapshot must never interrupt login or expose session material in logs. */ }
            };
            pending.put(account, task); QUEUE.postRunnable(task, 1000);
        }
    }
    public static void clear(int account) {
        synchronized (pending) { Runnable task = pending.remove(account); if (task != null) QUEUE.cancelRunnable(task); }
        QUEUE.postRunnable(() -> new AtomicFile(new File(directory(), "slot-" + account + ".bin")).delete());
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
            } catch (Exception failure) { }
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
            } catch (Exception failure) { }
            finally { Arrays.fill(password, '\0'); if (plain != null) Arrays.fill(plain, (byte) 0); }
            boolean result = ok; AndroidUtilities.runOnUIThread(() -> done.accept(result));
        });
    }
    public static void restoreLocal(int sourceSlot, java.util.function.Consumer<Boolean> done) {
        QUEUE.postRunnable(() -> {
            boolean ok = false; byte[] plain = null;
            try { plain = SessionBackupCodec.open(read(new File(directory(), "slot-" + sourceSlot + ".bin")), deviceKey()); stage(SessionBackupCodec.decode(plain)); ok = true; }
            catch (Exception failure) { }
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

    /** Called once before native libraries/UserConfig initialize after a confirmed restart. */
    public static void applyPending() {
        for (int slot = 0; slot < UserConfig.MAX_ACCOUNT_COUNT; slot++) {
            File staged = new File(directory(), "restore-" + slot + ".bin"); if (!staged.isFile()) continue;
            byte[] plain = null;
            try {
                // A restore never overwrites an account added between staging and restart.
                plain = SessionBackupCodec.open(read(staged), deviceKey());
                SessionBackupCodec.Snapshot snapshot = SessionBackupCodec.decode(plain); verifyIdentity(snapshot);
                if (preferences(slot).contains("user")) {
                    if (!java.util.Objects.equals(preferences(slot).getString("user", null), snapshot.preferences.get("user"))) continue;
                    // A crash after preference commit can finish selecting this same restored account.
                    if (preferences(0).edit().putInt("selectedAccount", slot).commit()) new AtomicFile(staged).delete();
                    continue;
                }
                atomicWrite(new File(configDirectory(slot), "tgnet.dat"), snapshot.network);
                new File(configDirectory(slot), "tgnet.dat.bak").delete();
                SharedPreferences.Editor editor = preferences(slot).edit().clear();
                for (Map.Entry<String, Object> entry : snapshot.preferences.entrySet()) put(editor, entry.getKey(), entry.getValue());
                if (!editor.commit()) throw new IOException("Cannot restore preferences");
                if (!preferences(0).edit().putInt("selectedAccount", slot).commit()) throw new IOException("Cannot select restored account");
                new AtomicFile(staged).delete();
            } catch (Exception failure) { /* Keep the authenticated staged backup for retry. */ }
            finally { if (plain != null) Arrays.fill(plain, (byte) 0); }
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
