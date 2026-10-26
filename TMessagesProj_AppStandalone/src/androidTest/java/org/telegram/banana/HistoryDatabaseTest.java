package org.telegram.banana;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.telegram.messenger.banana.HistoryDatabase;
import java.io.File;
import java.util.List;
import java.util.UUID;
import static org.junit.Assert.*;

/** Synthetic local databases only: never opens a user account, network or Telegram cache. */
public class HistoryDatabaseTest {
    private Context context() { return InstrumentationRegistry.getInstrumentation().getTargetContext(); }
    private File directory() { return new File(context().getCacheDir(), "history-test-" + UUID.randomUUID()); }
    private HistoryDatabase.Record record(int mid, boolean deleted, String text) {
        return new HistoryDatabase.Record(42, mid, 50, 0, 1000, deleted, text, new byte[0]);
    }

    @Test
    public void versionsSurviveReopenAndStayInsideOwnerAndBackend() {
        File dir = directory();
        try {
            try (HistoryDatabase db = new HistoryDatabase(context(), dir, 1, false)) {
                assertTrue(db.append(record(7, false, "A")));
                assertFalse(db.append(record(7, false, "A")));
                assertTrue(db.append(record(7, false, "B")));
                assertTrue("A reverted edit is a new chronological version", db.append(record(7, false, "A")));
                assertTrue(db.append(record(7, true, "last text")));
                assertFalse(db.append(record(7, true, "duplicate delete event")));
                assertEquals(4, db.count());
            }
            try (HistoryDatabase db = new HistoryDatabase(context(), dir, 1, false);
                 HistoryDatabase otherOwner = new HistoryDatabase(context(), dir, 2, false);
                 HistoryDatabase testBackend = new HistoryDatabase(context(), dir, 1, true)) {
                assertEquals(4, db.count());
                assertEquals("last text", db.recent("", 0, 100).get(0).text);
                assertEquals(0, otherOwner.count());
                assertEquals(0, testBackend.count());
                assertTrue(otherOwner.append(record(7, true, "other owner")));
                db.clearArchive();
                assertEquals(0, db.count());
                assertEquals(1, otherOwner.count());
            }
        } finally { remove(dir); }
    }

    @Test
    public void revisionAndArchiveLimitsKeepNewestRecords() {
        File dir = directory();
        try (HistoryDatabase db = new HistoryDatabase(context(), dir, 1, false)) {
            for (int i = 0; i < 40; i++) assertTrue(db.append(record(7, false, "version " + i)));
            assertEquals(32, db.count());
            List<HistoryDatabase.Record> versions = db.recent("", 42, 100);
            assertEquals("version 39", versions.get(0).text);
            assertEquals("version 8", versions.get(31).text);
            db.clearArchive();
            for (int i = 1; i <= HistoryDatabase.MAX_RECORDS + 2; i++) db.append(record(i, true, "message " + i));
            assertEquals(HistoryDatabase.MAX_RECORDS, db.count());
            assertEquals(HistoryDatabase.MAX_RECORDS + 2, db.recent("", 0, 1).get(0).messageId);
            assertTrue(db.recent("message 1", 0, 100).stream().noneMatch(r -> r.messageId == 1));
            assertFalse(db.append(record(6000, true, new String(new char[HistoryDatabase.MAX_CONTENT_BYTES]))));
        } finally { remove(dir); }
    }

    @Test
    public void searchTreatsWildcardsLiterallyAndPreservesFormatting() {
        File dir = directory();
        try (HistoryDatabase db = new HistoryDatabase(context(), dir, 1, false)) {
            byte[] formatting = {1, 2, 3};
            HistoryDatabase.Record formatted = new HistoryDatabase.Record(42, 1, 50, 5, 1000, false, "100%_\\😀", formatting);
            formatting[0] = 9;
            assertTrue(db.append(formatted));
            assertTrue(db.append(record(2, true, "100 percent")));
            List<HistoryDatabase.Record> result = db.recent("%_\\", 42, 100);
            assertEquals(1, result.size());
            assertEquals("100%_\\😀", result.get(0).text);
            assertArrayEquals(new byte[]{1, 2, 3}, result.get(0).formatting);
            assertEquals(5, result.get(0).topicId);
            assertTrue(db.recent("", 43, 100).isEmpty());
            assertTrue(db.append(record(3, true, "ПрИвЕт, Café")));
            assertEquals(1, db.recent("привет", 0, 100).size());
            assertEquals(1, db.recent("CAFE\u0301", 0, 100).size());
            db.clearArchive();
            assertEquals(0, db.count());
        } finally { remove(dir); }
    }

    @Test
    public void largeContentHasAnAggregateBudgetAndUiThreadIoIsRejected() {
        File dir = directory();
        try (HistoryDatabase db = new HistoryDatabase(context(), dir, 1, false)) {
            String text = new String(new char[60000]).replace('\0', 'x');
            for (int i = 1; i <= 300; i++) db.append(record(i, true, text));
            assertTrue("Content budget must evict older records before count limit", db.count() < 300);
            assertEquals(300, db.recent("", 0, 1).get(0).messageId);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                try { db.count(); fail("Database IO was allowed on UI thread"); }
                catch (IllegalStateException expected) { assertTrue(expected.getMessage().contains("UI thread")); }
            });
        } finally { remove(dir); }
    }

    private void remove(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) remove(child);
        }
        assertTrue(!file.exists() || file.delete());
    }
}
