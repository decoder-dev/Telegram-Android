package org.telegram.messenger.banana;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class TranslationPolicyTest {
    @Test public void chosenServiceFirstThenTheOthersWithFallback() {
        assertArrayEquals(new int[]{2}, TranslationPolicy.order(2, false, true, false));
        assertArrayEquals(new int[]{1, 2}, TranslationPolicy.order(0, true, true, false));
        assertArrayEquals(new int[]{3, 1}, TranslationPolicy.order(3, true, false, true));
        assertArrayEquals(new int[]{1, 2, 3}, TranslationPolicy.order(1, true, true, true));
        assertArrayEquals(new int[0], TranslationPolicy.order(0, false, true, true));
        // A service without a key is skipped; Google takes over rather than nothing.
        assertArrayEquals(new int[]{1}, TranslationPolicy.order(2, false, false, false));
        assertFalse(TranslationPolicy.hasKey("  "));
    }

    @Test public void deeplHostAndLanguageCodes() {
        assertEquals("api-free.deepl.com", TranslationPolicy.deeplHost("abc:fx"));
        assertEquals("api-free.deepl.com", TranslationPolicy.deeplHost("abc:fx "));
        assertEquals("api.deepl.com", TranslationPolicy.deeplHost("abc"));
        assertEquals("EN-US", TranslationPolicy.deeplLanguage("en"));
        assertEquals("PT-BR", TranslationPolicy.deeplLanguage("pt_BR"));
        assertEquals("ZH-HANS", TranslationPolicy.deeplLanguage("zh"));
        assertEquals("NB", TranslationPolicy.deeplLanguage("no"));
        assertEquals("NB", TranslationPolicy.deeplLanguage("nb"));
        assertEquals("DE", TranslationPolicy.deeplLanguage("de"));
        assertEquals("pt", TranslationPolicy.yandexLanguage("pt_BR"));
    }

    @Test public void bodiesAreValidJsonForAnyText() {
        assertEquals("{\"text\":[\"a\\\"b\"],\"target_lang\":\"DE\"}", TranslationPolicy.deeplBody(Collections.singletonList("a\"b"), "de"));
        assertEquals("\"line\\nnext\\\\ \\u0001\"", TranslationPolicy.jsonString("line\nnext\\ \u0001"));
        assertEquals("{\"texts\":[\"hi\"],\"targetLanguageCode\":\"ru\",\"format\":\"PLAIN_TEXT\"}",
                TranslationPolicy.yandexBody(Collections.singletonList("hi"), "ru"));
        assertEquals("\"😀\"", TranslationPolicy.jsonString("😀"));
    }

    @Test public void yandexChunksStayUnderTheLimitAndAreNeverEmpty() {
        StringBuilder text = new StringBuilder();
        while (text.length() < 25_000) text.append("word ");
        List<String> chunks = TranslationPolicy.yandexChunks(text.toString());
        assertTrue(chunks.size() >= 3);
        StringBuilder joined = new StringBuilder();
        for (String chunk : chunks) {
            assertFalse(chunk.isEmpty());
            assertTrue(chunk.length() <= TranslationPolicy.YANDEX_CHUNK);
            joined.append(chunk);
        }
        assertEquals(text.toString(), joined.toString());
        StringBuilder noSpaces = new StringBuilder();
        for (int i = 0; i < 12_000; i++) noSpaces.append("😀");
        for (String chunk : TranslationPolicy.yandexChunks(noSpaces.toString())) {
            assertFalse(Character.isHighSurrogate(chunk.charAt(chunk.length() - 1)));
        }
        assertTrue(TranslationPolicy.yandexChunks("").isEmpty());
    }

    @Test public void draftTranslationNeedsTextAndAPlainChat() {
        assertTrue(TranslationPolicy.shouldOfferDraftTranslation(true, false, "hello"));
        assertFalse(TranslationPolicy.shouldOfferDraftTranslation(true, false, "   "));
        assertFalse(TranslationPolicy.shouldOfferDraftTranslation(true, true, "hello"));
        assertFalse(TranslationPolicy.shouldOfferDraftTranslation(false, false, "hello"));
        assertEquals(Arrays.asList("a"), TranslationPolicy.yandexChunks("a"));
    }
}
