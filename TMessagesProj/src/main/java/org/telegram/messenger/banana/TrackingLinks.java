package org.telegram.messenger.banana;

import android.util.Patterns;

import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Clean a new outgoing message without modifying the composer's entities. */
public final class TrackingLinks {
    public static final class Result {
        public final String text;
        public final ArrayList<TLRPC.MessageEntity> entities;
        public final boolean changed;

        private Result(String text, ArrayList<TLRPC.MessageEntity> entities, boolean changed) {
            this.text = text;
            this.entities = entities;
            this.changed = changed;
        }
    }

    private TrackingLinks() {}

    public static Result clean(String text, ArrayList<TLRPC.MessageEntity> entities) {
        return clean(text, entities, Patterns.WEB_URL);
    }

    // Keep the platform detector separate so entity/UTF-16 policy can run in JVM tests.
    static Result clean(String text, ArrayList<TLRPC.MessageEntity> entities, Pattern detector) {
        Result original = new Result(text, entities, false);
        if (text == null || text.isEmpty() || text.length() > 65536 ||
                (entities != null && entities.size() > 1024)) return original;
        if (entities != null) {
            for (TLRPC.MessageEntity entity : entities) {
                if (entity == null || entity.offset < 0 || entity.length <= 0 ||
                        entity.offset > text.length() || entity.length > text.length() - entity.offset) return original;
            }
        }
        boolean changed = false;
        boolean[] keep = new boolean[text.length()];
        Arrays.fill(keep, true);
        if (text.indexOf('?') >= 0) {
            Matcher matches = detector.matcher(text);
            int count = 0;
            while (matches.find() && count++ < 128) {
                int start = matches.start();
                int end = trimEnd(text, start, matches.end());
                // A detector may find only the host part of an authenticated or non-web URL.
                if (start > 0 && !Character.isWhitespace(text.charAt(start - 1)) &&
                        "([{<\"'".indexOf(text.charAt(start - 1)) < 0) continue;
                if (blocked(entities, start, end, true)) continue;
                List<TrackingUrl.Range> projection = TrackingUrl.project(text.substring(start, end));
                if (projection == null) continue;
                Arrays.fill(keep, start, end, false);
                for (TrackingUrl.Range range : projection) Arrays.fill(keep, start + range.start, start + range.end, true);
                changed = true;
            }
        }
        ArrayList<TLRPC.MessageEntity> copies = entities == null ? null : new ArrayList<>();
        int[] offsets = new int[text.length() + 1];
        StringBuilder cleaned = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            if (keep[i]) cleaned.append(text.charAt(i));
            offsets[i + 1] = cleaned.length();
        }
        if (entities != null) {
            for (TLRPC.MessageEntity entity : entities) {
                String url = entity.url;
                if (entity instanceof TLRPC.TL_messageEntityTextUrl &&
                        !blocked(entities, entity.offset, entity.offset + entity.length, false)) {
                    url = TrackingUrl.clean(url);
                    if (url != null && !url.equals(entity.url)) changed = true;
                }
                TLRPC.MessageEntity copy = copy(entity);
                if (copy == null) return original;
                copy.url = url;
                copy.offset = offsets[entity.offset];
                copy.length = offsets[entity.offset + entity.length] - copy.offset;
                if (copy.length > 0) copies.add(copy);
            }
        }
        return changed ? new Result(cleaned.toString(), copies, true) : original;
    }

    private static boolean blocked(ArrayList<TLRPC.MessageEntity> entities, int start, int end, boolean includeHidden) {
        if (entities == null) return false;
        for (TLRPC.MessageEntity entity : entities) {
            if (entity.offset < end && entity.offset + entity.length > start &&
                    (entity instanceof TLRPC.TL_messageEntityCode || entity instanceof TLRPC.TL_messageEntityPre ||
                            (includeHidden && entity instanceof TLRPC.TL_messageEntityTextUrl))) return true;
        }
        return false;
    }

    private static int trimEnd(String text, int start, int end) {
        final String opens = "([{«", closes = ")]}»";
        int[] balance = new int[opens.length()];
        for (int i = start; i < end; i++) {
            int open = opens.indexOf(text.charAt(i));
            int close = closes.indexOf(text.charAt(i));
            if (open >= 0) balance[open]++;
            if (close >= 0) balance[close]--;
        }
        while (end > start) {
            char c = text.charAt(end - 1);
            if (".,;!".indexOf(c) >= 0) {
                end--;
                continue;
            }
            int close = closes.indexOf(c);
            if (close >= 0 && balance[close] < 0) {
                end--;
                balance[close]++;
                continue;
            }
            break;
        }
        return end;
    }

    private static TLRPC.MessageEntity copy(TLRPC.MessageEntity entity) {
        SerializedData encoded = new SerializedData();
        SerializedData decoded = null;
        try {
            entity.serializeToStream(encoded);
            byte[] data = encoded.toByteArray();
            if (data.length > 65536) return null;
            decoded = new SerializedData(data);
            if (data.length < 4) return null;
            TLRPC.MessageEntity result = TLRPC.MessageEntity.fromConstructor(decoded.readInt32(true));
            if (result == null) return null;
            result.readParams(decoded, true);
            return result;
        } catch (RuntimeException e) {
            return null;
        } finally {
            encoded.cleanup();
            if (decoded != null) decoded.cleanup();
        }
    }
}
