package org.telegram.tgnet;

import org.junit.Test;
import org.telegram.tgnet.tl.TL_iv;

import static org.junit.Assert.*;

public class MessageSerializationTest {
    // The database appends local attachment metadata after all protocol fields.
    private static final int LOCAL_METADATA = 0x2001;

    private static class StoredMessage extends TLRPC.TL_message {
        @Override
        protected void writeAttachPath(OutputSerializedData stream) {
            stream.writeInt32(LOCAL_METADATA);
        }
    }

    private StoredMessage message() {
        StoredMessage message = new StoredMessage();
        message.id = 123;
        message.peer_id = new TLRPC.TL_peerUser();
        message.peer_id.user_id = 456;
        message.date = 1793193600;
        message.message = "photo caption";
        return message;
    }

    private TLRPC.TL_message roundTrip(StoredMessage original) {
        SerializedData output = new SerializedData();
        original.serializeToStream(output);
        SerializedData input = new SerializedData(output.toByteArray());
        assertEquals(TLRPC.TL_message.constructor, input.readInt32(true));
        TLRPC.TL_message copy = new TLRPC.TL_message();
        copy.readParams(input, true);
        assertEquals("Protocol fields must precede local attachment metadata", LOCAL_METADATA, input.readInt32(true));
        assertEquals(0, input.remaining());
        assertEquals(original.id, copy.id);
        assertEquals(original.message, copy.message);
        assertEquals(original.peer_id.user_id, copy.peer_id.user_id);
        return copy;
    }

    @Test
    public void richMessageIsWrittenBeforeLocalMetadata() {
        StoredMessage original = message();
        original.rich_message = new TL_iv.RichMessage();
        original.rich_message.rtl = true;
        original.rich_message.part = true;
        TLRPC.TL_message copy = roundTrip(original);
        assertNotNull(copy.rich_message);
        assertTrue(copy.rich_message.rtl);
        assertTrue(copy.rich_message.part);
        assertTrue(copy.rich_message.blocks.isEmpty());
    }

    @Test
    public void optionalTailFieldsSurviveCachingTogether() {
        StoredMessage original = message();
        original.flags = TLObject.FLAG_30;
        original.flags2 = TLObject.FLAG_2 | TLObject.FLAG_5 | TLObject.FLAG_6
                | TLObject.FLAG_10 | TLObject.FLAG_11;
        original.quick_reply_shortcut_id = 73;
        original.effect = 0x123456789abcdefL;
        original.report_delivery_until_date = 1793194000;
        original.paid_message_stars = 4000000000L;
        original.schedule_repeat_period = 86400;
        original.summary_from_language = "ru";
        original.rich_message = new TL_iv.RichMessage();
        TLRPC.TL_message copy = roundTrip(original);
        assertEquals(original.quick_reply_shortcut_id, copy.quick_reply_shortcut_id);
        assertEquals(original.effect, copy.effect);
        assertEquals(original.report_delivery_until_date, copy.report_delivery_until_date);
        assertEquals(original.paid_message_stars, copy.paid_message_stars);
        assertEquals(original.schedule_repeat_period, copy.schedule_repeat_period);
        assertEquals(original.summary_from_language, copy.summary_from_language);
    }

    @Test
    public void currentLayerFlagsSurviveCaching() {
        StoredMessage original = message();
        original.invert_media = true;
        original.offline = true;
        original.video_processing_pending = true;
        original.paid_suggested_post_stars = true;
        original.paid_suggested_post_ton = true;
        original.noforwards = true;
        TLRPC.TL_message copy = roundTrip(original);
        assertTrue(copy.invert_media);
        assertTrue(copy.offline);
        assertTrue(copy.video_processing_pending);
        assertTrue(copy.paid_suggested_post_stars);
        assertTrue(copy.paid_suggested_post_ton);
        assertTrue(copy.noforwards);
        assertTrue(copy.ayuNoforwards);
        assertFalse("Inverted media is not an old-layer topic-start flag", copy.topic_start);
    }

    @Test
    public void plainMessageHasNoOptionalTail() {
        TLRPC.TL_message copy = roundTrip(message());
        assertNull(copy.rich_message);
        assertEquals(0, copy.flags2);
    }
}
