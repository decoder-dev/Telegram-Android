package org.telegram.banana;

import android.os.Build;
import android.os.SystemClock;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.telegram.SQLite.SQLiteDatabase;
import org.telegram.SQLite.SQLitePreparedStatement;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.banana.BananaConfig;
import org.telegram.messenger.banana.HistoryDatabase;
import org.telegram.messenger.banana.MessageHistory;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLRPC;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/** Local capture integration, not server acceptance. No login, initialized native user ID or real messages. */
public class HistoryCaptureTest {
    private static final int ACCOUNT = 3;

    @Test
    public void cacheReplacementDeletionAndPendingClearPreserveOnlyOwnedSnapshots() throws Exception {
        assertTrue("Fresh emulator only", "ranchu".equals(Build.HARDWARE) || "goldfish".equals(Build.HARDWARE));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(ApplicationLoader::postInitApplication);
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            assertFalse("Never mutate a signed-in client", UserConfig.getInstance(i).isClientActivated());
        }
        ConnectionsManager manager = ConnectionsManager.getInstance(ACCOUNT);
        boolean wasTest = manager.isTestBackend();
        if (!wasTest) manager.switchBackend(false);
        long until = SystemClock.uptimeMillis() + 5000;
        while (!manager.isTestBackend() && SystemClock.uptimeMillis() < until) SystemClock.sleep(50);
        assertTrue("Never run a fixture against production", manager.isTestBackend());
        ConnectionsManager.native_setNetworkAvailable(ACCOUNT, false, 0, false);
        boolean logging = BuildVars.LOGS_ENABLED;
        BuildVars.LOGS_ENABLED = false;
        UserConfig config = UserConfig.getInstance(ACCOUNT);
        // Only the capture layer needs an owning ID. Do not call login initialization,
        // setCurrentUser (which starts RPCs), saveConfig or ConnectionsManager.setUserId.
        Field userField = UserConfig.class.getDeclaredField("currentUser");
        userField.setAccessible(true);
        TLRPC.TL_user fixture = new TLRPC.TL_user();
        fixture.id = 9000000000000L + new java.security.SecureRandom().nextInt(1000000000);
        fixture.first_name = "Local fixture";
        boolean[] previous = new boolean[BananaConfig.Option.values().length];
        for (BananaConfig.Option option : BananaConfig.Option.values()) previous[option.ordinal()] = BananaConfig.get(ACCOUNT, option);
        File root = new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir(), "capture-fixture-" + UUID.randomUUID());
        assertTrue(root.mkdirs());
        SQLiteDatabase source = null;
        FileLoader loader = FileLoader.getInstance(ACCOUNT);
        long documentId = Math.abs(new java.security.SecureRandom().nextLong() / 2) + 1;
        CountDownLatch release = new CountDownLatch(1);
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                try { userField.set(config, fixture); }
                catch (IllegalAccessException error) { throw new AssertionError(error); }
            });
            BananaConfig.set(ACCOUNT, BananaConfig.Option.SAVE_EDITS, true);
            BananaConfig.set(ACCOUNT, BananaConfig.Option.SAVE_DELETED, true);
            BananaConfig.set(ACCOUNT, BananaConfig.Option.SAVE_MEDIA_HISTORY, true);
            BananaConfig.set(ACCOUNT, BananaConfig.Option.SAVE_BOT_HISTORY, true);
            File original = new File(root, "original.txt");
            try (FileOutputStream out = new FileOutputStream(original)) { out.write(new byte[]{7, 8, 9}); }
            TLRPC.Document document = document(documentId);
            loader.setLocalPathTo(document, original.getAbsolutePath());
            assertEquals(original.getAbsolutePath(), loader.getFileDatabase().getPath(document.id, 2, FileLoader.MEDIA_DIR_DOCUMENT, true));
            source = new SQLiteDatabase(new File(root, "cache.sqlite").getAbsolutePath());
            source.executeFast("CREATE TABLE messages_v2(uid INTEGER,mid INTEGER,is_channel INTEGER,data BLOB,PRIMARY KEY(uid,mid))").stepThis().dispose();
            TLRPC.Message old = message(501, document, "before edit");
            put(source, old);
            TLRPC.Message edited = message(501, document, "after edit");
            edited.edit_date = 2000;
            edited.flags |= 32768;
            MessageHistory.captureEdits(source, ACCOUNT, Collections.singletonList(edited));
            put(source, edited);
            loader.deleteFiles(new ArrayList<>(Collections.singletonList(original)), 0);
            List<HistoryDatabase.Record> first = history(1);
            assertEquals("before edit", first.get(0).text);
            assertFalse(first.get(0).deleted);
            AtomicReference<File> retained = new AtomicReference<>();
            CountDownLatch opened = new CountDownLatch(1);
            MessageHistory.attachment(ACCOUNT, first.get(0), file -> { retained.set(file); opened.countDown(); });
            assertTrue(opened.await(5, TimeUnit.SECONDS));
            assertNotNull(retained.get());
            try (FileInputStream in = new FileInputStream(retained.get())) {
                assertEquals(7, in.read()); assertEquals(8, in.read()); assertEquals(9, in.read()); assertEquals(-1, in.read());
            }
            until = SystemClock.uptimeMillis() + 5000;
            while (original.exists() && SystemClock.uptimeMillis() < until) SystemClock.sleep(50);
            assertFalse("Cache unlink must really occur", original.exists());
            MessageHistory.captureDeletes(source, ACCOUNT, 42, Collections.singletonList(501));
            List<HistoryDatabase.Record> deleted = history(2);
            assertTrue(deleted.get(0).deleted);
            assertEquals("after edit", deleted.get(0).text);
            assertEquals("The immutable file should be reused across versions", first.get(0).attachment.storedName,
                    deleted.get(0).attachment.storedName);

            File second = new File(root, "second.txt");
            try (FileOutputStream out = new FileOutputStream(second)) { out.write(new byte[]{1, 2, 3}); }
            TLRPC.Document secondDocument = document(documentId + 1);
            loader.setLocalPathTo(secondDocument, second.getAbsolutePath());
            loader.getFileDatabase().getPath(secondDocument.id, 2, FileLoader.MEDIA_DIR_DOCUMENT, true);
            put(source, message(502, secondDocument, "pending"));
            loader.getFileLoaderQueue().postRunnable(() -> {
                try { release.await(10, TimeUnit.SECONDS); }
                catch (InterruptedException error) { Thread.currentThread().interrupt(); }
            });
            MessageHistory.captureDeletes(source, ACCOUNT, 42, Collections.singletonList(502));
            clear();
            release.countDown();
            CountDownLatch fileQueueDrained = new CountDownLatch(1);
            loader.getFileLoaderQueue().postRunnable(fileQueueDrained::countDown);
            assertTrue(fileQueueDrained.await(5, TimeUnit.SECONDS));
            assertEquals("A cleared pending copy must not resurrect history", 0, load().size());
            assertTrue("Clear must not delete the original Telegram cache file", second.isFile());
            assertFalse("Clear must remove the shared retained copy", retained.get().exists());

            put(source, message(503, secondDocument, ""));
            MessageHistory.captureDeletes(source, ACCOUNT, 42, Collections.singletonList(503));
            List<HistoryDatabase.Record> mediaOnly = history(1);
            assertEquals("", mediaOnly.get(0).text);
            assertNotNull(mediaOnly.get(0).attachment.storedName);
            assertEquals(3, mediaOnly.get(0).attachment.bytes);
        } finally {
            release.countDown();
            try { if (config.getClientUserId() == fixture.id) clear(); }
            finally {
                for (BananaConfig.Option option : BananaConfig.Option.values()) BananaConfig.set(ACCOUNT, option, previous[option.ordinal()]);
                InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                    try { userField.set(config, null); }
                    catch (IllegalAccessException error) { throw new AssertionError(error); }
                });
                loader.getFileDatabase().putPath(documentId, 2, FileLoader.MEDIA_DIR_DOCUMENT, 0, null);
                loader.getFileDatabase().putPath(documentId + 1, 2, FileLoader.MEDIA_DIR_DOCUMENT, 0, null);
                manager.cleanup(true);
                if (!wasTest && manager.isTestBackend()) manager.switchBackend(false);
                manager.checkConnection();
                BuildVars.LOGS_ENABLED = logging;
                if (source != null) source.close();
                remove(root);
            }
        }
    }

    private TLRPC.Document document(long id) {
        TLRPC.TL_document doc = new TLRPC.TL_document();
        doc.id = id; doc.dc_id = 2; doc.access_hash = 1; doc.file_reference = new byte[0];
        doc.mime_type = "text/plain"; doc.size = 3;
        TLRPC.TL_documentAttributeFilename name = new TLRPC.TL_documentAttributeFilename();
        name.file_name = "fixture.txt";
        doc.attributes.add(name);
        return doc;
    }

    private TLRPC.Message message(int id, TLRPC.Document document, String text) {
        TLRPC.TL_message message = new TLRPC.TL_message();
        message.id = id; message.dialog_id = 42; message.date = 1000; message.message = text;
        TLRPC.TL_peerUser peer = new TLRPC.TL_peerUser(); peer.user_id = 42;
        message.peer_id = peer; message.from_id = peer;
        message.flags = TLRPC.MESSAGE_FLAG_HAS_MEDIA | TLRPC.MESSAGE_FLAG_HAS_FROM_ID;
        message.media = new TLRPC.TL_messageMediaDocument(); message.media.flags = 1; message.media.document = document;
        return message;
    }

    private void put(SQLiteDatabase db, TLRPC.Message message) throws Exception {
        NativeByteBuffer bytes = new NativeByteBuffer(message.getObjectSize());
        SQLitePreparedStatement statement = db.executeFast("REPLACE INTO messages_v2 VALUES(?,?,0,?)");
        try {
            message.serializeToStream(bytes);
            statement.bindLong(1, 42); statement.bindInteger(2, message.id); statement.bindByteBuffer(3, bytes); statement.step();
        } finally { statement.dispose(); bytes.reuse(); }
    }

    private List<HistoryDatabase.Record> history(int count) throws Exception {
        long deadline = SystemClock.uptimeMillis() + 10000;
        do {
            List<HistoryDatabase.Record> records = load();
            if (records.size() == count && records.stream().allMatch(r -> r.attachment != null && r.attachment.storedName != null)) return records;
            SystemClock.sleep(100);
        } while (SystemClock.uptimeMillis() < deadline);
        throw new AssertionError("Expected durable capture records with attached files: " + count);
    }

    private List<HistoryDatabase.Record> load() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<List<HistoryDatabase.Record>> answer = new AtomicReference<>();
        MessageHistory.load(ACCOUNT, "", 0, records -> { answer.set(records); done.countDown(); });
        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertNotNull(answer.get());
        return answer.get();
    }

    private void clear() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Boolean> success = new AtomicReference<>();
        MessageHistory.clear(ACCOUNT, ok -> { success.set(ok); done.countDown(); });
        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertEquals(Boolean.TRUE, success.get());
    }

    private void remove(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) for (File child : files) remove(child);
        }
        assertTrue(!file.exists() || file.delete());
    }
}
