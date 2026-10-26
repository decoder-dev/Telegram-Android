package org.telegram.messenger.banana;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import static org.junit.Assert.*;

public class SavedAttachmentsTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void openedSourceSurvivesTelegramCacheUnlinkAndCopyCommitsAtomically() throws Exception {
        File source = folder.newFile("source");
        Files.write(source.toPath(), new byte[]{1, 2, 3, 4});
        FileInputStream input = new FileInputStream(source);
        assertTrue(source.delete());
        SavedAttachments archive = new SavedAttachments(folder.newFolder("archive"), 64, 128);
        File saved = archive.save(input, "JPG", 4);
        assertTrue(saved.getName().endsWith(".jpg"));
        assertArrayEquals(new byte[]{1, 2, 3, 4}, Files.readAllBytes(saved.toPath()));
        assertEquals(saved, archive.resolve(saved.getName()));
        assertNull(archive.resolve("../source"));
        assertTrue(archive.delete(saved.getName()));
        assertNull(archive.resolve(saved.getName()));
    }

    @Test
    public void truncatedOrOversizedStreamsLeaveNoPartialArchiveFile() throws Exception {
        File root = folder.newFolder("archive");
        SavedAttachments archive = new SavedAttachments(root, 8, 16);
        for (int size : new int[]{3, 9}) {
            try { archive.save(new ByteArrayInputStream(new byte[size]), "../../bad", 4); fail(); }
            catch (IOException expected) { }
        }
        assertEquals(1, root.list().length); // Lock file only.
        File saved = archive.save(new ByteArrayInputStream(new byte[8]), "../../bad", 8);
        assertTrue(saved.getName().endsWith(".bin"));
        try { archive.save(new ByteArrayInputStream(new byte[8]), "bin", 9); fail(); }
        catch (IOException expected) { }
    }

    @Test
    public void quotaIsCheckedAgainstActualBytesAndPruningKeepsReferencedFiles() throws Exception {
        File root = folder.newFolder("archive");
        SavedAttachments archive = new SavedAttachments(root, 8, 12);
        File first = archive.save(new ByteArrayInputStream(new byte[8]), "bin", 8);
        try { archive.save(new ByteArrayInputStream(new byte[5]), "bin", 0); fail(); }
        catch (IOException expected) { }
        assertTrue(first.isFile());
        assertEquals(0, archive.prune(Collections.singleton(first.getName()), Long.MAX_VALUE));
        assertEquals(1, archive.prune(Collections.emptySet(), Long.MAX_VALUE));
        assertFalse(first.exists());
    }

    @Test
    public void pruningCannotFollowASymlinkOutsideTheArchive() throws Exception {
        File root = folder.newFolder("archive");
        File outside = folder.newFile("private-original");
        Files.write(outside.toPath(), new byte[]{42});
        String name = "0123456789abcdef0123456789abcdef.bin";
        Files.createSymbolicLink(new File(root, name).toPath(), outside.toPath());
        SavedAttachments archive = new SavedAttachments(root, 64, 128);
        assertNull(archive.resolve(name));
        assertFalse(archive.delete(name));
        assertEquals(0, archive.prune(Collections.emptySet(), Long.MAX_VALUE));
        assertArrayEquals(new byte[]{42}, Files.readAllBytes(outside.toPath()));
    }

    @Test
    public void failingCopyClosesSourceAndPreservesExistingAttachments() throws Exception {
        File root = folder.newFolder("archive");
        SavedAttachments archive = new SavedAttachments(root, 64, 128);
        File previous = archive.save(new ByteArrayInputStream(new byte[]{7}), "bin", 1);
        boolean[] closed = {false};
        InputStream failing = new InputStream() {
            @Override public int read() throws IOException { throw new IOException("Synthetic IO failure"); }
            @Override public void close() { closed[0] = true; }
        };
        try { archive.save(failing, "bin", 0); fail(); }
        catch (IOException expected) { }
        assertTrue(closed[0]);
        assertArrayEquals(new byte[]{7}, Files.readAllBytes(previous.toPath()));
        assertEquals(2, root.list().length); // Lock and previous completed file.
    }
}
