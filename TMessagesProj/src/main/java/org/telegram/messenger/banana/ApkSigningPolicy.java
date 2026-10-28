package org.telegram.messenger.banana;
import java.security.MessageDigest;
import java.util.*;
/** Require exactly the installed signer set; hashes alone do not authenticate an update feed. */
public final class ApkSigningPolicy {
    private ApkSigningPolicy() {}
    public static boolean sameSigners(byte[][] downloaded, byte[][] installed) {
        try { return downloaded != null && installed != null && downloaded.length > 0 && installed.length > 0
                && downloaded.length <= 16 && installed.length <= 16 && fingerprints(downloaded).equals(fingerprints(installed)); }
        catch (Exception invalid) { return false; }
    }
    private static Set<String> fingerprints(byte[][] certificates) throws Exception {
        Set<String> values = new HashSet<>();
        for (byte[] certificate : certificates) {
            if (certificate == null || certificate.length == 0) throw new IllegalArgumentException("Empty certificate");
            values.add(Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(certificate)));
        }
        return values;
    }
}
