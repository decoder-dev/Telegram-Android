package org.telegram.messenger.banana;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * The duress passcode's verifier and its pure rules (ported from BananaGram iOS). PBKDF2-HMAC-SHA256
 * with a random salt, compared in constant time; the code itself is never stored.
 */
public final class DuressVerifier {
    public static final int ITERATIONS = 100_000;
    public static final int ACTION_HIDE = 0, ACTION_LOG_OUT = 1, ACTION_ERASE = 2;

    private DuressVerifier() {}

    public static String make(String code, SecureRandom random) {
        if (code == null || code.isEmpty()) return null;
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        byte[] hash = derive(code, salt, ITERATIONS);
        if (hash == null) return null;
        Base64.Encoder encoder = Base64.getEncoder();
        return "pbkdf2$" + ITERATIONS + "$" + encoder.encodeToString(salt) + "$" + encoder.encodeToString(hash);
    }

    public static boolean matches(String code, String verifier) {
        if (code == null || verifier == null) return false;
        try {
            String[] parts = verifier.split("\\$");
            if (parts.length != 4 || !"pbkdf2".equals(parts[0])) return false;
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < 10_000 || iterations > 2_000_000) return false;
            Base64.Decoder decoder = Base64.getDecoder();
            byte[] salt = decoder.decode(parts[2]);
            byte[] expected = decoder.decode(parts[3]);
            if (salt.length < 8 || expected.length < 16) return false;
            byte[] actual = derive(code, salt, iterations);
            return actual != null && MessageDigest.isEqual(expected, actual);
        } catch (Throwable invalid) {
            return false;
        }
    }

    /** Spends the same time as a real check, so a wrong code does not reveal whether a duress code exists. */
    public static void dummy(String code) {
        derive(code == null || code.isEmpty() ? "0000" : code, new byte[16], ITERATIONS);
    }

    static byte[] derive(String code, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(code.toCharArray(), salt, iterations, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception unavailable) {
            return null;
        } finally {
            spec.clearPassword();
        }
    }

    public static int clampAction(int raw) {
        return raw >= ACTION_HIDE && raw <= ACTION_ERASE ? raw : ACTION_HIDE;
    }

    public static long[] parseIds(String csv) {
        Set<Long> ids = new LinkedHashSet<>();
        if (csv != null) {
            for (String part : csv.split(",")) {
                try {
                    long id = Long.parseLong(part.trim());
                    if (id > 0) ids.add(id);
                } catch (NumberFormatException ignored) {
                    // Skipped: the list is written by this app; a damaged entry costs one account at most.
                }
            }
        }
        long[] result = new long[ids.size()];
        int i = 0;
        for (long id : ids) result[i++] = id;
        return result;
    }

    public static String formatIds(long[] ids) {
        StringBuilder text = new StringBuilder();
        if (ids != null) {
            for (long id : ids) {
                if (id <= 0) continue;
                if (text.length() > 0) text.append(',');
                text.append(id);
            }
        }
        return text.toString();
    }

    /** Account slots the action logs out: the chosen signed-in accounts, every one for Erase, none for Hide. */
    public static int[] slotsToLogOut(int action, long[] chosenUserIds, long[] slotUserIds) {
        ArrayList<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < slotUserIds.length; slot++) {
            long id = slotUserIds[slot];
            if (id == 0) continue;
            if (action == ACTION_ERASE) {
                slots.add(slot);
            } else if (action == ACTION_LOG_OUT && chosenUserIds != null) {
                for (long chosen : chosenUserIds) {
                    if (chosen == id) {
                        slots.add(slot);
                        break;
                    }
                }
            }
        }
        int[] result = new int[slots.size()];
        for (int i = 0; i < result.length; i++) result[i] = slots.get(i);
        return result;
    }

    /** A PIN duress code is exactly four ASCII digits, like the PIN it stands beside; a password is any text. */
    public static boolean fitsType(String code, boolean pin) {
        if (code == null || code.isEmpty()) return false;
        if (!pin) return true;
        if (code.length() != 4) return false;
        for (int i = 0; i < 4; i++) if (code.charAt(i) < '0' || code.charAt(i) > '9') return false;
        return true;
    }
}
