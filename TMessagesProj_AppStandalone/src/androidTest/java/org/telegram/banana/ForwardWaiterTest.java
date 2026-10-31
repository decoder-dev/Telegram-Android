package org.telegram.banana;

import androidx.test.platform.app.InstrumentationRegistry;
import com.radolyn.ayugram.easy.DummyFileLoadWaiter;
import com.radolyn.ayugram.easy.DummyFileUploadWaiter;
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
}
