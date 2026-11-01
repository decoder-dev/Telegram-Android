package org.telegram.banana;

import android.content.*;
import android.content.pm.PackageManager;
import android.os.CancellationSignal;
import androidx.test.platform.app.InstrumentationRegistry;
import com.radolyn.ayugram.easy.DummyMessageWaiter;
import org.junit.Before;
import org.junit.Test;
import org.telegram.messenger.*;
import org.telegram.messenger.banana.*;
import org.telegram.tgnet.TLRPC;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

public class AccountLifecycleTest {
    @Before public void initialize() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            ApplicationLoader.postInitApplication();
            for(int i=0;i<UserConfig.MAX_ACCOUNT_COUNT;i++) assertEquals("Disposable emulator only",0,UserConfig.getInstance(i).getClientUserId());
        });
    }

    @Test public void clearedAccountLifetimeCannotBeReusedEvenWithTheSameUserId() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            AccountScope old=AccountScope.capture(0), other=AccountScope.capture(1);
            DummyMessageWaiter send=new DummyMessageWaiter(0);
            assertTrue(old.isCurrent()); assertTrue(send.isActive());
            AccountScope.invalidate(0);
            assertFalse(old.isCurrent()); assertFalse(send.isActive());
            assertTrue(AccountScope.capture(0).isCurrent()); assertTrue(other.isCurrent());
            send.close();
        });
    }

    @Test public void logoutClearsAliasesAndVisualPhoneWithoutRemovingAppearanceChoices() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            SharedPreferences prefs=MessagesController.getMainSettings(0);
            List<String> keys=Arrays.asList("bananaLocalGroupAlias_-42",BananaConfig.StringValue.VISUAL_PHONE_NUMBER.key,
                    BananaConfig.Option.SHOW_PROFILE_ID.key,BananaConfig.Option.MESSAGE_BOOKMARKS.key,"bananaMessageBookmarkEntries");
            Map<String,?> old=prefs.getAll();
            try {
                BananaHooks.setLocalGroupAlias(0,-42,"Private alias");
                BananaConfig.setString(0,BananaConfig.StringValue.VISUAL_PHONE_NUMBER,"123456789");
                BananaConfig.set(0,BananaConfig.Option.SHOW_PROFILE_ID,true);
                BananaConfig.set(0,BananaConfig.Option.MESSAGE_BOOKMARKS,true);
                BananaHooks.addBookmark(0,-42,7);
                BananaHooks.clearAccountLocalData(0);
                assertNull(BananaHooks.getLocalGroupAlias(0,-42));
                assertEquals("",BananaConfig.getString(0,BananaConfig.StringValue.VISUAL_PHONE_NUMBER));
                assertTrue(BananaConfig.get(0,BananaConfig.Option.SHOW_PROFILE_ID));
                assertTrue(BananaHooks.getBookmarks(0).isEmpty());
            } finally {
                SharedPreferences.Editor restore=prefs.edit();
                for(String key:keys) {restore.remove(key);Object value=old.get(key);if(value instanceof String)restore.putString(key,(String)value);else if(value instanceof Boolean)restore.putBoolean(key,(Boolean)value);}
                restore.apply();
            }
        });
    }

    @Test public void conversionDoesNotDeliverMediaToAReusedAccountLifetime() throws Exception {
        File input=new File(ApplicationLoader.applicationContext.getCacheDir(),"audit-convert-"+UUID.randomUUID()+".mp3");
        try(InputStream source=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("conversion-music.mp3");OutputStream target=new FileOutputStream(input)) {
            byte[] bytes=new byte[8192];int count;while((count=source.read(bytes))!=-1)target.write(bytes,0,count);
        }
        CountDownLatch done=new CountDownLatch(1);AtomicReference<BananaMediaConverter.Result> answer=new AtomicReference<>();
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                TLRPC.TL_message raw=new TLRPC.TL_message();raw.id=7;raw.message="";raw.attachPath=input.getAbsolutePath();
                raw.peer_id=new TLRPC.TL_peerUser();raw.peer_id.user_id=42;
                raw.media=new TLRPC.TL_messageMediaDocument();raw.media.document=new TLRPC.TL_document();raw.media.document.file_reference=new byte[0];
                MessageObject message=new MessageObject(0,raw,false,false);
                BananaMediaConverter.convertMessage(message,MediaConversionPolicy.Kind.AUDIO_TO_VOICE,0,0,result->{answer.set(result);done.countDown();});
                AccountScope.invalidate(0);
            });
            assertTrue(done.await(30,TimeUnit.SECONDS));assertNull(answer.get());assertTrue(input.isFile());
        } finally {input.delete();}
    }

    @Test public void deniedPhonePermissionDoesNotAccessTelephonyServices() throws Exception {
        Context denied=new ContextWrapper(ApplicationLoader.applicationContext) {
            @Override public int checkSelfPermission(String permission) {return PackageManager.PERMISSION_DENIED;}
            @Override public Object getSystemService(String name) {throw new AssertionError("Telephony accessed without permission");}
        };
        Method method=SMSJobController.class.getDeclaredMethod("getSIMs",Context.class);method.setAccessible(true);
        assertTrue(((List<?>)method.invoke(null,denied)).isEmpty());
    }

    @Test public void invalidSmsServiceAccountDoesNotIndexTheInstanceArray() {
        SMSJobsNotification service=new SMSJobsNotification();
        assertEquals(android.app.Service.START_NOT_STICKY,service.onStartCommand(new Intent().putExtra("account",-1),0,1));
        assertFalse(SMSJobsNotification.check(-1));
    }

    @Test public void smsServiceIsInternalAndUsesItsMessagingForegroundType() throws Exception {
        Context context=ApplicationLoader.applicationContext;
        android.content.pm.ServiceInfo service=context.getPackageManager().getServiceInfo(new ComponentName(context,SMSJobsNotification.class),0);
        assertFalse(service.exported);
        assertNotEquals(0,service.getForegroundServiceType() & android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING);
        assertEquals(0,service.getForegroundServiceType() & android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
    }
}
