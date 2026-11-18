package org.telegram.messenger.banana;

import java.util.Locale;

/** Unverified accounts that pose as Telegram staff (ported from BananaGram iOS). */
public final class FakeSupport {
    private static final String[] MARKERS = {"telegram", "телеграм", "безопасност", "security", "support", "поддержк",
            "служба", "модерац", "moderat", "verification", "верификац", "notification", "уведомлен"};

    private FakeSupport() {}

    public static boolean looksFake(long userId, boolean verified, String firstName, String lastName, String username) {
        if (verified || userId == LoginCodeGuard.SERVICE_ID) return false;
        String text = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName) + " "
                + (username == null ? "" : username)).toLowerCase(Locale.ROOT);
        for (String marker : MARKERS) {
            if (text.contains(marker)) return true;
        }
        return false;
    }
}
