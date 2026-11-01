package org.telegram.banana;

import android.content.*;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.ui.bots.BotDownloads;
import java.lang.reflect.Constructor;
import java.util.*;
import static org.junit.Assert.*;

public class BotDownloadsTest {
    @Before public void initialize() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(ApplicationLoader::postInitApplication);
    }

    private BotDownloads downloads(Context context, long botId) throws Exception {
        Constructor<BotDownloads> constructor=BotDownloads.class.getDeclaredConstructor(Context.class,int.class,long.class);
        constructor.setAccessible(true);
        return constructor.newInstance(context,0,botId);
    }

    @Test public void savingOneBotDoesNotEraseOtherBotDownloads() throws Exception {
        String prefix="audit-bot-"+UUID.randomUUID()+"-";
        Context isolated=new ContextWrapper(ApplicationLoader.applicationContext) {
            @Override public SharedPreferences getSharedPreferences(String name,int mode) {return super.getSharedPreferences(prefix+name,mode);}
        };
        SharedPreferences prefs=isolated.getSharedPreferences("botdownloads_0",0);
        try {
            Set<String> other=new HashSet<>(Collections.singleton("other-bot-download"));
            prefs.edit().putStringSet("42",other).commit();
            downloads(isolated,43).save();
            assertEquals(other,prefs.getStringSet("42",null));
            assertEquals(Collections.emptySet(),prefs.getStringSet("43",null));
        } finally {isolated.deleteSharedPreferences(prefix+"botdownloads_0");}
    }

    @Test public void vanishedSystemDownloadIsCancelledWithoutPollingForever() throws Exception {
        String prefix="audit-missing-bot-"+UUID.randomUUID()+"-";
        Context isolated=new ContextWrapper(ApplicationLoader.applicationContext) {
            @Override public SharedPreferences getSharedPreferences(String name,int mode) {return super.getSharedPreferences(prefix+name,mode);}
        };
        BotDownloads owner=downloads(isolated,43);
        BotDownloads.FileDownload file=owner.new FileDownload(new JSONObject());
        file.id=Long.MAX_VALUE;
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                owner.getFiles().add(file);
                file.getProgress();
                assertTrue(file.cancelled);assertNull(file.id);assertFalse(owner.isDownloading());
            });
            assertTrue(isolated.getSharedPreferences("botdownloads_0",0).getStringSet("43",Collections.emptySet()).isEmpty());
        } finally {isolated.deleteSharedPreferences(prefix+"botdownloads_0");}
    }
}
