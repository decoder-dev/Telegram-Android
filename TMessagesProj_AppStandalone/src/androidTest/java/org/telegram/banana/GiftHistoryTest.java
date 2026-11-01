package org.telegram.banana;

import android.content.SharedPreferences;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.SecretChatHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.banana.BananaConfig;
import org.telegram.messenger.banana.GiftHistory;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_stars;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

public class GiftHistoryTest {
    private static TL_stars.SavedStarGift gift(long id) {
        TL_stars.SavedStarGift saved = new TL_stars.TL_savedStarGift();
        saved.gift = new TL_stars.TL_starGiftUnique();
        saved.gift.id = id;
        saved.gift.slug = "Fixture-" + id;
        saved.gift.title = "Fixture";
        saved.date = 100;
        return saved;
    }

    @Test
    public void partialAndDuplicatePagesCannotDeclareGiftsMissing() {
        TL_stars.SavedStarGift a = gift(1), b = gift(2);
        assertFalse(GiftHistory.isComplete(Collections.singletonList(a), 2));
        assertFalse(GiftHistory.isComplete(Arrays.asList(a, a), 2));
        assertFalse(GiftHistory.isComplete(Collections.singletonList(a), 1001));
        assertTrue(GiftHistory.isComplete(Arrays.asList(a, b), 2));
        assertTrue(GiftHistory.isComplete(Collections.emptyList(), 0));
        assertEquals(Collections.singletonList(a), GiftHistory.missing(Arrays.asList(a, b), Collections.singletonList(b)));
        assertTrue(GiftHistory.missing(Arrays.asList(a, b), Arrays.asList(gift(1), gift(2))).isEmpty());
    }

    @Test
    public void upgradeIsNotReportedAsADisappearedGift() {
        TL_stars.SavedStarGift original = new TL_stars.TL_savedStarGift();
        original.gift = new TL_stars.TL_starGift(); original.gift.id = 50; original.date = 100;
        TL_stars.SavedStarGift upgraded = gift(51); upgraded.gift.gift_id = 50;
        assertTrue(GiftHistory.missing(Collections.singletonList(original), Collections.singletonList(upgraded)).isEmpty());
        upgraded.date = 500;
        TL_stars.starGiftAttributeOriginalDetails details = new TL_stars.starGiftAttributeOriginalDetails();
        details.date = original.date;
        upgraded.gift.attributes.add(details);
        assertTrue(GiftHistory.missing(Collections.singletonList(original), Collections.singletonList(upgraded)).isEmpty());
    }

    private ArrayList<TL_stars.SavedStarGift> observe(TL_stars.SavedStarGift... gifts) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<ArrayList<TL_stars.SavedStarGift>> result = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> GiftHistory.reconcile(0, 123456789,
                Arrays.asList(gifts), gifts.length, value -> { result.set(value); done.countDown(); }));
        assertTrue("Gift history IO did not complete", done.await(10, TimeUnit.SECONDS));
        return result.get();
    }

    @Test
    public void persistedSnapshotsRetainDisappearancesAndRemoveReappearances() throws Exception {
        assertTrue(android.os.Build.PRODUCT.contains("sdk") || android.os.Build.PRODUCT.contains("emulator"));
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) assertEquals(0, UserConfig.getInstance(i).getClientUserId());
        SharedPreferences prefs = MessagesController.getMainSettings(0);
        String key = BananaConfig.Option.SHOW_DISAPPEARED_GIFTS.key;
        Object old = prefs.getAll().get(key);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            TLRPC.User fake = new TLRPC.TL_user(); fake.id = 987654321;
            UserConfig.getInstance(0).setCurrentUser(fake);
            BananaConfig.set(0, BananaConfig.Option.SHOW_DISAPPEARED_GIFTS, true);
            GiftHistory.clear(0);
        });
        try {
            assertTrue(observe(gift(1), gift(2)).isEmpty());
            ArrayList<TL_stars.SavedStarGift> missing = observe(gift(2));
            assertEquals(1, missing.size()); assertEquals(1, missing.get(0).gift.id);
            assertEquals(1, observe(gift(2)).size());
            assertTrue(observe(gift(1), gift(2)).isEmpty());
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                GiftHistory.clear(0);
                try {
                    java.lang.reflect.Field user = UserConfig.class.getDeclaredField("currentUser");
                    java.lang.reflect.Field id = UserConfig.class.getDeclaredField("clientUserId");
                    user.setAccessible(true); id.setAccessible(true);
                    user.set(UserConfig.getInstance(0), null); id.setLong(UserConfig.getInstance(0), 0);
                } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
                SharedPreferences.Editor restore = prefs.edit().remove(key);
                if (old instanceof Boolean) restore.putBoolean(key, (Boolean) old);
                restore.apply();
            });
        }
    }

    @Test
    public void allowedSecretScreenshotDoesNotCreateAnOutgoingServiceMessage() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            assertEquals(0, UserConfig.getInstance(0).getClientUserId());
            SharedPreferences prefs = MessagesController.getMainSettings(0);
            String key = BananaConfig.Option.ALLOW_SECRET_SCREENSHOTS.key;
            Object old = prefs.getAll().get(key);
            int nextId = UserConfig.getInstance(0).lastSendMessageId;
            try {
                BananaConfig.set(0, BananaConfig.Option.ALLOW_SECRET_SCREENSHOTS, true);
                SecretChatHelper.getInstance(0).sendScreenshotMessage(new TLRPC.TL_encryptedChat(), new ArrayList<>(), null);
                assertEquals(nextId, UserConfig.getInstance(0).lastSendMessageId);
            } finally {
                SharedPreferences.Editor restore = prefs.edit().remove(key);
                if (old instanceof Boolean) restore.putBoolean(key, (Boolean) old);
                restore.apply();
            }
        });
    }
}
