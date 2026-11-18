package org.telegram.messenger.banana;

import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rules applied to the text of an outgoing message (ported from BananaGram iOS, Ghostgram): word
 * replacement (the user's rules, whole words, one pass), then Anti-Caps (ALL CAPS becomes sentence
 * case). Links, mentions, commands, hashtags, code and custom emoji are never touched, and the
 * formatting entities move with the text. Callers skip bots, which get exactly what was typed.
 */
public final class OutgoingTextRules {
    public static final int MAX_RULES = 200;
    public static final int MAX_RULE_LENGTH = 256;
    private static final int MAX_TEXT = 65536;

    public static final class Rule {
        public final String source, replacement;

        Rule(String source, String replacement) {
            this.source = source;
            this.replacement = replacement;
        }
    }

    public static final class Result {
        public final String text;
        public final ArrayList<TLRPC.MessageEntity> entities;
        public final boolean changed;

        Result(String text, ArrayList<TLRPC.MessageEntity> entities, boolean changed) {
            this.text = text;
            this.entities = entities;
            this.changed = changed;
        }
    }

    static final class Edit {
        final int start, end;
        final String replacement;

        Edit(int start, int end, String replacement) {
            this.start = start;
            this.end = end;
            this.replacement = replacement;
        }
    }

    private OutgoingTextRules() {}

    /** One rule per line, {@code source = replacement}; lines without '=' or with an empty source are skipped. */
    public static List<Rule> parseRules(String stored) {
        ArrayList<Rule> result = new ArrayList<>();
        if (stored == null) return result;
        for (String line : stored.split("\n")) {
            int separator = line.indexOf('=');
            if (separator < 0) continue;
            String source = line.substring(0, separator).trim();
            String replacement = line.substring(separator + 1).trim();
            if (source.isEmpty() || source.length() > MAX_RULE_LENGTH || replacement.length() > MAX_RULE_LENGTH) continue;
            result.add(new Rule(source, replacement));
            if (result.size() >= MAX_RULES) break;
        }
        return result;
    }

    /** The text and entities after the rules; {@code changed} is false (and the input returned) when nothing applies. */
    public static Result apply(String text, ArrayList<TLRPC.MessageEntity> entities, List<Rule> rules, boolean antiCaps) {
        Result original = new Result(text, entities, false);
        if (text == null || text.isEmpty() || text.length() > MAX_TEXT || ((rules == null || rules.isEmpty()) && !antiCaps)) {
            return original;
        }
        if (entities != null) {
            for (TLRPC.MessageEntity entity : entities) {
                if (entity == null || entity.offset < 0 || entity.length < 0 || entity.offset + entity.length > text.length()) {
                    return original;
                }
            }
        }
        String currentText = text;
        ArrayList<TLRPC.MessageEntity> currentEntities = entities;
        boolean changed = false;
        if (rules != null && !rules.isEmpty()) {
            List<Edit> edits = replacementEdits(currentText, rules, protectedRanges(currentText, currentEntities));
            Result applied = applying(edits, currentText, currentEntities);
            if (applied != null) {
                currentText = applied.text;
                currentEntities = applied.entities;
                changed = true;
            }
        }
        if (antiCaps && !currentText.startsWith("/")) {
            List<Edit> edits = antiCapsEdits(currentText, protectedRanges(currentText, currentEntities));
            Result applied = applying(edits, currentText, currentEntities);
            if (applied != null) {
                currentText = applied.text;
                currentEntities = applied.entities;
                changed = true;
            }
        }
        return changed ? new Result(currentText, currentEntities, true) : original;
    }

    // Ranges the stages never touch: entities whose text must stay as typed, and what the server
    // detects on its own in plain text (links, domains, e-mails, @usernames, /commands, #hashtags, $cashtags).
    private static final Pattern[] DETECTED = {
            Pattern.compile("(?i)\\b(?:https?://|tg://|www\\.)[^\\s]+"),
            Pattern.compile("(?i)(?<![\\p{L}\\p{N}_@])(?:[\\p{L}\\p{N}](?:[\\p{L}\\p{N}-]{0,61}[\\p{L}\\p{N}])?\\.)+(?:[a-z]{2,24}|рф|бел|укр|срб|мкд|қаз)\\b(?:[/?#][^\\s]*)?"),
            Pattern.compile("[\\p{L}\\p{N}._%+-]+@[\\p{L}\\p{N}-]+(?:\\.[\\p{L}\\p{N}-]+)+"),
            Pattern.compile("(?<![\\p{L}\\p{N}_@])@[A-Za-z0-9_]{3,32}"),
            Pattern.compile("(?<![\\p{L}\\p{N}_/])/[A-Za-z0-9_]{1,64}(?:@[A-Za-z0-9_]{3,32})?"),
            Pattern.compile("(?<![\\p{L}\\p{N}_#])#[\\p{L}\\p{N}_]+"),
            Pattern.compile("(?<![\\p{L}\\p{N}_$])\\$[A-Z]{1,8}\\b")
    };

