package org.telegram.banana;

import android.content.SharedPreferences;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.banana.BananaConfig;
import org.telegram.messenger.banana.BananaHooks;
import org.telegram.tgnet.TLRPC;
import static org.junit.Assert.*;

public class MentionPingTest {
    @Test
    public void removePingsUsesChatMutePolicyAndFolderRulesWithoutErasingMentions() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            SharedPreferences prefs = MessagesController.getMainSettings(0);
            String key = BananaConfig.Option.REMOVE_PINGS.key;
            Object old = prefs.getAll().get(key);
            MessagesController controller = MessagesController.getInstance(0);
            TLRPC.Chat group = new TLRPC.TL_chat(); group.id = 42;
            TLRPC.Chat previous = controller.getChats().put(42L, group);
            try {
                TLRPC.Dialog dialog = new TLRPC.TL_dialog();
                dialog.id = -42; dialog.unread_mentions_count = 1;
                MessagesController.DialogFilter filter = new MessagesController.DialogFilter();
                filter.flags = MessagesController.DIALOG_FILTER_FLAG_GROUPS | MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_READ;
                BananaConfig.set(0, BananaConfig.Option.REMOVE_PINGS, false);
                assertTrue(BananaHooks.useMentionNotificationOverride(0, -42, true));
                assertTrue(filter.includesDialog(AccountInstance.getInstance(0), -42, dialog));
                BananaConfig.set(0, BananaConfig.Option.REMOVE_PINGS, true);
                assertFalse("Group mentions must respect the group's own notification settings", BananaHooks.useMentionNotificationOverride(0, -42, true));
                assertTrue("Private message notifications must retain their own routing", BananaHooks.useMentionNotificationOverride(0, 42, true));
                assertFalse(BananaHooks.useMentionNotificationOverride(0, -42, false));
                assertEquals(0, BananaHooks.visibleMentionCount(0, dialog.unread_mentions_count));
                assertFalse(filter.includesDialog(AccountInstance.getInstance(0), -42, dialog));
                assertEquals("The stored server count must remain intact", 1, dialog.unread_mentions_count);
                dialog.unread_count = 1;
                assertTrue("Ordinary unread messages must still keep the chat in the folder", filter.includesDialog(AccountInstance.getInstance(0), -42, dialog));
                dialog.unread_count = 0;
                filter.alwaysShow.add(-42L);
                assertTrue("Explicit folder inclusions keep their priority", filter.includesDialog(AccountInstance.getInstance(0), -42, dialog));
            } finally {
                if (previous == null) controller.getChats().remove(42L); else controller.getChats().put(42L, previous);
                SharedPreferences.Editor restore = prefs.edit().remove(key);
                if (old instanceof Boolean) restore.putBoolean(key, (Boolean) old);
                restore.apply();
            }
        });
    }
}
