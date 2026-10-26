package org.telegram.banana;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import androidx.test.platform.app.InstrumentationRegistry;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.Test;
import org.telegram.messenger.voip.VoIPService;
import static org.junit.Assert.*;

/** A context-attached service object only: no onCreate, call, account, microphone or routing change. */
public class VoipAudioDevicesTest {
    private static class HeadlessService extends VoIPService {
        HeadlessService(Context context) { attachBaseContext(context); }
        @Override public int checkSelfPermission(String permission) {
            return Manifest.permission.BLUETOOTH_CONNECT.equals(permission) ? PackageManager.PERMISSION_DENIED : super.checkSelfPermission(permission);
        }
    }
    @Test public void headlessPermissionDeniedAndNoHeadsetClearStaleOutputWithoutStartingACall() {
        if (Build.VERSION.SDK_INT < 31) return;
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            assertTrue("Only use this synthetic no-headset fixture on the emulator", Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("sdk_gphone"));
            HeadlessService service = new HeadlessService(context);
            assertEquals(PackageManager.PERMISSION_DENIED, service.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT));
            try {
                Field adapter = VoIPService.class.getDeclaredField("btAdapter"); adapter.setAccessible(true);
                adapter.set(service, BluetoothAdapter.getDefaultAdapter());
                Method available = VoIPService.class.getDeclaredMethod("isBluetoothHeadsetAvailable", AudioManager.class); available.setAccessible(true);
                assertFalse((Boolean) available.invoke(service, context.getSystemService(Context.AUDIO_SERVICE)));
                service.currentBluetoothDeviceName = "stale fixture headset";
                Method update = VoIPService.class.getDeclaredMethod("checkUpdateBluetoothHeadset"); update.setAccessible(true); update.invoke(service);
                if (!VoIPService.USE_CONNECTION_SERVICE) assertNull("Disconnected routes must not keep their old display name", service.currentBluetoothDeviceName);
                Method fetch = VoIPService.class.getDeclaredMethod("fetchBluetoothDeviceName"); fetch.setAccessible(true); fetch.invoke(service);
                Field fetching = VoIPService.class.getDeclaredField("fetchingBluetoothDeviceName"); fetching.setAccessible(true);
                assertFalse("Unavailable device names must remain retryable", fetching.getBoolean(service));
                assertNull(VoIPService.getSharedInstance());
            } catch (Exception error) { throw new AssertionError(error); }
        });
    }
}
