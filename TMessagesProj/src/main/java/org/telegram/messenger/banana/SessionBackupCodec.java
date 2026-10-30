package org.telegram.messenger.banana;

import java.io.*;
import java.util.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;

/** Versioned, bounded session backup. Metadata is authenticated with the encrypted payload. */
public final class SessionBackupCodec {
    public static final int MAX_BYTES = 8 * 1024 * 1024;
    private static final int ITERATIONS = 310000;
    private static final byte[] MAGIC = {'B', 'N', 'S', 1};
    private SessionBackupCodec() {}

    public static final class Snapshot {
        public final long owner;
        public final byte[] network;
        public final Map<String, Object> preferences;
        public Snapshot(long owner, byte[] network, Map<String, ?> preferences) {
            if (owner <= 0 || !validNetworkConfig(network)) throw new IllegalArgumentException("Invalid session");
            this.owner = owner; this.network = network.clone(); this.preferences = new TreeMap<>();
            for (Map.Entry<String, ?> item : preferences.entrySet()) {
                if (!"selectedAccount".equals(item.getKey())) this.preferences.put(item.getKey(), item.getValue() instanceof Set ? new HashSet<>((Set<?>) item.getValue()) : item.getValue());
            }
        }
    }
    /**
     * Keys of an account's own state in its userconfig file. Slot 0's file is shared with
     * SharedConfig (passcode hash and salt, lock state, push token, proxy rotation and more), so a
     * backup carries only these: it must not export the source device's passcode hash, nor replace
     * the target device's. Dialog and migration offsets are stored under per-folder suffixes.
     */
    private static final Set<String> ACCOUNT_KEYS = new HashSet<>(Arrays.asList(
            "user", "registeredForPush", "lastSendMessageId", "contactsSavedCount", "lastBroadcastId",
            "lastContactsSyncTime", "lastHintsSyncTime", "draftsLoaded", "unreadDialogsLoaded",
            "contactsReimported", "ratingLoadTime", "botRatingLoadTime", "botGuestRatingLoadTime",
            "webappRatingLoadTime", "loginTime", "syncContacts", "suggestContacts", "hasSecureData",
            "notificationsSettingsLoaded4", "notificationsSignUpSettingsLoaded", "autoDownloadConfigLoadTime",
            "hasValidDialogLoadIds", "sharingMyLocationUntil", "lastMyLocationShareTime", "filtersLoaded",
            "showCallsTab", "genericAnimationsStickerPack", "lastUpdatedGenericAnimations",
            "premiumGiftsStickerPack", "lastUpdatedPremiumGiftsStickerPack", "terms", "tmpPassword"));

    public static boolean isAccountKey(String key) {
        return key != null && (ACCOUNT_KEYS.contains(key) || key.startsWith("2dialogsLoadOffset") || key.startsWith("6migrateOffset"));
    }

    public static Map<String, Object> accountPreferences(Map<String, ?> all) {
        Map<String, Object> result = new TreeMap<>();
        for (Map.Entry<String, ?> item : all.entrySet()) if (isAccountKey(item.getKey())) result.put(item.getKey(), item.getValue());
        return result;
    }

