package org.telegram.messenger.banana;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Approximate Last Seen (ported from BananaGram iOS): for someone who hides their status, "last seen
 * recently" becomes the time of their latest activity this device saw (a message from them), when it
 * is under three days old. Kept in memory only, and only while the switch is on.
 */
public final class ApproximateLastSeen {
    public static final int MAXIMUM_AGE = 3 * 24 * 60 * 60;
    private static final ConcurrentHashMap<Integer, ConcurrentHashMap<Long, Integer>> ACTIVITY = new ConcurrentHashMap<>();

    private ApproximateLastSeen() {}

    public static void record(int account, long userId, int date) {
        if (BananaConfig.get(account, BananaConfig.Option.APPROXIMATE_LAST_SEEN)) put(account, userId, date);
    }

    static void put(int account, long userId, int date) {
        if (userId <= 0 || date <= 0) return;
        ACTIVITY.computeIfAbsent(account, key -> new ConcurrentHashMap<>()).merge(userId, date, Integer::max);
    }

    static int latest(int account, long userId, int now) {
        ConcurrentHashMap<Long, Integer> map = ACTIVITY.get(account);
        Integer value = map != null ? map.get(userId) : null;
        return value != null && showable(value, now) ? value : 0;
    }

    static boolean showable(int activity, int now) {
        long age = (long) now - activity;
        return activity > 0 && age >= 0 && age < MAXIMUM_AGE;
    }

    public static void clear(int account) {
        ACTIVITY.remove(account);
    }

    /** "last seen today at ~14:22" and the like, or null to keep "last seen recently". */
    public static String format(int account, long userId, int now) {
        if (!BananaConfig.get(account, BananaConfig.Option.APPROXIMATE_LAST_SEEN)) return null;
        int activity = latest(account, userId, now);
        if (activity == 0) return null;
        Date date = new Date(activity * 1000L);
        String time = "~" + LocaleController.getInstance().getFormatterDay().format(date);
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_YEAR), year = calendar.get(Calendar.YEAR);
        calendar.setTime(date);
        int activityDay = calendar.get(Calendar.DAY_OF_YEAR), activityYear = calendar.get(Calendar.YEAR);
        if (day == activityDay && year == activityYear) {
            return LocaleController.formatString(R.string.LastSeenFormatted, LocaleController.formatString(R.string.TodayAtFormatted, time));
        } else if (activityDay + 1 == day && year == activityYear) {
            return LocaleController.formatString(R.string.LastSeenFormatted, LocaleController.formatString(R.string.YesterdayAtFormatted, time));
        }
        return LocaleController.formatString(R.string.LastSeenDateFormatted, LocaleController.formatString(R.string.formatDateAtTime,
                LocaleController.getInstance().getFormatterDayMonth().format(date), time));
    }
}
