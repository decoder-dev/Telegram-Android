package org.telegram.messenger.banana;

import org.junit.Test;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class TrackingLinksTest {
    // Platform link discovery is an Android integration concern. These tests exercise
    // real TL entities and the cleanup policy using complete candidate spans.
    private static final Pattern DETECTOR = Pattern.compile("(?:https?://|[a-z]+\\.example/)[^\\s]+", Pattern.CASE_INSENSITIVE);

    private TrackingLinks.Result clean(String text, TLRPC.MessageEntity... entities) {
        return TrackingLinks.clean(text, new ArrayList<>(Arrays.asList(entities)), DETECTOR);
    }

    private <T extends TLRPC.MessageEntity> T span(T entity, int offset, int length) {
        entity.offset = offset;
        entity.length = length;
        return entity;
    }

    @Test
    public void iosUrlCompatibilityCases() {
        String[][] cases = {
                {"https://example.org/a?utm_source=chat&id=A%2Fb+c#section", "https://example.org/a?id=A%2Fb+c#section"},
                {"https://example.org/?id=1&utm_campaign=x&next=%2Fkeep%3Fv%3D2&gclid=foo", "https://example.org/?id=1&next=%2Fkeep%3Fv%3D2"},
                {"https://example.org/?UTM%5Fsource=chat#😀", "https://example.org/#😀"},
                {"https://www.youtube.com/watch?v=abc&t=12&si=share", "https://www.youtube.com/watch?v=abc&t=12"},
                {"https://open.spotify.com/track/123?si=share&context=playlist", "https://open.spotify.com/track/123?context=playlist"},
                {"https://youtube.com.evil.example/?si=essential&utm_source=x", "https://youtube.com.evil.example/?si=essential"},
                {"https://example.org/?si=essential&ref=required", "https://example.org/?si=essential&ref=required"},
                {"https://example.org/?utm_source=x&X-Amz-Signature=abc", "https://example.org/?utm_source=x&X-Amz-Signature=abc"},
                {"https://example.org/?sig=abc&utm_source=x", "https://example.org/?sig=abc&utm_source=x"},
                {"https://example.org/?utm_source=x&token=auth", "https://example.org/?utm_source=x&token=auth"},
                {"https://user:password@example.org/?utm_source=x", "https://user:password@example.org/?utm_source=x"},
                {"https://example.org/#view?utm_source=route", "https://example.org/#view?utm_source=route"},
                {"tg://resolve?domain=example&utm_source=x", "tg://resolve?domain=example&utm_source=x"},
                {"mailto:test@example.org?utm_source=x", "mailto:test@example.org?utm_source=x"},
                {"example.org/?utm_source=x&id=1", "example.org/?id=1"},
                {"https://example.org/?utm_source=x&invalid%ZZ=value", "https://example.org/?utm_source=x&invalid%ZZ=value"}
        };
        for (String[] pair : cases) assertEquals(pair[0], pair[1], TrackingUrl.clean(pair[0]));
    }

    @Test
    public void signaturesAndMalformedEncodingProtectWholeUrl() {
        for (String key : new String[]{"%73ig", "HMAC", "X-Goog-Credential", "key-pair-id", "auth", "%FF", "invalid%"}) {
            String url = "https://example.org/?utm_source=x&" + key + "=keep";
            assertEquals(url, TrackingUrl.clean(url));
        }
    }

    @Test
    public void preservesRawQueryEncodingAndEmptySeparatorsWhenNoTracking() {
        assertEquals("https://example.org/?id=%2f+%2B&&", TrackingUrl.clean("https://example.org/?id=%2f+%2B&&"));
        assertEquals("https://example.org/?id=%2f+%2B#fragment", TrackingUrl.clean("https://example.org/?utm_x=a&&id=%2f+%2B&&#fragment"));
        assertEquals("https://youtu.be./x", TrackingUrl.clean("https://youtu.be./x?si=a"));
    }

    @Test
    public void emojiAndFormattingInsideRetainedQueryKeepUtf16Offsets() {
        String text = "🇷🇺 https://example.org/😀?utm_source=chat&id=KEEP#anchor tail";
        TLRPC.TL_messageEntityBold bold = span(new TLRPC.TL_messageEntityBold(), text.indexOf("KEEP"), 4);
        TLRPC.TL_messageEntityUrl url = span(new TLRPC.TL_messageEntityUrl(), 5, text.indexOf(" tail") - 5);
        TrackingLinks.Result result = clean(text, bold, url);
        assertTrue(result.changed);
        assertEquals("🇷🇺 https://example.org/😀?id=KEEP#anchor tail", result.text);
        assertEquals(result.text.indexOf("KEEP"), result.entities.get(0).offset);
        assertEquals(4, result.entities.get(0).length);
        assertEquals(result.text.indexOf(" tail") - 5, result.entities.get(1).length);
        assertEquals(text.indexOf("KEEP"), bold.offset);
        assertNotSame(bold, result.entities.get(0));
    }

    @Test
    public void hiddenLinkLabelIsUntouchedEvenWhenItLooksLikeUrl() {
        String label = "https://label.example/?utm_source=part-of-label";
        TLRPC.TL_messageEntityTextUrl hidden = span(new TLRPC.TL_messageEntityTextUrl(), 0, label.length());
        hidden.url = "https://target.example/?id=1&utm_source=share";
        TrackingLinks.Result result = clean(label, hidden);
        assertEquals(label, result.text);
        assertEquals("https://target.example/?id=1", result.entities.get(0).url);
        assertEquals("https://target.example/?id=1&utm_source=share", hidden.url);
    }

    @Test
    public void codeAndPreformattedBlocksAreUntouched() {
        String url = "https://example.org/?utm_source=x&id=1";
        assertFalse(clean(url, span(new TLRPC.TL_messageEntityCode(), 0, url.length())).changed);
        TLRPC.TL_messageEntityPre pre = span(new TLRPC.TL_messageEntityPre(), 0, url.length());
        pre.language = "java";
        assertFalse(clean(url, pre).changed);
        TLRPC.TL_messageEntityTextUrl hidden = span(new TLRPC.TL_messageEntityTextUrl(), 0, url.length());
        hidden.url = url;
        assertFalse(clean(url, pre, hidden).changed);
    }

    @Test
    public void customEmojiAndMentionMetadataSurviveDeletionBeforeThem() {
        String text = "https://a.example/?utm_x=a&id=1 😀 Alice";
        TLRPC.TL_messageEntityCustomEmoji emoji = span(new TLRPC.TL_messageEntityCustomEmoji(), text.indexOf("😀"), 2);
        emoji.document_id = 123456789L;
        TLRPC.TL_messageEntityMentionName mention = span(new TLRPC.TL_messageEntityMentionName(), text.indexOf("Alice"), 5);
        mention.user_id = 987654321L;
        TrackingLinks.Result result = clean(text, emoji, mention);
        assertEquals(result.text.indexOf("😀"), result.entities.get(0).offset);
        assertEquals(123456789L, ((TLRPC.TL_messageEntityCustomEmoji) result.entities.get(0)).document_id);
        assertEquals(result.text.indexOf("Alice"), result.entities.get(1).offset);
        assertEquals(987654321L, ((TLRPC.TL_messageEntityMentionName) result.entities.get(1)).user_id);
    }

    @Test
    public void multipleUrlsAndOutsidePunctuationSurvive() {
        assertEquals("(https://a.example/), https://b.example/?id=2!",
                clean("(https://a.example/?utm_source=x), https://b.example/?id=2&utm_source=y!").text);
        assertEquals("https://a.example/path_(ok)?id=1", clean("https://a.example/path_(ok)?utm_x=x&id=1").text);
    }

    @Test
    public void invalidOrUnknownEntitiesLeaveOriginalMessageUntouched() {
        String text = "https://a.example/?utm_x=a&id=1";
        assertFalse(clean(text, span(new TLRPC.TL_messageEntityBold(), Integer.MAX_VALUE, 10)).changed);
        TLRPC.MessageEntity unsupported = span(new TLRPC.TL_messageEntityBold() {
            @Override
            public void serializeToStream(org.telegram.tgnet.OutputSerializedData stream) {
                stream.writeInt32(0x12345678);
            }
        }, 0, 4);
        TrackingLinks.Result result = clean(text, unsupported);
        assertFalse(result.changed);
        assertEquals(text, result.text);
    }

    @Test
    public void entitiesCoveringOnlyRemovedTextDisappear() {
        String text = "https://a.example/?utm_x=a&id=1";
        TLRPC.MessageEntity bold = span(new TLRPC.TL_messageEntityBold(), text.indexOf("utm_x"), 7);
        assertTrue(clean(text, bold).entities.isEmpty());
    }

    @Test
    public void cleaningIsIdempotentAndPlainTextKeepsOriginalEntities() {
        TrackingLinks.Result once = clean("https://a.example/?utm_x=a&id=1");
        TrackingLinks.Result twice = TrackingLinks.clean(once.text, once.entities, DETECTOR);
        assertEquals(once.text, twice.text);
        assertFalse(twice.changed);
        assertSame(once.entities, twice.entities);
        assertEquals("plain text", clean("plain text").text);
    }
}
