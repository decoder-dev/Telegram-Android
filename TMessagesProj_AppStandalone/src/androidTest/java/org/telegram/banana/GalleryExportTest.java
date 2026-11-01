package org.telegram.banana;

import android.content.*;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.provider.MediaStore;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Before;
import org.junit.Test;
import org.telegram.messenger.*;
import org.telegram.messenger.banana.BananaConfig;
import java.io.*;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

public class GalleryExportTest {
    @Before public void initialize() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            ApplicationLoader.postInitApplication();
            for(int i=0;i<UserConfig.MAX_ACCOUNT_COUNT;i++)assertEquals("Disposable emulator only",0,UserConfig.getInstance(i).getClientUserId());
        });
    }

    private File fixture() throws Exception {
        File file=new File(AndroidUtilities.getCacheDir(),"audit-gallery-"+UUID.randomUUID()+".mp4");
        try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("conversion-video.mp4");OutputStream out=new FileOutputStream(file)) {
            byte[] bytes=new byte[8192];int count;while((count=in.read(bytes))!=-1)out.write(bytes,0,count);
        }
        return file;
    }

    @Test public void missingInputReportsFailureExactlyOnce() throws Exception {
        CountDownLatch done=new CountDownLatch(1);AtomicInteger failures=new AtomicInteger(),successes=new AtomicInteger();
        MediaController.saveFile(null,ApplicationLoader.applicationContext,1,"absent.mp4","video/mp4",uri->successes.incrementAndGet(),false,
                ()->{failures.incrementAndGet();done.countDown();});
        assertTrue(done.await(5,TimeUnit.SECONDS));assertEquals(1,failures.get());assertEquals(0,successes.get());
    }

    @Test public void copyFailureCompletesAndDoesNotLeaveAPendingGalleryRow() throws Exception {
        File directory=new File(AndroidUtilities.getCacheDir(),"audit-invalid-"+UUID.randomUUID()+".mp4");assertTrue(directory.mkdir());
        String name="Audit-failed-"+UUID.randomUUID()+".mp4";
        CountDownLatch done=new CountDownLatch(1);AtomicInteger failures=new AtomicInteger();
        try {
            MediaController.saveFile(directory.getAbsolutePath(),ApplicationLoader.applicationContext,1,name,"video/mp4",uri->fail("Directory exported as a file"),false,
                    ()->{failures.incrementAndGet();done.countDown();});
            assertTrue("Failed gallery writes must finish the owner job",done.await(10,TimeUnit.SECONDS));
            assertEquals(1,failures.get());assertEquals(0,rows(name));
        } finally {directory.delete();}
    }

    @Test public void backgroundExportHonorsNameAndDoesNotCreateUiResources() throws Exception {
        File source=fixture();String name="Audit-success-"+UUID.randomUUID()+".mp4";
        AtomicReference<Uri> saved=new AtomicReference<>();AtomicInteger failures=new AtomicInteger();CountDownLatch done=new CountDownLatch(1);
        SharedPreferences prefs=MessagesController.getMainSettings(0);String key=BananaConfig.Option.SIGN_DOWNLOADED_MEDIA.key;Object old=prefs.getAll().get(key);
        Context background=new ContextWrapper(ApplicationLoader.applicationContext) {
            @Override public Resources getResources(){throw new AssertionError("showProgress=false requested dialog resources");}
        };
        try {
            prefs.edit().putBoolean(key,false).apply();
            MediaController.saveFile(source.getAbsolutePath(),background,1,name,"video/mp4",uri->{saved.set(uri);done.countDown();},false,
                    ()->{failures.incrementAndGet();done.countDown();});
            assertTrue(done.await(15,TimeUnit.SECONDS));assertEquals(0,failures.get());assertNotNull(saved.get());
            try(Cursor cursor=ApplicationLoader.applicationContext.getContentResolver().query(saved.get(),new String[]{MediaStore.MediaColumns.DISPLAY_NAME},null,null,null)) {
                assertNotNull(cursor);assertTrue(cursor.moveToFirst());assertEquals(name,cursor.getString(0));
            }
            try(InputStream exported=ApplicationLoader.applicationContext.getContentResolver().openInputStream(saved.get());InputStream original=new FileInputStream(source)) {
                assertArrayEquals(digest(original),digest(exported));
            }
        } finally {
            if(saved.get()!=null)ApplicationLoader.applicationContext.getContentResolver().delete(saved.get(),null,null);
            source.delete();SharedPreferences.Editor restore=prefs.edit().remove(key);if(old instanceof Boolean)restore.putBoolean(key,(Boolean)old);restore.apply();
        }
    }

    @Test public void cancelledExportDoesNotPublishAPartialFile() throws Exception {
        File source=fixture();String name="Audit-cancel-"+UUID.randomUUID()+".mp4";
        CountDownLatch done=new CountDownLatch(1);AtomicInteger successes=new AtomicInteger(),failures=new AtomicInteger();
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                CancellationSignal signal=MediaController.saveFile(source.getAbsolutePath(),ApplicationLoader.applicationContext,1,name,"video/mp4",
                        uri->{successes.incrementAndGet();done.countDown();},false,()->{failures.incrementAndGet();done.countDown();});
                signal.cancel();
            });
            assertTrue(done.await(10,TimeUnit.SECONDS));assertEquals(0,successes.get());assertEquals(1,failures.get());assertEquals(0,rows(name));
        } finally {source.delete();}
    }

    private int rows(String name) {
        try(Cursor cursor=ApplicationLoader.applicationContext.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.MediaColumns._ID},MediaStore.MediaColumns.DISPLAY_NAME+"=?",new String[]{name},null)) {
            return cursor==null?0:cursor.getCount();
        }
    }
    private byte[] digest(InputStream stream) throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");byte[] bytes=new byte[8192];int count;
        while((count=stream.read(bytes))!=-1)digest.update(bytes,0,count);return digest.digest();
    }
}
