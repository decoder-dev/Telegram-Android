package org.telegram.messenger.banana;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Translator choice and request formats (ported from BananaGram iOS ForkTranslationBackends): the
 * chosen service first, then, with Automatic Fallback, the others that are set up.
 */
public final class TranslationPolicy {
    public static final int TELEGRAM = 0, GOOGLE = 1, DEEPL = 2, YANDEX = 3;
    public static final int MAX_KEY = 128;
    public static final int YANDEX_CHUNK = 10_000;

    private TranslationPolicy() {}

    /** Services to try for one text, in order. Telegram is batch-only and handled by its own request. */
    public static int[] order(int chosen, boolean fallback, boolean hasDeepl, boolean hasYandex) {
        ArrayList<Integer> order = new ArrayList<>();
        if (chosen == GOOGLE || chosen == DEEPL && hasDeepl || chosen == YANDEX && hasYandex) order.add(chosen);
        if (fallback || order.isEmpty() && chosen != TELEGRAM) {
            if (!order.contains(GOOGLE)) order.add(GOOGLE);
            if (hasDeepl && !order.contains(DEEPL)) order.add(DEEPL);
            if (hasYandex && !order.contains(YANDEX)) order.add(YANDEX);
        }
        int[] result = new int[order.size()];
        for (int i = 0; i < result.length; i++) result[i] = order.get(i);
        return result;
    }

    public static boolean hasKey(String key) {
        return key != null && !key.trim().isEmpty();
    }

    /** Free-plan keys end in ":fx" and use their own host. */
    public static String deeplHost(String key) {
        return key != null && key.trim().endsWith(":fx") ? "api-free.deepl.com" : "api.deepl.com";
    }

    public static String deeplLanguage(String code) {
        String c = code == null ? "" : code.replace('_', '-').toLowerCase(Locale.ROOT);
        if (c.equals("en")) return "EN-US";
        if (c.equals("pt") || c.equals("pt-br")) return "PT-BR";
        if (c.equals("zh") || c.startsWith("zh-")) return "ZH-HANS";
        if (c.equals("no") || c.equals("nb")) return "NB";
        return c.toUpperCase(Locale.ROOT);
    }

    public static String yandexLanguage(String code) {
        String c = code == null ? "" : code.replace('_', '-').toLowerCase(Locale.ROOT);
        int dash = c.indexOf('-');
        return dash > 0 ? c.substring(0, dash) : c;
    }

    /** A JSON string literal; no org.json, so it runs in JVM tests too. */
    public static String jsonString(String text) {
        StringBuilder out = new StringBuilder(text.length() + 16).append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 0x20 || c == 0x2028 || c == 0x2029) {
                        out.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        return out.append('"').toString();
    }

    public static String deeplBody(List<String> texts, String target) {
        StringBuilder body = new StringBuilder("{\"text\":[");
        for (int i = 0; i < texts.size(); i++) {
            if (i > 0) body.append(',');
            body.append(jsonString(texts.get(i)));
        }
        return body.append("],\"target_lang\":").append(jsonString(deeplLanguage(target))).append('}').toString();
    }

    public static String yandexBody(List<String> texts, String target) {
        StringBuilder body = new StringBuilder("{\"texts\":[");
        for (int i = 0; i < texts.size(); i++) {
            if (i > 0) body.append(',');
            body.append(jsonString(texts.get(i)));
        }
        return body.append("],\"targetLanguageCode\":").append(jsonString(yandexLanguage(target)))
                .append(",\"format\":\"PLAIN_TEXT\"}").toString();
    }

    /** Yandex takes at most 10,000 UTF-16 characters per request; a longer text is split, never into empty parts. */
    public static List<String> yandexChunks(String text) {
        ArrayList<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) return chunks;
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + YANDEX_CHUNK);
            if (end < text.length() && Character.isHighSurrogate(text.charAt(end - 1))) end--;
            if (end < text.length()) {
                int space = text.lastIndexOf('\n', end - 1);
                if (space <= start) space = text.lastIndexOf(' ', end - 1);
                if (space > start) end = space + 1;
            }
            chunks.add(text.substring(start, end));
            start = end;
        }
        return chunks;
    }

    public static boolean shouldOfferDraftTranslation(boolean enabled, boolean encrypted, CharSequence text) {
        return enabled && !encrypted && text != null && text.toString().trim().length() > 0;
    }
}
