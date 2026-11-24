package org.telegram.messenger.banana;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import static org.junit.Assert.*;

/**
 * LRU eviction of the media archive (BUG-017). Same package as HistoryDatabase, so the
 * package-private linking layer is exercised directly. Synthetic local databases only: never
 * opens a user account, network or Telegram cache.
 */
public class HistoryEvictionTest {
    private static final String RECORD_COPY = "0123456789abcdef0123456789abcdef.bin";
    private static final String RETAINED_COPY = "fedcba9876543210fedcba9876543210.bin";

    @Test
    public void evictionFreesRetainedFirstThenOldestRecordCopiesAndKeepsText() throws Exception {
        File dir = directory();
        try (HistoryDatabase db = new HistoryDatabase(context(), dir, 1, false)) {
            HistoryDatabase.Attachment media = new HistoryDatabase.Attachment("document:1", 4, "text/plain", "a.txt");
            assertTrue(db.append(new HistoryDatabase.Record(42, 7, 50, 0, 1000, true, "keep me", null, media)));
            long row = db.recent("keep me", 0, 1).get(0).id;
            assertTrue(db.linkAttachment(row, RECORD_COPY, 100));
            assertFalse(db.canLinkAttachment(row));
            assertTrue(db.rememberMedia(new HistoryDatabase.Attachment("document:2", 4, "text/plain", "b.txt"), RETAINED_COPY, 50));

            // 60 bytes short: the 50-byte speculative copy goes first, then the record's copy.
            List<String> freed = db.evictOldestStoredMedia(60);
            assertEquals(Arrays.asList(RETAINED_COPY, RECORD_COPY), freed);

            HistoryDatabase.Record after = db.recent("keep me", 0, 1).get(0);
            assertEquals("keep me", after.text);
            assertNotNull(after.attachment);
            assertEquals("a.txt", after.attachment.displayName);
            assertNull(after.attachment.storedName);
            assertNull(db.storedMedia("document:2"));
            // The record is linkable again, so a later re-download can refill it.
            assertTrue(db.canLinkAttachment(row));

            assertTrue(db.evictOldestStoredMedia(0).isEmpty());
            assertTrue(db.evictOldestStoredMedia(10).isEmpty());
        } finally { remove(dir); }
    }

    @Test
    public void evictionCountsAFileLinkedFromBothTablesOnce() throws Exception {
        File dir = directory();
        try (HistoryDatabase db = new HistoryDatabase(context(), dir, 1, false)) {
            HistoryDatabase.Attachment media = new HistoryDatabase.Attachment("document:3", 4, "text/plain", "c.txt");
            assertTrue(db.append(new HistoryDatabase.Record(42, 9, 50, 0, 1000, true, "shared copy", null, media)));
            long row = db.recent("shared copy", 0, 1).get(0).id;
            assertTrue(db.linkAttachment(row, RECORD_COPY, 100));
            assertTrue(db.rememberMedia(media, RECORD_COPY, 100));

            // 100 bytes short, one file linked twice: freed once, counted once.
            List<String> freed = db.evictOldestStoredMedia(100);
            assertEquals(Arrays.asList(RECORD_COPY), freed);
            assertNull(db.storedMedia("document:3"));
        } finally { remove(dir); }
    }

    private Context context() { return InstrumentationRegistry.getInstrumentation().getTargetContext(); }
    private File directory() { return new File(context().getCacheDir(), "eviction-test-" + UUID.randomUUID()); }
    private void remove(File file) {
        File[] children = file.listFiles();
        if (children != null) for (File child : children) child.delete();
        file.delete();
    }
}
