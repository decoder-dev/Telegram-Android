package org.telegram.banana;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.telegram.messenger.banana.HistoryDatabase;
import java.io.File;
import java.util.List;
import java.util.UUID;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.FileOutputStream;
import androidx.core.content.FileProvider;
import org.telegram.messenger.banana.SavedAttachments;
import static org.junit.Assert.*;

/** Synthetic local databases only: never opens a user account, network or Telegram cache. */
public class HistoryDatabaseTest {
    @Test
    public void schemaOneMigratesWithoutLosingTextAndMediaOnlyRowsAreSupported() throws Exception {
        File dir = directory();
        assertTrue(dir.mkdirs());
        File oldFile = new File(dir, "1-production.sqlite");
        try {
            try (android.database.sqlite.SQLiteDatabase old = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(oldFile, null)) {
                old.execSQL("CREATE TABLE records (id INTEGER PRIMARY KEY AUTOINCREMENT,dialog_id INTEGER NOT NULL,message_id INTEGER NOT NULL," +
                        "author_id INTEGER NOT NULL,topic_id INTEGER NOT NULL,message_date INTEGER NOT NULL,deleted INTEGER NOT NULL," +
                        "text TEXT NOT NULL,formatting BLOB NOT NULL,saved_at INTEGER NOT NULL,byte_size INTEGER NOT NULL,search_text TEXT NOT NULL)");
                old.execSQL("CREATE INDEX message_revisions ON records(dialog_id,message_id,deleted,id)");
                old.execSQL("INSERT INTO records VALUES(1,42,7,50,0,1000,1,'before upgrade',X'',1000,28,'before upgrade')");
                old.setVersion(1);
            }
            try (HistoryDatabase db = new HistoryDatabase(context(), dir, 1, false)) {
                assertEquals(1, db.count());
                assertEquals("before upgrade", db.recent("", 0, 1).get(0).text);
                assertNull(db.recent("", 0, 1).get(0).attachment);
                assertTrue(db.append(new HistoryDatabase.Record(42, 8, 50, 0, 1000, true, "", null,
                        new HistoryDatabase.Attachment("document:123", 4, "text/plain", "Отчёт.txt"))));
                HistoryDatabase.Record media = db.recent("ОТЧЁТ", 0, 1).get(0);
                assertEquals("", media.text);
                assertEquals("Отчёт.txt", media.attachment.displayName);
                assertEquals("text/plain", media.attachment.mimeType);
                assertNull(media.attachment.storedName);
            }
        } finally { remove(dir); }
    }

    @Test
    public void fileProviderCanShareOnlyArchiveMediaAndNotSiblingDatabases() throws Exception {
        File root = new File(context().getNoBackupFilesDir(), "banana-history/attachments/fixture-" + UUID.randomUUID());
        File privateDb = new File(context().getNoBackupFilesDir(), "banana-history/private-fixture-" + UUID.randomUUID() + ".sqlite");
        try {
            File file = new SavedAttachments(root).save(new ByteArrayInputStream(new byte[]{1, 2, 3}), "bin", 3);
            android.net.Uri uri = FileProvider.getUriForFile(context(), context().getPackageName() + ".provider", file);
            try (InputStream stream = context().getContentResolver().openInputStream(uri)) {
                assertNotNull(stream);
                assertEquals(1, stream.read());
                assertEquals(2, stream.read());
                assertEquals(3, stream.read());
                assertEquals(-1, stream.read());
            }
            try (FileOutputStream output = new FileOutputStream(privateDb)) { output.write(42); }
            try { FileProvider.getUriForFile(context(), context().getPackageName() + ".provider", privateDb); fail("History databases must not be shareable"); }
            catch (IllegalArgumentException expected) { }
        } finally { remove(root); remove(privateDb); }
    }
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

    @Test
    public void versionTwoMigrationPreservesHistoryAndCreatesProactiveIndex() {
        File dir = directory();
        try {
            try (HistoryDatabase db = new HistoryDatabase(context(), dir, 1, false)) {
                assertTrue(db.append(record(7, true, "before upgrade")));
                android.database.sqlite.SQLiteDatabase sqlite = db.getWritableDatabase();
                sqlite.execSQL("DROP TABLE retained_media");
                sqlite.setVersion(2);
            }
            try (HistoryDatabase db = new HistoryDatabase(context(), dir, 1, false)) {
                assertEquals(1, db.count());
                assertEquals("before upgrade", db.recent("", 0, 1).get(0).text);
                try (android.database.Cursor cursor = db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM retained_media", null)) {
                    assertTrue(cursor.moveToFirst()); assertEquals(0, cursor.getInt(0));
                }
            }
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
