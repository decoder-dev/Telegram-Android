package org.telegram.banana;

import androidx.test.platform.app.InstrumentationRegistry;
import com.radolyn.ayugram.easy.DummyFileLoadWaiter;
import com.radolyn.ayugram.easy.DummyFileUploadWaiter;
import com.radolyn.ayugram.easy.EasyWaiter;
import org.junit.Test;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.tgnet.TLRPC;
import java.util.ArrayList;
import static org.junit.Assert.*;

/** Synthetic completion events only; never starts downloads or sends messages. */
public class ForwardWaiterTest {
    // An isolated event ID: real upload notifications also reach Telegram controllers.
    private static final int TEST_EVENT = -117001;
    private static class Events extends EasyWaiter {
        int delivered;
        Events() { super(0); notifications.add(TEST_EVENT); }
        @Override public void didReceivedNotification(int id, int account, Object... args) { delivered++; }
    }
    private static class Downloads extends DummyFileLoadWaiter {
        Downloads(ArrayList<MessageObject> messages) { super(0, messages); }
        boolean completed() { return latch.getCount() == 0; }
    }

    private static class Upload extends DummyFileUploadWaiter {
        Upload(String path) { super(0, path); }
        boolean completed() { return latch.getCount() == 0; }
    }

    @Test
    public void photoCompletionKeepsCallerListAndDeduplicatesSharedFiles() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            TLRPC.TL_photoSize size = new TLRPC.TL_photoSize();
            size.type = "x";
            size.w = size.h = AndroidUtilities.getPhotoSize();
            size.location = new TLRPC.TL_fileLocationToBeDeprecated();
            size.location.volume_id = -987654321123456L;
            size.location.local_id = 17;
            TLRPC.TL_photo photo = new TLRPC.TL_photo();
            photo.id = 987654321;
            photo.sizes.add(size);
            TLRPC.TL_message raw = new TLRPC.TL_message();
            raw.id = 1;
            raw.message = "";
            raw.peer_id = new TLRPC.TL_peerUser();
            raw.peer_id.user_id = 42;
            raw.media = new TLRPC.TL_messageMediaPhoto();
            raw.media.photo = photo;
            MessageObject message = new MessageObject(0, raw, false, false);
            ArrayList<MessageObject> caller = new ArrayList<>();
            caller.add(message);
            caller.add(message);
            Downloads waiter = new Downloads(caller);
            assertFalse(waiter.completed());
            waiter.didReceivedNotification(NotificationCenter.fileLoaded, 0, "unrelated.jpg");
            assertFalse(waiter.completed());
            waiter.didReceivedNotification(NotificationCenter.fileLoaded, 0, FileLoader.getAttachFileName(size));
            assertTrue(waiter.completed());
            assertEquals(2, caller.size());
        });
    }

    @Test
    public void uploadRequiresExactPathAndIgnoresNullOrSubstringEvents() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Upload waiter = new Upload("/cache/fixture/video.mp4");
            waiter.didReceivedNotification(NotificationCenter.fileUploaded, 0, (Object) null);
            waiter.didReceivedNotification(NotificationCenter.fileUploaded, 0, "video.mp4");
            waiter.didReceivedNotification(NotificationCenter.fileUploaded, 0, "/cache/fixture/video.mp4-other");
            assertFalse(waiter.completed());
            waiter.didReceivedNotification(NotificationCenter.fileUploadFailed, 0, "/cache/fixture/video.mp4");
            assertTrue(waiter.completed());
        });
    }

    @Test
    public void blockingUiWaitFailsAndRemovesObserver() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Events waiter = new Events();
            waiter.subscribe();
            try {
                waiter.await();
                fail("An active transfer must never block the main thread");
            } catch (IllegalStateException expected) { }
            NotificationCenter.getInstance(0).postNotificationName(TEST_EVENT);
            assertEquals(0, waiter.delivered);
        });
    }

    @Test
    public void interruptedSubscriptionCannotLeaveObserverBehind() throws Exception {
        Events waiter = new Events();
        java.util.concurrent.atomic.AtomicBoolean interrupted = new java.util.concurrent.atomic.AtomicBoolean();
        Thread worker = new Thread(() -> {
            Thread.currentThread().interrupt();
            try { waiter.subscribe(); }
            catch (IllegalStateException expected) { interrupted.set(Thread.currentThread().isInterrupted()); }
        });
        worker.start(); worker.join(5000);
        assertFalse("Cleanup must not wait for the UI", worker.isAlive());
        assertTrue("Interrupt must survive cleanup", interrupted.get());
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            NotificationCenter.getInstance(0).postNotificationName(TEST_EVENT);
            assertEquals(0, waiter.delivered);
        });
    }
}
