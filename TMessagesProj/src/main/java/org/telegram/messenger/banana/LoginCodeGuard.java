package org.telegram.messenger.banana;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Login codes from Telegram (777000) must never be sent to anyone (ported from BananaGram iOS): a
 * message is held back when it carries a fresh code, even spaced out like "1 2-3 4 5".
 */
public final class LoginCodeGuard {
    public static final long SERVICE_ID = 777000L;
    /** A code older than this is spent. */
    public static final int FRESH_SECONDS = 15 * 60;

    private LoginCodeGuard() {}

    /** ASCII digit runs of exactly 5 or 6 in Telegram's service message. */
    public static Set<String> codesIn(String serviceText) {
        Set<String> codes = new LinkedHashSet<>();
        if (serviceText == null) return codes;
        int start = -1;
        for (int i = 0; i <= serviceText.length(); i++) {
            char c = i < serviceText.length() ? serviceText.charAt(i) : ' ';
            boolean digit = c >= '0' && c <= '9';
            if (digit && start < 0) {
                start = i;
            } else if (!digit && start >= 0) {
                int length = i - start;
                if (length == 5 || length == 6) codes.add(serviceText.substring(start, i));
                start = -1;
            }
        }
        return codes;
    }

    /** Only texts with at least five digits can hold a code; the rest skip the lookup. */
    public static boolean mayHoldCode(String text) {
        if (text == null) return false;
        int digits = 0;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isDigit(text.charAt(i)) && ++digits >= 5) return true;
        }
        return false;
    }

    public static boolean containsCode(String text, Set<String> codes) {
        if (text == null || codes == null || codes.isEmpty()) return false;
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            int value = Character.digit(text.charAt(i), 10);
            if (value >= 0) digits.append((char) ('0' + value));
        }
        String compact = digits.toString();
        for (String code : codes) {
            if (text.contains(code) || compact.contains(code)) return true;
        }
        return false;
    }

    public static boolean isFresh(int messageDate, int now) {
        return now - messageDate < FRESH_SECONDS;
    }
}
