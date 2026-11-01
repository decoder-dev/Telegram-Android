package org.telegram.banana;

import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import static org.junit.Assert.*;

/** Request mutation only: no RPC is sent. */
public class GhostScheduleTest {
    @Test
    public void everySendKindPreservesManualDatesAndRefreshesOnlyAutomaticOnes() throws Exception {
        java.lang.reflect.Method refresh = SendMessagesHelper.class.getDeclaredMethod("bananaRefreshGhostSchedule", TLObject.class, boolean.class);
        refresh.setAccessible(true);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            SendMessagesHelper helper = SendMessagesHelper.getInstance(0);
            TLObject[] requests = {new TLRPC.TL_messages_sendMessage(), new TLRPC.TL_messages_sendMedia(),
                    new TLRPC.TL_messages_sendMultiMedia(), new TLRPC.TL_messages_forwardMessages()};
            int now = org.telegram.tgnet.ConnectionsManager.getInstance(0).getCurrentTime();
            for (TLObject request : requests) {
                try {
                    java.lang.reflect.Field date = request.getClass().getField("schedule_date");
                    date.setInt(request, now - 100);
                    refresh.invoke(helper, request, false);
                    assertEquals("A manually chosen date must not be moved by Ghost", now - 100, date.getInt(request));
                    refresh.invoke(helper, request, true);
                    assertTrue(date.getInt(request) >= now + 11);
                    date.setInt(request, 0);
                    refresh.invoke(helper, request, true);
                    assertEquals(0, date.getInt(request));
                } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
            }
        });
    }
}
