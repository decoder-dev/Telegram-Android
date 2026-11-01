package org.telegram.banana;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.CancellationSignal;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Before;
import org.junit.Test;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.banana.AccountScope;
import org.telegram.messenger.banana.BananaVideoDownloadService;
import java.lang.reflect.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

public class VideoDownloadLifecycleTest {
    @Before public void initialize() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            ApplicationLoader.postInitApplication();assertEquals("Disposable emulator only",0,UserConfig.getInstance(0).getClientUserId());
        });
    }
    private Object download() throws Exception {
        Class<?> type=Class.forName(BananaVideoDownloadService.class.getName()+"$Download");
        Constructor<?> constructor=type.getDeclaredConstructor(AccountScope.class);constructor.setAccessible(true);
        return constructor.newInstance(AccountScope.capture(0));
    }
    private Object field(Object owner,String name) throws Exception {
        Field field=owner.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(owner);
    }

    @Test public void oldCancellationCannotCancelTheNextDownload() throws Exception {
        Object old=download(), next=download();
        CancellationSignal oldExport=new CancellationSignal(),nextExport=new CancellationSignal();
        Field export=old.getClass().getDeclaredField("export");export.setAccessible(true);export.set(old,oldExport);export.set(next,nextExport);
        Method cancel=old.getClass().getDeclaredMethod("cancel");cancel.setAccessible(true);cancel.invoke(old);
        assertTrue(oldExport.isCanceled());assertFalse(nextExport.isCanceled());
        assertTrue(((AtomicBoolean)field(old,"cancelled")).get());assertFalse(((AtomicBoolean)field(next,"cancelled")).get());
        assertNotEquals(field(old,"id"),field(next,"id"));
    }

    @Test public void schedulerTeardownRaceCompletesTheJobInsteadOfCrashingTheWorker() throws Exception {
        BananaVideoDownloadService service=new BananaVideoDownloadService();
        Method attach=ContextWrapper.class.getDeclaredMethod("attachBaseContext",Context.class);attach.setAccessible(true);attach.invoke(service,ApplicationLoader.applicationContext);
        ((ScheduledExecutorService)field(service,"timer")).shutdownNow();
        Object job=download();
        Method run=service.getClass().getDeclaredMethod("download",String.class,job.getClass());run.setAccessible(true);
        try {
            run.invoke(service,"https://www.youtube.com/shorts/fixture",job);
            assertTrue(((AtomicBoolean)field(job,"finished")).get());
        } finally {InstrumentationRegistry.getInstrumentation().runOnMainSync(service::onDestroy);}
    }
}
