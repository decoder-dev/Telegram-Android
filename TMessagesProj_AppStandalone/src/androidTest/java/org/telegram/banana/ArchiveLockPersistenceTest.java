package org.telegram.banana;

import android.os.Build;
import android.util.AtomicFile;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.banana.BananaArchiveLock;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public class ArchiveLockPersistenceTest {
    @Test
    public void interruptedWriteRecoversVerifierFromAtomicBackup() throws Exception {
        assertTrue("Fresh emulator only", "ranchu".equals(Build.HARDWARE) || "goldfish".equals(Build.HARDWARE));
        int account = UserConfig.MAX_ACCOUNT_COUNT - 1;
        assertFalse("Never touch a signed-in account", UserConfig.getInstance(account).isClientActivated());
        File target = new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getNoBackupFilesDir(),
                "banana_archive_" + account + ".lock");
        File backup = new File(target.getPath() + ".bak");
        assertFalse("Never overwrite an existing lock", target.exists());
        assertFalse("Never overwrite an existing recovery file", backup.exists());
        java.lang.reflect.Field cache = BananaArchiveLock.class.getDeclaredField("persistedStates");
        cache.setAccessible(true);
        Object[] cached = (Object[]) cache.get(null);
        cached[account] = null;
        try {
            // AtomicFile on older Android can leave only .bak after a process dies mid-write.
            try (FileOutputStream output = new FileOutputStream(backup)) {
                output.write("verifier=pbkdf2$100000$fixture$fixture\nfailures=2\nbiometrics=0\n".getBytes(StandardCharsets.UTF_8));
            }
            assertTrue("Recovery must keep the archive locked", BananaArchiveLock.isConfigured(account));
            assertTrue("AtomicFile should restore the base file", target.isFile());
            java.lang.reflect.Method read = BananaArchiveLock.class.getDeclaredMethod("state", int.class);
            read.setAccessible(true);
            Object first = read.invoke(null, account);
            java.lang.reflect.Field verifier = first.getClass().getDeclaredField("verifier");
            verifier.setAccessible(true);
            verifier.set(first, null);
            assertTrue("Callers must not mutate the persisted cache", BananaArchiveLock.isConfigured(account));
            cached[account] = null;
            assertTrue(target.setReadable(false, false));
            assertTrue("A storage read failure must keep the archive locked", BananaArchiveLock.isConfigured(account));
            assertNull("Do not cache a transient read error", cached[account]);
            assertTrue(target.setReadable(true, true));
            assertTrue("Retry must recover after storage becomes readable", BananaArchiveLock.isConfigured(account));
        } finally { target.setReadable(true, true); new AtomicFile(target).delete(); cached[account] = null; }
    }
}
