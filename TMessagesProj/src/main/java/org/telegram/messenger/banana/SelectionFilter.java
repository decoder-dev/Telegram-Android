package org.telegram.messenger.banana;

import java.util.regex.Pattern;

/** Selected text starts as a literal, never executable user-supplied regex syntax. */
public final class SelectionFilter {
    private SelectionFilter() { }
    public static String expression(CharSequence selection) {
        if (selection == null || selection.length() == 0 || selection.length() > RegexProtocol.MAX_PATTERN) return null;
        String pattern = Pattern.quote(selection.toString());
        return pattern.length() <= RegexProtocol.MAX_PATTERN ? pattern : null;
    }
}