    /** Formatting moves with the text; every other entity (links, mentions, code, custom emoji…) protects its text. */
    static boolean isProtected(TLRPC.MessageEntity entity) {
        return !(entity instanceof TLRPC.TL_messageEntityBold || entity instanceof TLRPC.TL_messageEntityItalic
                || entity instanceof TLRPC.TL_messageEntityUnderline || entity instanceof TLRPC.TL_messageEntityStrike
                || entity instanceof TLRPC.TL_messageEntitySpoiler || entity instanceof TLRPC.TL_messageEntityBlockquote);
    }

    static List<int[]> protectedRanges(String text, List<TLRPC.MessageEntity> entities) {
        ArrayList<int[]> ranges = new ArrayList<>();
        if (entities != null) {
            for (TLRPC.MessageEntity entity : entities) {
                if (isProtected(entity) && entity.length > 0) ranges.add(new int[]{entity.offset, entity.offset + entity.length});
            }
        }
        for (Pattern pattern : DETECTED) {
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                if (matcher.end() > matcher.start()) ranges.add(new int[]{matcher.start(), matcher.end()});
            }
        }
        Collections.sort(ranges, (a, b) -> Integer.compare(a[0], b[0]));
        ArrayList<int[]> merged = new ArrayList<>();
        for (int[] range : ranges) {
            int[] last = merged.isEmpty() ? null : merged.get(merged.size() - 1);
            if (last != null && range[0] <= last[1]) {
                last[1] = Math.max(last[1], range[1]);
            } else {
                merged.add(new int[]{range[0], range[1]});
            }
        }
        return merged;
    }

    static boolean intersects(int start, int end, List<int[]> protectedRanges) {
        for (int[] range : protectedRanges) {
            if (range[0] >= end) break;
            if (range[0] < end && start < range[1]) return true;
        }
        return false;
    }

    private static boolean isWordCharacter(char unit) {
        // Surrogate halves belong to emoji and other symbols, not to words.
        return !Character.isSurrogate(unit) && (Character.isLetterOrDigit(unit) || unit == '_');
    }

    /** Whole words, case-insensitive, in one pass: the earliest match wins, the longest wins a tie, no chaining. */
    static List<Edit> replacementEdits(String text, List<Rule> rules, List<int[]> protectedRanges) {
        ArrayList<Edit> matches = new ArrayList<>();
        for (Rule rule : rules) {
            String source = rule.source;
            int length = source.length();
            if (length == 0) continue;
            for (int at = 0; at + length <= text.length(); at++) {
                if (!text.regionMatches(true, at, source, 0, length)) continue;
                int end = at + length;
                if (at > 0 && isWordCharacter(text.charAt(at - 1))) continue;
                if (end < text.length() && isWordCharacter(text.charAt(end))) continue;
                if (intersects(at, end, protectedRanges)) continue;
                matches.add(new Edit(at, end, adaptCase(text.substring(at, end), rule.replacement)));
                at = end - 1;
            }
        }
        Collections.sort(matches, (a, b) -> a.start != b.start ? Integer.compare(a.start, b.start)
                : Integer.compare(b.end - b.start, a.end - a.start));
        ArrayList<Edit> result = new ArrayList<>();
        int lastEnd = 0;
        for (Edit match : matches) {
            if (match.start < lastEnd) continue;
            result.add(match);
            lastEnd = match.end;
        }
        return result;
    }

    /** "HELLO" gives the replacement in capitals, "Hello" capitalizes it, anything else keeps it as written. */
    static String adaptCase(String matched, String replacement) {
        int letters = 0, upper = 0, firstLetter = -1;
        for (int i = 0; i < matched.length(); ) {
            int codePoint = matched.codePointAt(i);
            if (Character.isLetter(codePoint)) {
                if (firstLetter < 0) firstLetter = codePoint;
                letters++;
                if (Character.isUpperCase(codePoint)) upper++;
            }
            i += Character.charCount(codePoint);
        }
        if (letters > 1 && upper == letters) return replacement.toUpperCase(Locale.ROOT);
        if (firstLetter >= 0 && Character.isUpperCase(firstLetter) && !replacement.isEmpty()) {
            int first = replacement.codePointAt(0);
            return new String(Character.toChars(Character.toUpperCase(first))) + replacement.substring(Character.charCount(first));
        }
        return replacement;
    }

    /**
     * A text of at least four cased letters that is at least 80% capitals becomes sentence case. Only
     * letters that have a case count, so "NASA" inside Chinese or Arabic text is not shouting.
     */
    static List<Edit> antiCapsEdits(String text, List<int[]> protectedRanges) {
        int cased = 0, upper = 0, uncased = 0;
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            int next = i + Character.charCount(codePoint);
            if (Character.isLetter(codePoint) && !intersects(i, next, protectedRanges)) {
                if (Character.isUpperCase(codePoint)) {
                    upper++;
                    cased++;
                } else if (Character.isLowerCase(codePoint)) {
                    cased++;
                } else {
                    uncased++;
                }
            }
            i = next;
        }
        ArrayList<Edit> result = new ArrayList<>();
        if (cased < 4 || uncased * 4 > cased || upper * 5 < cased * 4) return result;
        boolean sentenceStart = true;
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            int next = i + Character.charCount(codePoint);
            boolean isProtected = intersects(i, next, protectedRanges);
            if (Character.isLetter(codePoint)) {
                if (!isProtected) {
                    String original = text.substring(i, next);
                    String updated = sentenceStart ? original.toUpperCase(Locale.ROOT) : original.toLowerCase(Locale.ROOT);
                    if (!updated.equals(original)) result.add(new Edit(i, next, updated));
                }
                sentenceStart = false;
            } else if (codePoint == '.' || codePoint == '!' || codePoint == '?' || codePoint == '…' || codePoint == '\n') {
                sentenceStart = true;
            } else if (Character.isDigit(codePoint) || isProtected) {
                sentenceStart = false;
            }
            i = next;
        }
        return result;
    }

    /**
     * Applies non-overlapping edits. An entity boundary outside every edit shifts by the length change
     * of the edits before it, a boundary inside an edit moves to that edit's new bounds, and entities
     * that become empty are dropped. Entities are copied; the composer's objects are not modified.
     */
    static Result applying(List<Edit> edits, String text, ArrayList<TLRPC.MessageEntity> entities) {
        ArrayList<Edit> sorted = new ArrayList<>();
        for (Edit edit : edits) if (edit.start >= 0 && edit.end <= text.length() && edit.start <= edit.end) sorted.add(edit);
        Collections.sort(sorted, (a, b) -> a.start != b.start ? Integer.compare(a.start, b.start) : Integer.compare(a.end, b.end));
        ArrayList<Edit> accepted = new ArrayList<>();
        int lastEnd = 0;
        for (Edit edit : sorted) {
            if (edit.start < lastEnd) continue;
            if (text.substring(edit.start, edit.end).equals(edit.replacement)) continue;
            accepted.add(edit);
            lastEnd = edit.end;
        }
        if (accepted.isEmpty()) return null;
        StringBuilder result = new StringBuilder(text.length() + 16);
        int[][] mapped = new int[accepted.size()][];
        int cursor = 0;
        for (int i = 0; i < accepted.size(); i++) {
            Edit edit = accepted.get(i);
            result.append(text, cursor, edit.start);
            int newStart = result.length();
            result.append(edit.replacement);
            mapped[i] = new int[]{edit.start, edit.end, newStart, result.length()};
            cursor = edit.end;
        }
        result.append(text, cursor, text.length());
        ArrayList<TLRPC.MessageEntity> updated = null;
        if (entities != null) {
            updated = new ArrayList<>(entities.size());
            for (TLRPC.MessageEntity entity : entities) {
                int start = map(mapped, entity.offset, true);
                int end = map(mapped, entity.offset + entity.length, false);
                if (end <= start) continue;
                TLRPC.MessageEntity copy = TrackingLinks.copy(entity);
                if (copy == null) return null;
                copy.offset = start;
                copy.length = end - start;
                updated.add(copy);
            }
        }
        return new Result(result.toString(), updated, true);
    }

    private static int map(int[][] mapped, int position, boolean isStart) {
        int delta = 0;
        for (int[] item : mapped) {
            int oldStart = item[0], oldEnd = item[1], newStart = item[2], newEnd = item[3];
            if (oldEnd <= position) {
                delta += (newEnd - newStart) - (oldEnd - oldStart);
            } else if (oldStart < position) {
                return isStart ? newStart : newEnd;
            } else {
                break;
            }
        }
        return position + delta;
    }
}
