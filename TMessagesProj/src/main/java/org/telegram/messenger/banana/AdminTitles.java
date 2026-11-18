package org.telegram.messenger.banana;

/**
 * Admin titles with nothing visible in them (ported from BananaGram iOS): some admins use spaces,
 * zero-width or braille-blank characters to hide that they are admins.
 */
public final class AdminTitles {
    private AdminTitles() {}

    /** True when the title is non-empty but has no visible character. */
    public static boolean isInvisible(String title) {
        if (title == null || title.isEmpty()) return false;
        for (int i = 0; i < title.length(); ) {
            int codePoint = title.codePointAt(i);
            if (!isInvisible(codePoint)) return false;
            i += Character.charCount(codePoint);
        }
        return true;
    }

    static boolean isInvisible(int codePoint) {
        if (Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)) return true;
        int type = Character.getType(codePoint);
        if (type == Character.CONTROL || type == Character.FORMAT) return true;
        return codePoint == 0x2800 // Braille pattern blank is not Unicode whitespace.
                || isDefaultIgnorable(codePoint);
    }

    /** Unicode Default_Ignorable_Code_Point entries that are not format characters. */
    private static boolean isDefaultIgnorable(int c) {
        return c == 0x00AD || c == 0x034F || c == 0x061C || c == 0x115F || c == 0x1160
                || c == 0x17B4 || c == 0x17B5 || (c >= 0x180B && c <= 0x180F)
                || (c >= 0x200B && c <= 0x200F) || (c >= 0x202A && c <= 0x202E) || (c >= 0x2060 && c <= 0x206F)
                || c == 0x3164 || (c >= 0xFE00 && c <= 0xFE0F) || c == 0xFEFF || c == 0xFFA0
                || (c >= 0xFFF0 && c <= 0xFFF8) || (c >= 0x1BCA0 && c <= 0x1BCA3)
                || (c >= 0x1D173 && c <= 0x1D17A) || (c >= 0xE0000 && c <= 0xE0FFF);
    }
}
