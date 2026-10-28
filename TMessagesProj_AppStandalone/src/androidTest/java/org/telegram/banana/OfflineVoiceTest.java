package org.telegram.banana;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.telegram.messenger.*;
import org.telegram.messenger.banana.*;
import org.telegram.tgnet.TLRPC;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;
/** Explicit CI probe: downloads the public English model, transcribes only a public audio fixture. */
public class OfflineVoiceTest {
 @Test public void decodesOpusAndRecognizesOfflineWithoutAnAccount() throws Exception {
  assertTrue("Fresh emulator only", "ranchu".equals(android.os.Build.HARDWARE) || "goldfish".equals(android.os.Build.HARDWARE));
  android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
  assertEquals("YES", InstrumentationRegistry.getArguments().getString("banana_voice_probe"));
  instrumentation.runOnMainSync(ApplicationLoader::postInitApplication);
  for(int account=0;account<UserConfig.MAX_ACCOUNT_COUNT;account++)assertFalse(UserConfig.getInstance(account).isClientActivated());
  int previous=BananaConfig.transcriptionBackend(0);
  File file=new File(instrumentation.getTargetContext().getCacheDir(),"voice-fixture.ogg");
  try {
   BananaConfig.setTranscriptionBackend(0,2);
   CountDownLatch model=new CountDownLatch(1); AtomicReference<Boolean> ready=new AtomicReference<>(false);
   if(BananaOfflineVoice.isModelReady("en")){ready.set(true);model.countDown();}
   else BananaOfflineVoice.download("en",new BananaOfflineVoice.DownloadListener(){public void onProgress(long n,long total){}public void onDone(boolean ok){ready.set(ok);model.countDown();}});
   assertTrue("Model download must finish",model.await(6,TimeUnit.MINUTES));assertTrue(ready.get());
   try(InputStream input=instrumentation.getContext().getAssets().open("voice-fixture.ogg");OutputStream output=new FileOutputStream(file)){byte[] buffer=new byte[8192];int n;while((n=input.read(buffer))!=-1)output.write(buffer,0,n);}
   TLRPC.TL_message message=new TLRPC.TL_message();message.id=1;message.message="";message.date=1;message.peer_id=new TLRPC.TL_peerUser();message.peer_id.user_id=1;message.attachPath=file.getAbsolutePath();
   MessageObject object=new MessageObject(0,message,false,false);
   CountDownLatch recognized=new CountDownLatch(1);AtomicReference<String> text=new AtomicReference<>();
   BananaOfflineVoice.transcribe(object,result->{text.set(result);recognized.countDown();});
   assertTrue("Offline decode must finish",recognized.await(2,TimeUnit.MINUTES));assertNotNull(text.get());assertTrue(text.get(),text.get().toLowerCase(java.util.Locale.ROOT).contains("zero"));
  } finally {BananaConfig.setTranscriptionBackend(0,previous);file.delete();}
 }
}
