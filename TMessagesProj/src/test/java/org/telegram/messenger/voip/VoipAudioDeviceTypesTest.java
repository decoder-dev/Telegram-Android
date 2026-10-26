package org.telegram.messenger.voip;

import android.media.AudioDeviceInfo;
import org.junit.Test;
import static org.junit.Assert.*;

public class VoipAudioDeviceTypesTest {
    @Test public void aMediaBluetoothRouteIsNotACallHeadset() {
        for (int api : new int[]{23, 28, 31, 36}) {
            assertFalse(VoipAudioManager.isBluetoothDeviceType(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, api));
            assertFalse(VoipAudioManager.isBluetoothDeviceType(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, api));
            assertFalse(VoipAudioManager.isBluetoothDeviceType(AudioDeviceInfo.TYPE_WIRED_HEADSET, api));
            assertFalse(VoipAudioManager.isBluetoothDeviceType(Integer.MAX_VALUE, api));
            assertTrue(VoipAudioManager.isBluetoothDeviceType(AudioDeviceInfo.TYPE_BLUETOOTH_SCO, api));
        }
    }
    @Test public void hearingAidsAndLeAudioUseTheirSupportedPlatformVersions() {
        assertFalse(VoipAudioManager.isBluetoothDeviceType(AudioDeviceInfo.TYPE_HEARING_AID, 27));
        assertTrue(VoipAudioManager.isBluetoothDeviceType(AudioDeviceInfo.TYPE_HEARING_AID, 28));
        for (int type : new int[]{AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLE_SPEAKER}) {
            assertFalse(VoipAudioManager.isBluetoothDeviceType(type, 30));
            assertTrue(VoipAudioManager.isBluetoothDeviceType(type, 31));
            assertTrue(VoipAudioManager.isBluetoothDeviceType(type, 36));
        }
    }
}
