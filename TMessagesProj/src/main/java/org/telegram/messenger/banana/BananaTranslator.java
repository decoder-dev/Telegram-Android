package org.telegram.messenger.banana;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.Utilities;
import org.telegram.ui.Components.TranslateAlert2;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * Google, DeepL and Yandex translation with Automatic Fallback (ported from BananaGram iOS). Keys stay
 * in this account's settings and are never logged; neither is the text.
 */
public final class BananaTranslator {
    private static final DispatchQueue QUEUE = new DispatchQueue("bananaTranslate");
    private static final int MAX_RESPONSE = 4 * 1024 * 1024;

    private BananaTranslator() {}

    public static int[] order(int account) {
        return TranslationPolicy.order(BananaConfig.translationBackend(account),
                BananaConfig.get(account, BananaConfig.Option.TRANSLATION_FALLBACK),
                TranslationPolicy.hasKey(BananaConfig.getString(account, BananaConfig.StringValue.DEEPL_API_KEY)),
                TranslationPolicy.hasKey(BananaConfig.getString(account, BananaConfig.StringValue.YANDEX_API_KEY)));
    }

    /** Telegram failed: the other services, when Automatic Fallback is on. */
    public static int[] fallbackOrder(int account) {
        if (!BananaConfig.get(account, BananaConfig.Option.TRANSLATION_FALLBACK)) return new int[0];
        return TranslationPolicy.order(TranslationPolicy.TELEGRAM, true,
                TranslationPolicy.hasKey(BananaConfig.getString(account, BananaConfig.StringValue.DEEPL_API_KEY)),
                TranslationPolicy.hasKey(BananaConfig.getString(account, BananaConfig.StringValue.YANDEX_API_KEY)));
    }

    /** Same callback contract as TranslateAlert2.alternativeTranslate: on the UI thread, (text or null, rate limited). */
    public static void translate(int account, String text, String toLng, Utilities.Callback2<String, Boolean> done) {
        translate(account, text, toLng, order(account), done);
    }

    public static void translate(int account, String text, String toLng, int[] order, Utilities.Callback2<String, Boolean> done) {
        next(account, text, toLng, order, 0, false, done);
    }

    private static void next(int account, String text, String toLng, int[] order, int index, boolean limited, Utilities.Callback2<String, Boolean> done) {
        if (index >= order.length) {
            done.run(null, limited);
            return;
        }
        int backend = order[index];
        if (backend == TranslationPolicy.GOOGLE) {
            TranslateAlert2.alternativeTranslate(text, null, toLng, (result, rateLimit) -> {
                if (result != null) done.run(result, false);
                else next(account, text, toLng, order, index + 1, limited || Boolean.TRUE.equals(rateLimit), done);
            });
            return;
        }
        final String key = BananaConfig.getString(account, backend == TranslationPolicy.DEEPL
                ? BananaConfig.StringValue.DEEPL_API_KEY : BananaConfig.StringValue.YANDEX_API_KEY).trim();
        QUEUE.postRunnable(() -> {
            boolean[] rateLimit = new boolean[1];
            String result = null;
            try {
                result = backend == TranslationPolicy.DEEPL ? deepl(key, text, toLng, rateLimit) : yandex(key, text, toLng, rateLimit);
            } catch (Throwable error) {
                // Only the error type: the request carries the user's text and key.
                FileLog.e("BananaGram: translation request failed (" + error.getClass().getSimpleName() + ")");
            }
            final String translated = result;
            AndroidUtilities.runOnUIThread(() -> {
                if (translated != null && !translated.isEmpty()) done.run(translated, false);
                else next(account, text, toLng, order, index + 1, limited || rateLimit[0], done);
            });
        });
    }

    private static String post(String url, String authorization, String body, boolean[] rateLimit) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setUseCaches(false);
            connection.setInstanceFollowRedirects(false);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", authorization);
            connection.setRequestProperty("Content-Type", "application/json");
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(bytes);
            }
            int code = connection.getResponseCode();
            if (code == 429) rateLimit[0] = true;
            if (code != 200) return null;
            try (InputStream in = connection.getInputStream()) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] chunk = new byte[16 * 1024];
                int read;
                while ((read = in.read(chunk)) != -1) {
                    buffer.write(chunk, 0, read);
                    if (buffer.size() > MAX_RESPONSE) return null;
                }
                return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static String firstTranslation(String response, String field) throws Exception {
        if (response == null) return null;
        JSONArray translations = new JSONObject(response).optJSONArray("translations");
        if (translations == null || translations.length() == 0) return null;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < translations.length(); i++) out.append(translations.getJSONObject(i).optString(field, ""));
        return out.toString();
    }

    private static String deepl(String key, String text, String toLng, boolean[] rateLimit) throws Exception {
        if (!TranslationPolicy.hasKey(key)) return null;
        List<String> texts = Collections.singletonList(text);
        String response = post("https://" + TranslationPolicy.deeplHost(key) + "/v2/translate", "DeepL-Auth-Key " + key,
                TranslationPolicy.deeplBody(texts, toLng), rateLimit);
        return firstTranslation(response, "text");
    }

    private static String yandex(String key, String text, String toLng, boolean[] rateLimit) throws Exception {
        if (!TranslationPolicy.hasKey(key)) return null;
        StringBuilder out = new StringBuilder();
        for (String chunk : TranslationPolicy.yandexChunks(text)) {
            String response = post("https://translate.api.cloud.yandex.net/translate/v2/translate", "Api-Key " + key,
                    TranslationPolicy.yandexBody(Collections.singletonList(chunk), toLng), rateLimit);
            String part = firstTranslation(response, "text");
            if (part == null) return null;
            out.append(part);
        }
        return out.toString();
    }
}
