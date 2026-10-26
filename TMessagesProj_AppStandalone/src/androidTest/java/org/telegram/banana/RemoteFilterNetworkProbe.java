package org.telegram.banana;

import android.os.Build;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.Assume;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.banana.RemoteFilterSource;
import static org.junit.Assert.*;

/** Optional public HTTPS probe, excluded from the deterministic CI suite. Never applies fetched rules. */
public class RemoteFilterNetworkProbe {
    @Test public void fetchPublicJsonWithTheActualAndroidTransport() throws Exception {
        Assume.assumeTrue("Explicit opt-in required", "YES".equals(InstrumentationRegistry.getArguments().getString("banana_filter_source_probe")));
        String source = InstrumentationRegistry.getArguments().getString("banana_public_filter_source");
        assertTrue("Only use a fresh emulator", Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("sdk_gphone"));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(ApplicationLoader::postInitApplication);
        for (int account = 0; account < UserConfig.MAX_ACCOUNT_COUNT; account++) assertFalse("Never run this fixture with a signed-in account", UserConfig.getInstance(account).isClientActivated());
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<RemoteFilterSource.Result> result = new AtomicReference<>();
        RemoteFilterSource.Task task = RemoteFilterSource.fetch(RemoteFilterSource.url(source), value -> { result.set(value); done.countDown(); });
        try {
            assertTrue("The public read must complete within its deadline", done.await(25, TimeUnit.SECONDS));
            assertNotNull("The actual Android HTTPS transfer must succeed", result.get().json);
            assertTrue(new JSONObject(result.get().json).has("version"));
        } finally { task.cancel(); }
    }
}
