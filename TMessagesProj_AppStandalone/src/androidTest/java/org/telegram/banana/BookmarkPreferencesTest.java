package org.telegram.banana;

import android.content.SharedPreferences;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.banana.BananaConfig;
import org.telegram.messenger.banana.BananaHooks;

import static org.junit.Assert.*;

public class BookmarkPreferencesTest {
    private static final String ENTRIES = "bananaMessageBookmarkEntries";

    @Test
    public void switchAndEntriesPersistIndependently() {
        withCleanPreferences(() -> {
            BananaConfig.set(0, BananaConfig.Option.MESSAGE_BOOKMARKS, true);
            BananaHooks.addBookmark(0, -42, 7);
            BananaHooks.addBookmark(0, -42, 8);
            BananaHooks.addBookmark(0, -42, 7);
            assertTrue(BananaHooks.messageBookmarks(0));
            assertEquals(java.util.Arrays.asList("-42:7", "-42:8"), BananaHooks.getBookmarks(0));
            BananaConfig.set(0, BananaConfig.Option.MESSAGE_BOOKMARKS, false);
            assertTrue(BananaHooks.getBookmarks(0).isEmpty());
            BananaConfig.set(0, BananaConfig.Option.MESSAGE_BOOKMARKS, true);
            assertEquals(2, BananaHooks.getBookmarks(0).size());
            BananaHooks.removeBookmark(0, "-42:7");
            assertEquals(java.util.Collections.singletonList("-42:8"), BananaHooks.getBookmarks(0));
        });
    }

    @Test
    public void navigationKeeps64BitPeersAndRejectsMalformedEntries() {
        assertEquals(6_000_000_000L, BananaHooks.bookmarkArguments("6000000000:7").getLong("user_id"));
        assertEquals(6_000_000_000L, BananaHooks.bookmarkArguments("-6000000000:7").getLong("chat_id"));
        long encrypted = org.telegram.messenger.DialogObject.makeEncryptedDialogId(42);
        assertEquals(42, BananaHooks.bookmarkArguments(encrypted + ":7").getInt("enc_id"));
        assertEquals(-210001, BananaHooks.bookmarkArguments(encrypted + ":-210001").getInt("message_id"));
        assertNull(BananaHooks.bookmarkArguments(org.telegram.messenger.DialogObject.makeEncryptedDialogId(0) + ":-1"));
        withCleanPreferences(() -> {
            BananaConfig.set(0, BananaConfig.Option.MESSAGE_BOOKMARKS, true);
            BananaHooks.addBookmark(0, encrypted, -210001);
            assertEquals(java.util.Collections.singletonList(encrypted + ":-210001"), BananaHooks.getBookmarks(0));
            BananaHooks.clearBookmarks(0);
            assertTrue(BananaHooks.messageBookmarks(0));
            assertTrue("A reused account slot must not expose the previous account's bookmarks", BananaHooks.getBookmarks(0).isEmpty());
        });
        assertEquals(7, BananaHooks.bookmarkArguments("-42:7").getInt("message_id"));
        for (String invalid : new String[]{null, "", "text", "0:7", "42:0", "42:-1", "42:2147483648", "42:7:8", "-9223372036854775808:1"}) {
            assertNull(invalid, BananaHooks.bookmarkArguments(invalid));
        }
    }

    @Test
    public void legacyStringRecoversBeforeBooleanReadOrSwitchWrite() {
        withCleanPreferences(() -> {
            SharedPreferences prefs = MessagesController.getMainSettings(0);
            String key = BananaConfig.Option.MESSAGE_BOOKMARKS.key;
            prefs.edit().putString(key, "-42:7").apply();
            assertTrue(BananaHooks.messageBookmarks(0));
            assertEquals(java.util.Collections.singletonList("-42:7"), BananaHooks.getBookmarks(0));
            assertTrue(prefs.getBoolean(key, false));
            prefs.edit().remove(ENTRIES).putString(key, "-43:8").apply();
            BananaConfig.set(0, BananaConfig.Option.MESSAGE_BOOKMARKS, false);
            BananaConfig.set(0, BananaConfig.Option.MESSAGE_BOOKMARKS, true);
            assertEquals(java.util.Collections.singletonList("-43:8"), BananaHooks.getBookmarks(0));
            prefs.edit().putString(key, "-44:9").apply();
            BananaHooks.clearBookmarks(0);
            assertFalse(prefs.contains(key));
            assertFalse(prefs.contains(ENTRIES));
        });
    }

    private void withCleanPreferences(Runnable test) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            SharedPreferences prefs = MessagesController.getMainSettings(0);
            String key = BananaConfig.Option.MESSAGE_BOOKMARKS.key;
            Object oldSwitch = prefs.getAll().get(key), oldEntries = prefs.getAll().get(ENTRIES);
            prefs.edit().remove(key).remove(ENTRIES).apply();
            try { test.run(); }
            finally {
                SharedPreferences.Editor restore = prefs.edit().remove(key).remove(ENTRIES);
                if (oldSwitch instanceof Boolean) restore.putBoolean(key, (Boolean) oldSwitch);
                else if (oldSwitch instanceof String) restore.putString(key, (String) oldSwitch);
                if (oldEntries instanceof String) restore.putString(ENTRIES, (String) oldEntries);
                restore.apply();
            }
        });
    }
}
