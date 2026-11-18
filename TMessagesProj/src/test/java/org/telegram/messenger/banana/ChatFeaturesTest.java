package org.telegram.messenger.banana;

import org.junit.Test;
import org.telegram.tgnet.TLRPC;

import static org.junit.Assert.*;

public class ChatFeaturesTest {
    @Test public void messageJsonUsesBotApiNames() {
        TLRPC.TL_message message = new TLRPC.TL_message();
        message.id = 5;
        message.message = "hi";
        TLRPC.TL_peerChannel channel = new TLRPC.TL_peerChannel();
        channel.channel_id = 123;
        message.peer_id = channel;
        TLRPC.TL_messageEntityBold bold = new TLRPC.TL_messageEntityBold();
        bold.offset = 0;
        bold.length = 2;
        message.entities.add(bold);
        String json = MessageJson.toJson(message);
        assertTrue(json, json.contains("\"id\": 5"));
        assertTrue(json, json.contains("\"text\": \"hi\""));
        assertTrue(json, json.contains("\"type\": \"bold\""));
        assertTrue(json, json.contains("-1000000000123"));
        assertEquals("text_url", MessageJson.entityType(new TLRPC.TL_messageEntityTextUrl()));
        assertEquals(-42, MessageJson.botApiId(chat(42)));
    }

    private static TLRPC.Peer chat(long id) {
        TLRPC.TL_peerChat peer = new TLRPC.TL_peerChat();
        peer.chat_id = id;
        return peer;
    }

    @Test public void localBadgesMapToTheIosEmoji() {
        assertEquals(0, LocalVerificationBadges.iconFor(0));
        assertEquals(5909078961467431777L, LocalVerificationBadges.iconFor(1));
        assertEquals(5913729775393840687L, LocalVerificationBadges.iconFor(2));
        assertEquals(5800995289124838177L, LocalVerificationBadges.iconFor(3));
        assertEquals(0, LocalVerificationBadges.iconFor(9));
    }

    @Test public void anonymousStickerLosesItsSetButKeepsTheRest() {
        TLRPC.TL_document document = new TLRPC.TL_document();
        document.id = 7;
        document.access_hash = 9;
        document.mime_type = "image/webp";
        TLRPC.TL_documentAttributeSticker sticker = new TLRPC.TL_documentAttributeSticker();
        sticker.alt = "😀";
        sticker.stickerset = new TLRPC.TL_inputStickerSetID();
        TLRPC.TL_documentAttributeImageSize size = new TLRPC.TL_documentAttributeImageSize();
        document.attributes.add(sticker);
        document.attributes.add(size);
        TLRPC.TL_document copy = AnonymousStickers.setlessCopy(document, 1234, 100);
        assertEquals(0, copy.id);
        assertEquals(0, copy.access_hash);
        assertEquals(1234, copy.size);
        assertEquals("image/webp", copy.mime_type);
        assertTrue(copy.attributes.get(0) instanceof TLRPC.TL_documentAttributeSticker);
        assertTrue(copy.attributes.get(0).stickerset instanceof TLRPC.TL_inputStickerSetEmpty);
        assertEquals("😀", copy.attributes.get(0).alt);
        assertSame(size, copy.attributes.get(1));
        assertTrue(document.attributes.get(0).stickerset instanceof TLRPC.TL_inputStickerSetID);
    }

    @Test public void approximateLastSeenKeepsTheLatestActivityForThreeDays() {
        assertTrue(ApproximateLastSeen.showable(1000, 1000 + ApproximateLastSeen.MAXIMUM_AGE - 1));
        assertFalse(ApproximateLastSeen.showable(1000, 1000 + ApproximateLastSeen.MAXIMUM_AGE));
        assertFalse(ApproximateLastSeen.showable(2000, 1000));
        ApproximateLastSeen.clear(7);
        ApproximateLastSeen.clear(8);
        ApproximateLastSeen.put(7, 5, 100);
        ApproximateLastSeen.put(7, 5, 50);
        assertEquals(100, ApproximateLastSeen.latest(7, 5, 200));
        assertEquals(0, ApproximateLastSeen.latest(8, 5, 200));
        ApproximateLastSeen.clear(7);
        assertEquals(0, ApproximateLastSeen.latest(7, 5, 200));
    }
}
