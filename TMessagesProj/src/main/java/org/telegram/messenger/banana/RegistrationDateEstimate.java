package org.telegram.messenger.banana;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * When an account was most likely created, estimated from its user ID alone with the anchor table
 * ({@link RegistrationDateAnchors}); ported from BananaGram iOS (Ghostgram). Nothing leaves the
 * device. Used only where Telegram itself gave no date.
 */
public final class RegistrationDateEstimate {
    public static final class Result {
        /** Unix seconds, UTC. */
        public final long point, from, to;

        Result(long point, long from, long to) {
            this.point = point;
            this.from = from;
            this.to = to;
        }

        /** A 90% band up to half a year wide is shown as a month; a wider one as a year. */
        public boolean isMonthPrecise() {
            return to - from <= 183L * 86400L;
        }
    }

    private RegistrationDateEstimate() {}

    public static Result estimate(long userId) {
        return estimate(userId, System.currentTimeMillis() / 1000L);
    }

    static Result estimate(long userId, long now) {
        return rawEstimate(userId, Math.max(now, RegistrationDateAnchors.DATA_END));
    }

    /** "≈ March 2016" or "≈ 2016" in the given language, UTC. */
    public static String text(Result result, Locale locale) {
        SimpleDateFormat format = new SimpleDateFormat(result.isMonthPrecise() ? "LLLL yyyy" : "yyyy", locale);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return "≈ " + format.format(new Date(result.point * 1000L));
    }

    static boolean isReserved(long userId) {
        return userId == 777000 || userId == 333000 || userId == 489000 || userId == 4244000;
    }

    /** The table alone; null for IDs that are not accounts on the counter. */
    static Result rawEstimate(long userId, long now) {
        if (userId <= 0 || isReserved(userId)) return null;
        long[] ids = RegistrationDateAnchors.IDS;
        int[] points = RegistrationDateAnchors.POINTS, lows = RegistrationDateAnchors.LOWS, highs = RegistrationDateAnchors.HIGHS;
        int count = ids.length;
        if (count < 2 || points.length != count || lows.length != count || highs.length != count) return null;
        int last = count - 1;
        if (userId >= ids[last]) {
            // Newer than every anchor: the point follows the last year's ID growth, the low edge moves
            // at most as fast as IDs could have grown, and the range never ends after now.
            long lastPoint = points[last];
            long lastLow = lows[last];
            long band = (long) highs[last] - lastLow;
            double offset = (double) (userId - ids[last]);
            long upper = Math.max(lastPoint, now);
            long point = clamp(lastPoint + offset / RegistrationDateAnchors.IDS_PER_SECOND, lastPoint, upper);
            long lowCap = Math.min(point, Math.max(upper - band, lastLow));
            long from = clamp(lastLow + offset / (RegistrationDateAnchors.IDS_PER_SECOND * RegistrationDateAnchors.MAX_RATE_FACTOR),
                    lastLow, Math.max(lastLow, lowCap));
            return new Result(point, Math.max(RegistrationDateAnchors.LAUNCH, from), upper);
        }
        // First index with ids[i] > userId; the table starts at ID 0, so i >= 1.
        int low = 0, high = last;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (ids[mid] <= userId) low = mid + 1; else high = mid;
        }
        int i = low;
        if (i == 0) {
            long to = Math.min(highs[0], now);
            long point = Math.min(points[0], to);
            return new Result(point, Math.max(RegistrationDateAnchors.LAUNCH, Math.min(lows[0], point)), to);
        }
        double fraction = (double) (userId - ids[i - 1]) / (double) (ids[i] - ids[i - 1]);
        long to = interpolate(highs[i - 1], highs[i], fraction);
        long point = interpolate(points[i - 1], points[i], fraction);
        long from = interpolate(lows[i - 1], lows[i], fraction);
        // Near the top of the table accounts made after the data still get IDs: once the data is
        // old, the high edge there moves on with the clock.
        double distance = (double) (ids[last] - userId);
        if (distance < RegistrationDateAnchors.FRONT_SPAN && now > RegistrationDateAnchors.DATA_END) {
            double shift = (now - RegistrationDateAnchors.DATA_END) * (1.0 - distance / RegistrationDateAnchors.FRONT_SPAN);
            to += Math.round(shift);
        }
        to = Math.min(to, now);
        point = Math.min(point, to);
        from = Math.max(RegistrationDateAnchors.LAUNCH, Math.min(from, point));
        return new Result(point, from, to);
    }

    static long interpolate(int a, int b, double fraction) {
        return Math.round(a + ((double) b - a) * fraction);
    }

    static long clamp(double value, long lower, long upper) {
        if (!(value > lower)) return lower;
        if (value >= upper) return upper;
        return Math.round(value);
    }
}
