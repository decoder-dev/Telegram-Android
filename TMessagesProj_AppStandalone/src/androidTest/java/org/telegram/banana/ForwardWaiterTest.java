package org.telegram.banana;

import androidx.test.platform.app.InstrumentationRegistry;
import com.radolyn.ayugram.easy.DummyFileLoadWaiter;
import com.radolyn.ayugram.easy.DummyFileUploadWaiter;
import com.radolyn.ayugram.easy.DummyMessageWaiter;
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

    private static class Messages extends DummyMessageWaiter {
        Messages(int count) { super(0, count); }
        Messages(DummyMessageWaiter batch) { super(0, 1, false, batch); }
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

            Downloads failed = new Downloads(caller);
            failed.didReceivedNotification(NotificationCenter.fileLoadFailed, 1, FileLoader.getAttachFileName(size));
            assertFalse(failed.completed());
            failed.didReceivedNotification(NotificationCenter.fileLoadFailed, 0, FileLoader.getAttachFileName(size));
            assertTrue(failed.completed());
            try {
                failed.await();
                fail("A failed download must stop the forwarding batch");
            } catch (IllegalStateException expected) { assertEquals("Forward download failed", expected.getMessage()); }
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
            try {
                waiter.await();
                fail("A failed upload must stop the forwarding batch");
            } catch (IllegalStateException expected) { assertEquals("Forward upload failed", expected.getMessage()); }
        });
    }

    @Test
    public void sendErrorStopsOnlyTheRegisteredSend() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try (Messages waiter = new Messages(1)) {
                waiter.onMessageCreated(-42);
                waiter.didReceivedNotification(NotificationCenter.messageSendError, 0, -43);
                assertFalse(waiter.completed());
                waiter.didReceivedNotification(NotificationCenter.messageSendError, 0, -42);
                try {
                    waiter.await();
                    fail("A registered send failure must stop the queue");
                } catch (IllegalStateException expected) { assertEquals("Forward send failed", expected.getMessage()); }
            }
        });
    }

    @Test
    public void scheduledBatchWaitsForEveryOwnServerConfirmation() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try (Messages waiter = new Messages(2)) {
                waiter.onMessageCreated(-42);
                waiter.onMessageCreated(-43);
                waiter.onMessageCreated(-43);
                waiter.didReceivedNotification(NotificationCenter.messageReceivedByServer, 0, -99);
                waiter.didReceivedNotification(NotificationCenter.messageReceivedByAck, 0, -42);
                assertFalse(waiter.completed());
                waiter.didReceivedNotification(NotificationCenter.messageReceivedByServer, 0, -42);
                assertFalse(waiter.completed());
                waiter.didReceivedNotification(NotificationCenter.messageReceivedByServer, 1, -43);
                assertFalse(waiter.completed());
                waiter.didReceivedNotification(NotificationCenter.messageReceivedByServer, 0, -43);
                assertTrue(waiter.completed());
                waiter.await();
            }
        });
    }

    @Test
    public void albumFailureStopsTheNextItemAndDeferredWork() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try (Messages album = new Messages(2); Messages first = new Messages(album)) {
                first.onMessageCreated(-42);
                assertTrue(first.completed());
                assertFalse(album.completed());
                album.didReceivedNotification(NotificationCenter.messageSendError, 0, -42);
                try (Messages next = new Messages(album)) {
                    assertFalse(next.isActive());
                    try { next.await(); fail("A failed album must reject its next item"); }
                    catch (IllegalStateException expected) { }
                }
                assertFalse(album.isActive());
            }
            Messages closed = new Messages(1);
            closed.close();
            assertFalse("A late payment/upload callback must not restart a closed send", closed.isActive());
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

    @Test
    public void explicitLocalAttachmentIsUsedBeforeDisposableCache() throws Exception {
        java.io.File attached = java.io.File.createTempFile("forward-fixture-", ".bin",
                InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir());
        try {
            try (java.io.FileOutputStream output = new java.io.FileOutputStream(attached)) { output.write(1); }
            java.util.concurrent.atomic.AtomicReference<Downloads> subscribed = new java.util.concurrent.atomic.AtomicReference<>();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                TLRPC.TL_message raw = new TLRPC.TL_message();
                raw.id = 1; raw.message = ""; raw.attachPath = attached.getAbsolutePath();
                raw.peer_id = new TLRPC.TL_peerUser(); raw.peer_id.user_id = 42;
                MessageObject message = new MessageObject(0, raw, false, false);
                assertEquals(attached, com.radolyn.ayugram.AyuUtils.getLocalMessageFile(message));
                ArrayList<MessageObject> messages = new ArrayList<>(); messages.add(message);
                Downloads waiter = new Downloads(messages);
                subscribed.set(waiter);
                waiter.subscribe();
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> assertTrue(subscribed.get().completed()));
        } finally { attached.delete(); }
    }

    @Test
    public void copiedDocumentsKeep64BitSizeAndOwnTheirAttributeList() throws Exception {
        java.lang.reflect.Method mapper = com.radolyn.ayugram.AyuForwarder.class.getDeclaredMethod(
                "mapDocument", int.class, TLRPC.Document.class, java.io.File.class);
        mapper.setAccessible(true);
        TLRPC.TL_document source = new TLRPC.TL_document();
        source.mime_type = "application/octet-stream";
        source.attributes.add(new TLRPC.TL_documentAttributeFilename());
        long bytes = (long) Integer.MAX_VALUE + 1234;
        java.io.File synthetic = new java.io.File("/synthetic/forward.bin") {
            @Override public long length() { return bytes; }
        };
        TLRPC.Document copy = (TLRPC.Document) mapper.invoke(null, 0, source, synthetic);
        assertEquals(bytes, copy.size);
        assertNotSame(source.attributes, copy.attributes);
        copy.attributes.clear();
        assertEquals(1, source.attributes.size());
    }

    @Test
    public void corruptSecondPhotoIsRejectedBeforeAnAlbumCreatesAnyOutgoingMessages() throws Exception {
        android.content.Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        java.io.File valid = java.io.File.createTempFile("forward-valid-", ".png", context.getCacheDir());
        java.io.File corrupt = java.io.File.createTempFile("forward-corrupt-", ".jpg", context.getCacheDir());
        java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
        Thread worker = null;
        try {
            android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(8, 8, android.graphics.Bitmap.Config.ARGB_8888);
            try (java.io.FileOutputStream output = new java.io.FileOutputStream(valid)) {
                assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output));
            } finally { bitmap.recycle(); }
            try (java.io.FileOutputStream output = new java.io.FileOutputStream(corrupt)) { output.write(1); }
            ArrayList<MessageObject> photos = new ArrayList<>();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                assertFalse("Use an unregistered emulator", org.telegram.messenger.UserConfig.getInstance(0).isClientActivated());
                for (java.io.File file : new java.io.File[]{valid, corrupt}) {
                    TLRPC.TL_message raw = new TLRPC.TL_message();
                    raw.id = photos.size() + 1; raw.message = ""; raw.attachPath = file.getAbsolutePath();
                    raw.peer_id = new TLRPC.TL_peerUser(); raw.peer_id.user_id = 42;
                    raw.ayuNoforwards = true; raw.grouped_id = 55; raw.flags |= 131072;
                    raw.media = new TLRPC.TL_messageMediaPhoto(); raw.media.photo = new TLRPC.TL_photo();
                    photos.add(new MessageObject(0, raw, false, false));
                }
            });
            int before = org.telegram.messenger.UserConfig.getInstance(0).lastSendMessageId;
            worker = new Thread(() -> {
                try { com.radolyn.ayugram.AyuForwarder.forwardMessages(0, photos, 42, true, false, true, 0, null); }
                catch (Throwable error) { failure.set(error); }
            });
            worker.start(); worker.join(15000);
            assertFalse("Photo preflight must finish without waiting for a send", worker.isAlive());
            assertTrue("The corrupt photo must fail decoding", failure.get() instanceof IllegalStateException);
            assertEquals("Preflight must run before the first outgoing message", before,
                    org.telegram.messenger.UserConfig.getInstance(0).lastSendMessageId);
        } finally {
            if (worker != null && worker.isAlive()) { worker.interrupt(); worker.join(5000); }
            valid.delete(); corrupt.delete();
        }
    }
}
