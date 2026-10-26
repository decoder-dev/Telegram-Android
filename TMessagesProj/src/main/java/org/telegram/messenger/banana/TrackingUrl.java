package org.telegram.messenger.banana;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Android port of BananaGram iOS ForkTrackingURL: keep original UTF-16 ranges. */
public final class TrackingUrl {
    private static final Set<String> TRACKING = new HashSet<>(Arrays.asList(
            "fbclid", "gclid", "dclid", "gbraid", "wbraid", "msclkid", "yclid", "ysclid",
            "igshid", "mc_cid", "mc_eid", "srsltid"));
    private static final Set<String> SIGNED = new HashSet<>(Arrays.asList(
            "signature", "sig", "hmac", "hash", "token", "access_token", "auth", "authorization",
            "awsaccesskeyid", "key-pair-id", "policy"));

    public static final class Range {
        public final int start, end;

        public Range(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    private TrackingUrl() {}

    /** Null means leave the entire URL untouched, including its original encoding. */
    public static List<Range> project(String source) {
        if (source == null || source.length() > 16384) return null;
        int fragment = source.indexOf('#');
        if (fragment < 0) fragment = source.length();
        final int question = source.indexOf('?');
        if (question < 0 || question > fragment) return null;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (Character.isWhitespace(c) || Character.isSpaceChar(c) || Character.isISOControl(c)) return null;
        }
        final String host;
        try {
            URI parsed = new URI(source);
            if (parsed.getScheme() == null) parsed = new URI("https://" + source);
            String scheme = parsed.getScheme();
            if (!("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)) ||
                    parsed.getRawUserInfo() != null || parsed.getHost() == null) return null;
            String normalized = parsed.getHost().toLowerCase(Locale.ROOT);
            host = normalized.endsWith(".") ? normalized.substring(0, normalized.length() - 1) : normalized;
        } catch (URISyntaxException e) {
            return null;
        }
        boolean shareHost = false;
        for (String domain : new String[]{"youtube.com", "youtu.be", "spotify.com"}) {
            if (host.equals(domain) || host.endsWith("." + domain)) shareHost = true;
        }

        ArrayList<Range> kept = new ArrayList<>();
        boolean removed = false;
        int start = question + 1;
        while (start <= fragment) {
            int end = source.indexOf('&', start);
            if (end < 0 || end > fragment) end = fragment;
            int equals = start;
            while (equals < end && source.charAt(equals) != '=') equals++;
            String key = decodeKey(source.substring(start, equals));
            if (key == null || SIGNED.contains(key) || key.startsWith("x-amz-") || key.startsWith("x-goog-")) return null;
            if (key.startsWith("utm_") || TRACKING.contains(key) || (shareHost && key.equals("si"))) {
                removed = true;
            } else if (start < end) {
                kept.add(new Range(start, end));
            }
            if (end == fragment) break;
            start = end + 1;
        }
        if (!removed) return null;
        ArrayList<Range> result = new ArrayList<>();
        result.add(new Range(0, question));
        if (!kept.isEmpty()) {
            result.add(new Range(question, question + 1));
            for (int i = 0; i < kept.size(); i++) {
                Range pair = kept.get(i);
                if (i != 0) result.add(new Range(pair.start - 1, pair.start));
                result.add(pair);
            }
        }
        if (fragment < source.length()) result.add(new Range(fragment, source.length()));
        return result;
    }

    public static String clean(String source) {
        List<Range> projection = project(source);
        if (projection == null) return source;
        StringBuilder result = new StringBuilder(source.length());
        for (Range range : projection) result.append(source, range.start, range.end);
        return result.toString();
    }

    private static String decodeKey(String encoded) {
        StringBuilder decoded = new StringBuilder();
        for (int i = 0; i < encoded.length();) {
            if (encoded.charAt(i) != '%') {
                decoded.append(encoded.charAt(i++));
                continue;
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            while (i < encoded.length() && encoded.charAt(i) == '%') {
                if (i + 2 >= encoded.length()) return null;
                int high = Character.digit(encoded.charAt(i + 1), 16);
                int low = Character.digit(encoded.charAt(i + 2), 16);
                if (high < 0 || low < 0) return null;
                bytes.write(high * 16 + low);
                i += 3;
            }
            try {
                decoded.append(StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes.toByteArray())));
            } catch (CharacterCodingException e) {
                return null;
            }
        }
        return decoded.toString().toLowerCase(Locale.ROOT);
    }
}