    public static boolean validNetworkConfig(byte[] data) {
        if (data == null || data.length < 8 || data.length > MAX_BYTES / 2) return false;
        long length = (data[0] & 255L) | ((data[1] & 255L) << 8) | ((data[2] & 255L) << 16) | ((data[3] & 255L) << 24);
        return length == data.length - 4;
    }
    public static byte[] encode(Snapshot snapshot) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(); DataOutputStream out = new DataOutputStream(bytes);
        out.writeInt(1); out.writeLong(snapshot.owner); out.writeInt(snapshot.network.length); out.write(snapshot.network);
        if (snapshot.preferences.size() > 1024) throw new IOException("Too many preferences");
        out.writeInt(snapshot.preferences.size());
        for (Map.Entry<String, Object> entry : snapshot.preferences.entrySet()) {
            out.writeUTF(entry.getKey()); Object value = entry.getValue();
            if (value instanceof String) { out.writeByte(1); byte[] text = ((String) value).getBytes(java.nio.charset.StandardCharsets.UTF_8); out.writeInt(text.length); out.write(text); }
            else if (value instanceof Integer) { out.writeByte(2); out.writeInt((Integer) value); }
            else if (value instanceof Long) { out.writeByte(3); out.writeLong((Long) value); }
            else if (value instanceof Boolean) { out.writeByte(4); out.writeBoolean((Boolean) value); }
            else if (value instanceof Float) { out.writeByte(5); out.writeFloat((Float) value); }
            else if (value instanceof Set) { out.writeByte(6); out.writeInt(((Set<?>) value).size()); for (Object item : (Set<?>) value) out.writeUTF((String) item); }
            else throw new IOException("Unsupported preference type");
            if (bytes.size() > MAX_BYTES - 128) throw new IOException("Snapshot too large");
        }
        return bytes.toByteArray();
    }
    public static Snapshot decode(byte[] bytes) throws IOException {
        if (bytes.length > MAX_BYTES) throw new IOException("Snapshot too large");
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
        if (in.readInt() != 1) throw new IOException("Unknown snapshot version");
        long owner = in.readLong(); byte[] network = field(in, MAX_BYTES / 2);
        int count = in.readInt(); if (count < 0 || count > 1024) throw new IOException("Invalid preference count");
        Map<String, Object> preferences = new TreeMap<>();
        for (int n = 0; n < count; n++) {
            String key = in.readUTF(); Object value;
            switch (in.readByte()) {
                case 1: value = new String(field(in, 1024 * 1024), java.nio.charset.StandardCharsets.UTF_8); break;
                case 2: value = in.readInt(); break;
                case 3: value = in.readLong(); break;
                case 4: value = in.readBoolean(); break;
                case 5: value = in.readFloat(); break;
                case 6:
                    int size = in.readInt(); if (size < 0 || size > 1024) throw new IOException("Invalid set size");
                    Set<String> set = new HashSet<>(); for (int i = 0; i < size; i++) set.add(in.readUTF()); value = set; break;
                default: throw new IOException("Unknown preference type");
            }
            if (preferences.put(key, value) != null) throw new IOException("Duplicate preference");
        }
        if (in.available() != 0) throw new IOException("Trailing data");
        try { return new Snapshot(owner, network, preferences); } catch (IllegalArgumentException invalid) { throw new IOException("Invalid session"); }
    }
    private static byte[] field(DataInputStream in, int max) throws IOException {
        int size = in.readInt(); if (size < 0 || size > max || size > in.available()) throw new IOException("Invalid length");
        byte[] bytes = new byte[size]; in.readFully(bytes); return bytes;
    }
    public static byte[] encrypt(byte[] plain, char[] password) throws GeneralSecurityException, IOException {
        if (password.length < 10) throw new GeneralSecurityException("Password too short");
        byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt);
        SecretKey key = passwordKey(password, salt);
        byte[] envelope = seal(plain, key, salt);
        return envelope;
    }
    public static byte[] decrypt(byte[] encrypted, char[] password) throws GeneralSecurityException, IOException {
        if (encrypted.length < 48 || encrypted.length > MAX_BYTES) throw new IOException("Invalid backup size");
        byte[] salt = Arrays.copyOfRange(encrypted, 4, 20);
        return open(encrypted, passwordKey(password, salt));
    }
    private static SecretKey passwordKey(char[] password, byte[] salt) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, 256);
        try {
            byte[] derived = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).getEncoded();
            try { return new SecretKeySpec(derived, "AES"); } finally { Arrays.fill(derived, (byte) 0); }
        } finally { spec.clearPassword(); }
    }
    public static byte[] seal(byte[] plain, SecretKey key, byte[] salt) throws GeneralSecurityException, IOException {
        if (plain.length > MAX_BYTES - 64 || salt.length != 16) throw new IOException("Backup too large");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, key);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(); bytes.write(MAGIC); bytes.write(salt); bytes.write(cipher.getIV());
        byte[] header = bytes.toByteArray(); cipher.updateAAD(header); bytes.write(cipher.doFinal(plain)); return bytes.toByteArray();
    }
    public static byte[] open(byte[] bytes, SecretKey key) throws GeneralSecurityException, IOException {
        if (bytes.length < 48 || bytes.length > MAX_BYTES || !Arrays.equals(MAGIC, Arrays.copyOf(bytes, 4))) throw new IOException("Invalid backup");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, bytes, 20, 12));
        cipher.updateAAD(bytes, 0, 32); return cipher.doFinal(bytes, 32, bytes.length - 32);
    }
    public static byte[] readBounded(InputStream input) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(); byte[] chunk = new byte[8192]; int count;
        while ((count = input.read(chunk)) != -1) { if (bytes.size() + count > MAX_BYTES) throw new IOException("Backup too large"); bytes.write(chunk, 0, count); }
        return bytes.toByteArray();
    }
}
