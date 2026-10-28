package org.telegram.banana;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import org.junit.Test;
import org.telegram.messenger.banana.SessionBackupCodec;
import java.security.KeyStore;
import java.util.UUID;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import static org.junit.Assert.*;

/** Real AndroidKeyStore provider, using a temporary key and synthetic non-session bytes. */
public class SessionBackupAndroidTest {
    @Test public void deviceKeyAuthenticatesLocalSnapshots() throws Exception {
        String alias = "banana-backup-fixture-" + UUID.randomUUID();
        KeyStore store = KeyStore.getInstance("AndroidKeyStore"); store.load(null);
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES", "AndroidKeyStore");
            generator.init(new KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
            SecretKey key = generator.generateKey();
            byte[] encrypted = SessionBackupCodec.seal(new byte[]{1, 2, 3}, key, new byte[16]);
            assertArrayEquals(new byte[]{1, 2, 3}, SessionBackupCodec.open(encrypted, key));
            encrypted[encrypted.length - 1] ^= 1;
            try { SessionBackupCodec.open(encrypted, key); fail("Tampered local copy accepted"); }
            catch (java.security.GeneralSecurityException expected) { }
        } finally { store.deleteEntry(alias); }
    }
}
