package org.telegram.banana;

import android.net.Uri;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Before;
import org.junit.Test;
import org.telegram.messenger.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

public class CacheImportTest {
    @Before public void initialize() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(ApplicationLoader::postInitApplication);
    }

    @Test public void concurrentImportsKeepEachPayloadAndReserveDistinctNames() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(12);
        File directory = new File(AndroidUtilities.getCacheDir(), "audit-import-" + UUID.randomUUID());
        assertTrue(directory.mkdir());
        List<File> sources = new ArrayList<>(), results = new ArrayList<>();
        List<Future<String>> futures = new ArrayList<>();
        CountDownLatch start = new CountDownLatch(1);
        String name = "audit-same-" + UUID.randomUUID() + ".bin";
        try {
            for (int i = 0; i < 12; i++) {
                File parent = new File(directory, String.valueOf(i)); assertTrue(parent.mkdir());
                File source = new File(parent, name); sources.add(source);
                byte[] payload = new byte[65536]; Arrays.fill(payload, (byte) i); Files.write(source.toPath(), payload);
                futures.add(pool.submit(() -> { start.await(); return MediaController.copyFileToCache(Uri.fromFile(source), "bin"); }));
            }
            start.countDown();
            Set<String> paths = new HashSet<>();
            for (int i = 0; i < futures.size(); i++) {
                String path = futures.get(i).get(30, TimeUnit.SECONDS); assertNotNull(path);
                File result = new File(path); results.add(result);
                assertTrue("Imports overwrote a shared destination", paths.add(path));
                assertArrayEquals(Files.readAllBytes(sources.get(i).toPath()), Files.readAllBytes(result.toPath()));
            }
        } finally {
            start.countDown(); pool.shutdownNow(); pool.awaitTermination(30, TimeUnit.SECONDS);
            for (Future<String> future : futures) if (future.isDone() && !future.isCancelled()) {
                try { String path = future.get(); if (path != null) new File(path).delete(); } catch (ExecutionException ignored) { }
            }
            for (File source : sources) { source.delete(); source.getParentFile().delete(); }
            directory.delete();
        }
    }

    @Test public void sizeLimitIsExactAndRejectedImportLeavesNoPartialFile() throws Exception {
        File source = new File(AndroidUtilities.getCacheDir(), "audit-limit-" + UUID.randomUUID() + ".bin");
        byte[] bytes = new byte[32769]; new Random(7).nextBytes(bytes); Files.write(source.toPath(), bytes);
        File target = new File(AndroidUtilities.getSharingDirectory(), source.getName());
        try {
            assertNull(MediaController.copyFileToCache(Uri.fromFile(source), "bin", bytes.length - 1));
            assertFalse(target.exists());
            String path = MediaController.copyFileToCache(Uri.fromFile(source), "bin", bytes.length);
            assertNotNull(path); assertArrayEquals(bytes, Files.readAllBytes(new File(path).toPath()));
        } finally { target.delete(); source.delete(); }
    }

    @Test public void sizeLimitStillAppliesAfterTwoGiB() throws Exception {
        File source = new File(AndroidUtilities.getCacheDir(), "audit-large-" + UUID.randomUUID() + ".bin");
        File target = new File(AndroidUtilities.getSharingDirectory(), source.getName());
        long limit = (1L << 31) + 1024;
        try {
            try (RandomAccessFile sparse = new RandomAccessFile(source, "rw")) { sparse.setLength(limit + 1); }
            assertNull("The size counter overflowed above 2 GiB", MediaController.copyFileToCache(Uri.fromFile(source), "bin", limit));
            assertFalse("Oversized import left a partial file", target.exists());
        } finally { target.delete(); source.delete(); }
    }

    @Test public void privateFilesCannotBeImportedAndNamelessUrisAreHandled() throws Exception {
        File file = new File(ApplicationLoader.applicationContext.getFilesDir(), "audit-import-private-" + UUID.randomUUID());
        Files.write(file.toPath(), new byte[]{1});
        try {
            assertNull(MediaController.copyFileToCache(Uri.fromFile(file), "bin"));
            assertEquals("", MediaController.getFileName(Uri.parse("content://audit-provider-without-path")));
            assertEquals("filename", MediaController.getFileName(Uri.parse("filename")));
        } finally { file.delete(); }
    }
}
