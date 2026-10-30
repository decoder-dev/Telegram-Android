package org.telegram.messenger.banana;
import org.junit.Test;
import java.util.*;
import java.io.*;
import javax.crypto.spec.SecretKeySpec;
import static org.junit.Assert.*;

public class SessionBackupCodecTest {
    private SessionBackupCodec.Snapshot snapshot() {
        Map<String,Object> prefs = new HashMap<>(); prefs.put("user", "synthetic-fixture"); prefs.put("selectedAccount", 4);
        prefs.put("integer", 3); prefs.put("long", 9000000000L); prefs.put("boolean", true); prefs.put("float", 1.25f);
        prefs.put("set", new HashSet<>(Arrays.asList("a", "b")));
        return new SessionBackupCodec.Snapshot(42, new byte[]{4,0,0,0,1,2,3,4}, prefs);
    }
    @Test public void roundTripsTypedPreferencesAndNeverRestoresSourceSlot() throws Exception {
        SessionBackupCodec.Snapshot value = SessionBackupCodec.decode(SessionBackupCodec.encode(snapshot()));
        assertEquals(42, value.owner); assertEquals(snapshot().preferences, value.preferences);
        assertFalse(value.preferences.containsKey("selectedAccount")); assertArrayEquals(snapshot().network,value.network);
    }
    @Test public void passwordBackupAuthenticatesCiphertextAndMetadata() throws Exception {
        byte[] plain = SessionBackupCodec.encode(snapshot());
        byte[] encrypted = SessionBackupCodec.encrypt(plain, "correct password".toCharArray());
        assertArrayEquals(plain, SessionBackupCodec.decrypt(encrypted,"correct password".toCharArray()));
        try { SessionBackupCodec.decrypt(encrypted,"wrong password".toCharArray()); fail(); } catch (java.security.GeneralSecurityException expected) { }
        encrypted[encrypted.length-1] ^= 1;
        try { SessionBackupCodec.decrypt(encrypted,"correct password".toCharArray()); fail(); } catch (java.security.GeneralSecurityException expected) { }
    }
    @Test public void eachEncryptionHasUniqueNonceAndAuthenticatedHeader() throws Exception {
        SecretKeySpec key = new SecretKeySpec(new byte[32], "AES");
        byte[] a=SessionBackupCodec.seal(new byte[]{1,2,3},key,new byte[16]);
        byte[] b=SessionBackupCodec.seal(new byte[]{1,2,3},key,new byte[16]);
        assertFalse(Arrays.equals(a,b)); assertArrayEquals(new byte[]{1,2,3},SessionBackupCodec.open(a,key));
        a[4] ^= 1;
        try { SessionBackupCodec.open(a,key); fail(); } catch (java.security.GeneralSecurityException expected) { }
    }
    @Test public void rejectsTruncatedNativeSnapshotAndTrailingPayload() throws Exception {
        assertFalse(SessionBackupCodec.validNetworkConfig(new byte[]{100,0,0,0,1,2,3,4}));
        byte[] original=SessionBackupCodec.encode(snapshot());byte[] extra=Arrays.copyOf(original,original.length+1);
        try { SessionBackupCodec.decode(extra); fail(); } catch (IOException expected) { }
        try { SessionBackupCodec.decode(Arrays.copyOf(original, 12)); fail(); } catch (IOException expected) { }
    }
    @Test public void accountPreferencesNeverCarryDeviceState() {
        Map<String,Object> shared = new HashMap<>();
        shared.put("user", "synthetic-fixture"); shared.put("registeredForPush", true);
        shared.put("2dialogsLoadOffsetId1", 7); shared.put("6migrateOffsetDate", 9);
        shared.put("passcodeHash1", "hash"); shared.put("passcodeSalt", "salt"); shared.put("appLocked", true);
        shared.put("pushString2", "token"); shared.put("selectedAccount", 2); shared.put("lastLocalId", -5);
        shared.put("loginTime", 1); shared.put("draftsLoaded", true); shared.put("filtersLoaded", true);
        // Identity and the user's own choices move; device state and "already loaded" cursors do not.
        assertEquals(new HashSet<>(Arrays.asList("user", "loginTime")), SessionBackupCodec.accountPreferences(shared).keySet());
    }
    @Test public void rejectsWeakPasswordAndOversizedBackup() throws Exception {
        try { SessionBackupCodec.encrypt(new byte[]{1},"short".toCharArray()); fail(); } catch (java.security.GeneralSecurityException expected) { }
        try { SessionBackupCodec.readBounded(new ByteArrayInputStream(new byte[SessionBackupCodec.MAX_BYTES+1])); fail(); } catch (IOException expected) { }
    }
}
