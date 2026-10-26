package org.telegram.banana;

import android.os.Build;
import android.os.SystemClock;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/** Opt-in external capability probe; never uses production auth or initializes an account. */
public class BananaTestServerProbe {
    private static final int ACCOUNT = 3;
    private static final int FLAGS = ConnectionsManager.RequestFlagWithoutLogin | ConnectionsManager.RequestFlagFailOnServerErrors;

    @Test
    public void reservedTestNumberAuthorizationIsAvailable() throws Exception {
        assumeTrue("Explicit test-server opt-in required", "YES".equals(
                InstrumentationRegistry.getArguments().getString("banana_test_server")));
        assertTrue("Only run in an Android emulator", "ranchu".equals(Build.HARDWARE) || "goldfish".equals(Build.HARDWARE));
        assertTrue("Only run a debug build", BuildVars.DEBUG_VERSION);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(ApplicationLoader::postInitApplication);
        assertEquals("Refuse a populated account slot", 0, UserConfig.getInstance(ACCOUNT).getClientUserId());
        ConnectionsManager manager = ConnectionsManager.getInstance(ACCOUNT);
        boolean wasTest = manager.isTestBackend();
        boolean authorized = false;
        boolean originalLogging = BuildVars.LOGS_ENABLED;
        BuildVars.LOGS_ENABLED = false;
        try {
            if (!wasTest) manager.switchBackend(false);
            long deadline = SystemClock.uptimeMillis() + 10000;
            while (!manager.isTestBackend() && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(50);
            assertTrue("Test backend must be selected before any RPC", manager.isTestBackend());
            assertTrue("API pair must be supplied without logging it", BuildVars.APP_ID > 0 && BuildVars.APP_HASH.length() == 32);
            String phone = "999662" + String.format(Locale.ROOT, "%04d", new java.security.SecureRandom().nextInt(10000));
            TLRPC.TL_auth_sendCode send = new TLRPC.TL_auth_sendCode();
            send.phone_number = phone;
            send.api_id = BuildVars.APP_ID;
            send.api_hash = BuildVars.APP_HASH;
            send.settings = new TLRPC.TL_codeSettings();
            TLObject answer = rpc(manager, send);
            assertTrue("Test server must support reserved-number auth", answer instanceof TLRPC.TL_auth_sentCode);
            TLRPC.TL_auth_sentCode code = (TLRPC.TL_auth_sentCode) answer;
            int length = code.type.length;
            assertTrue("Only deterministic reserved test codes are supported", length == 5 || length == 6);
            TLRPC.TL_auth_signIn signIn = new TLRPC.TL_auth_signIn();
            signIn.flags = 1;
            signIn.phone_number = phone;
            signIn.phone_code_hash = code.phone_code_hash;
            signIn.phone_code = length == 5 ? "22222" : "222222";
            TLObject auth = rpc(manager, signIn);
            authorized = auth instanceof TLRPC.TL_auth_authorization;
            assertTrue("Test server must accept its documented reserved-number code",
                    authorized || auth instanceof TLRPC.TL_auth_authorizationSignUpRequired);
            System.out.println(authorized ? "BANANA_TEST_AUTH=EXISTING_TEST_ACCOUNT_NO_MESSAGES_SENT" : "BANANA_TEST_AUTH=SIGNUP_AVAILABLE");
        } finally {
            try {
                if (authorized && manager.isTestBackend()) rpc(manager, new TLRPC.TL_auth_logOut());
            } finally {
                manager.cleanup(true);
                if (!wasTest && manager.isTestBackend()) manager.switchBackend(false);
                BuildVars.LOGS_ENABLED = originalLogging;
            }
        }
    }

    private TLObject rpc(ConnectionsManager manager, TLObject request) throws Exception {
        assertTrue("Refuse all production RPCs", manager.isTestBackend());
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<TLObject> response = new AtomicReference<>();
        AtomicReference<TLRPC.TL_error> failure = new AtomicReference<>();
        int id = manager.sendRequest(request, (result, error) -> {
            response.set(result);
            failure.set(error);
            done.countDown();
        }, FLAGS);
        if (!done.await(40, TimeUnit.SECONDS)) {
            manager.cancelRequest(id, true);
            fail("Test backend RPC timed out: " + request.getClass().getSimpleName());
        }
        if (failure.get() != null) {
            String code = failure.get().text;
            fail("Test backend RPC rejected " + request.getClass().getSimpleName() + ": " +
                    (code != null && code.matches("[A-Z_0-9]+") ? code : "RPC_ERROR"));
        }
        return response.get();
    }
}
