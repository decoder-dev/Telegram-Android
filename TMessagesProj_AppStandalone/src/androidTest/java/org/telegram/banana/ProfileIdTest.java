package org.telegram.banana;

import android.content.SharedPreferences;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.banana.BananaConfig;
import org.telegram.messenger.banana.BananaHooks;
import org.telegram.tgnet.TLRPC;
import static org.junit.Assert.*;

public class ProfileIdTest {
    @Test
    public void botApiChannelIdsUseOffsetForEveryIdWidthAndMonoforums() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            SharedPreferences prefs = MessagesController.getMainSettings(0);
            String key = BananaConfig.Option.PROFILE_ID_BOT_API_FORMAT.key;
            Object old = prefs.getAll().get(key);
            java.util.Map<Long, TLRPC.Chat> chats = MessagesController.getInstance(0).getChats();
            try {
                BananaConfig.set(0, BananaConfig.Option.PROFILE_ID_BOT_API_FORMAT, true);
                for (long id : new long[]{42, 1_234_567_890L, 12_345_678_901L, 1_500_000_000_000L}) {
                    TLRPC.Chat channel = new TLRPC.TL_channel(); channel.id = id;
                    TLRPC.Chat previous = chats.put(id, channel);
                    try {
                        assertEquals(Long.toString(-1_000_000_000_000L - id), BananaHooks.formatProfileId(0, 0, id));
                        BananaConfig.set(0, BananaConfig.Option.PROFILE_ID_BOT_API_FORMAT, false);
                        assertEquals(Long.toString(id), BananaHooks.formatProfileId(0, 0, id));
                        BananaConfig.set(0, BananaConfig.Option.PROFILE_ID_BOT_API_FORMAT, true);
                    } finally {
                        if (previous == null) chats.remove(id); else chats.put(id, previous);
                    }
                }
                TLRPC.Chat group = new TLRPC.TL_chat(); group.id = 42;
                TLRPC.Chat previous = chats.put(42L, group);
                try { assertEquals("-42", BananaHooks.formatProfileId(0, 0, 42)); }
                finally { if (previous == null) chats.remove(42L); else chats.put(42L, previous); }
                assertEquals("42", BananaHooks.formatProfileId(0, 42, 0));
            } finally {
                SharedPreferences.Editor restore = prefs.edit().remove(key);
                if (old instanceof Boolean) restore.putBoolean(key, (Boolean) old);
                restore.apply();
            }
        });
    }
}
