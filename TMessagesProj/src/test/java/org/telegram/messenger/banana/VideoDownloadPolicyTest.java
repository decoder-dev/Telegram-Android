package org.telegram.messenger.banana;
import org.junit.Test;
import java.io.*;
import java.nio.file.*;
import static org.junit.Assert.*;
public class VideoDownloadPolicyTest {
    @Test public void acceptsOnlyPublicHttpsServiceUrls() {
        assertTrue(VideoDownloadPolicy.accepts("https://www.youtube.com/shorts/abc"));
        assertTrue(VideoDownloadPolicy.accepts("https://vm.tiktok.com/abc/"));
        assertTrue(VideoDownloadPolicy.accepts("https://www.tiktok.com/@example/video/12345"));
        for(String value:new String[]{"https://www.tiktok.com/@example","https://www.youtube.com/shorts/","https://www.tiktok.com/", "file:///etc/passwd","https://127.0.0.1/shorts/abc","https://youtube.com.evil/shorts/abc","https://user:pass@youtube.com/shorts/abc","http://youtube.com/shorts/abc","https://youtube.com:8443/shorts/abc"})assertFalse(value,VideoDownloadPolicy.accepts(value));
    }
    @Test public void doesNotPromotePartialOrAmbiguousOutput() throws Exception {
        File dir=Files.createTempDirectory("video-output").toFile();
        try {
            Files.write(new File(dir,"video.mp4.part").toPath(),new byte[]{1});
            try {VideoDownloadPolicy.completedVideo(dir);fail();}catch(IOException expected){}
            File video=new File(dir,"video.mp4");Files.write(video.toPath(),new byte[]{1,2});assertEquals(video,VideoDownloadPolicy.completedVideo(dir));
            Files.write(new File(dir,"video.second.mp4").toPath(),new byte[]{1});
            try {VideoDownloadPolicy.completedVideo(dir);fail();}catch(IOException expected){}
        } finally {for(File file:dir.listFiles())file.delete();dir.delete();}
    }
    @Test public void rejectsOversizedFinalVideo() throws Exception {
        File dir=Files.createTempDirectory("video-size").toFile();File video=new File(dir,"video.mp4");
        try(RandomAccessFile output=new RandomAccessFile(video,"rw")){output.setLength(VideoDownloadPolicy.MAX_BYTES+1);try{VideoDownloadPolicy.completedVideo(dir);fail();}catch(IOException expected){}}
        finally {video.delete();dir.delete();}
    }
    @Test public void cleanupDoesNotFollowLinksOutsideTheJobDirectory() throws Exception {
        Path job=Files.createTempDirectory("download-job"), other=Files.createTempDirectory("download-other");
        Path protectedFile=Files.write(other.resolve("keep.bin"),new byte[]{1,2,3});
        try {
            Files.createSymbolicLink(job.resolve("outside"),other);
            assertEquals(Long.MAX_VALUE,VideoDownloadPolicy.directoryBytes(job.toFile()));
            assertTrue(VideoDownloadPolicy.erase(job.toFile()));
            assertArrayEquals(new byte[]{1,2,3},Files.readAllBytes(protectedFile));
        } finally {VideoDownloadPolicy.erase(job.toFile());VideoDownloadPolicy.erase(other.toFile());}
    }
    @Test public void oversizedTemporaryFilesExhaustTheBudgetBeforePromotion() throws Exception {
        File dir=Files.createTempDirectory("download-budget").toFile();
        try {
            try(RandomAccessFile file=new RandomAccessFile(new File(dir,"video.part"),"rw")){file.setLength(VideoDownloadPolicy.MAX_BYTES*2+1);}
            assertEquals(Long.MAX_VALUE,VideoDownloadPolicy.directoryBytes(dir));
        } finally {assertTrue(VideoDownloadPolicy.erase(dir));}
    }
    @Test public void staleJobDirectoriesAreSweptButTheActiveJobAndOtherFilesStay() throws Exception {
        Path cache=Files.createTempDirectory("download-cache"), outside=Files.createTempDirectory("download-outside");
        try {
            Path stale=Files.createDirectory(cache.resolve(VideoDownloadPolicy.JOB_PREFIX+"old"));
            Files.write(stale.resolve("video.part"),new byte[]{1});
            Path active=Files.createDirectory(cache.resolve(VideoDownloadPolicy.JOB_PREFIX+"now"));
            Files.write(active.resolve("video.part"),new byte[]{2});
            Path unrelated=Files.write(cache.resolve("other.bin"),new byte[]{3});
            Path keep=Files.write(outside.resolve("keep.bin"),new byte[]{4});
            Files.createSymbolicLink(cache.resolve(VideoDownloadPolicy.JOB_PREFIX+"link"),outside);
            assertEquals(1,VideoDownloadPolicy.sweepStale(cache.toFile(),VideoDownloadPolicy.JOB_PREFIX+"now"));
            assertFalse(Files.exists(stale));
            assertTrue(Files.exists(active.resolve("video.part")));
            assertTrue(Files.exists(unrelated));
            assertArrayEquals(new byte[]{4},Files.readAllBytes(keep));
            assertEquals(0,VideoDownloadPolicy.sweepStale(null,"x"));
        } finally {VideoDownloadPolicy.erase(cache.toFile());VideoDownloadPolicy.erase(outside.toFile());}
    }
    @Test public void normalTemporaryFilesAreCountedAndCanBeRemovedTwice() throws Exception {
        File dir=Files.createTempDirectory("download-normal").toFile();
        Files.write(new File(dir,"video.part").toPath(),new byte[]{1,2,3});
        assertEquals(3,VideoDownloadPolicy.directoryBytes(dir));
        assertTrue(VideoDownloadPolicy.erase(dir));
        assertTrue(VideoDownloadPolicy.erase(dir));
    }
}
