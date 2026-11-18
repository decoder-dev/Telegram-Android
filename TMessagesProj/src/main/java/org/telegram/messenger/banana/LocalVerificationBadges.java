package org.telegram.messenger.banana;

/**
 * A verification badge on your own profile that only you see (ported from BananaGram iOS). It is not
 * verification by Telegram; the values are the custom emoji iOS uses.
 */
public final class LocalVerificationBadges {
    public static final String[] TITLES = {null, "Major", "Hold", "Developer"};

    private LocalVerificationBadges() {}

    public static long iconFor(int badge) {
        switch (badge) {
            case 1: return 5909078961467431777L;
            case 2: return 5913729775393840687L;
            case 3: return 5800995289124838177L;
            default: return 0;
        }
    }
}
