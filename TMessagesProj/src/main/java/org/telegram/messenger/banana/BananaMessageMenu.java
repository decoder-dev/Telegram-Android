package org.telegram.messenger.banana;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Message menu order and hidden items (ported from BananaGram iOS). Keys are the iOS raw values, so
 * stored settings mean the same on both apps. Items without a key keep their place.
 */
public final class BananaMessageMenu {
    /** Stock order of the items Android has. */
    public static final List<String> KEYS = Collections.unmodifiableList(Arrays.asList(
            "reply", "editHistory", "copy", "translate", "saveMedia", "saveToFiles", "convertAudio", "viewReplies",
            "edit", "pin", "copyLink", "saveGif", "forward", "saveToSavedMessages", "screenshot", "bookmarks",
            "selectFromAuthor", "repeatMessage", "messageJson", "report", "statistics", "delete"));

    private BananaMessageMenu() {}

    public static List<String> parse(String csv) {
        ArrayList<String> keys = new ArrayList<>();
        if (csv == null) return keys;
        for (String part : csv.split(",")) {
            String key = part.trim();
            if (KEYS.contains(key) && !keys.contains(key)) keys.add(key);
        }
        return keys;
    }

    public static String format(List<String> keys) {
        StringBuilder out = new StringBuilder();
        for (String key : keys) {
            if (!KEYS.contains(key)) continue;
            if (out.length() > 0) out.append(',');
            out.append(key);
        }
        return out.toString();
    }

    /** The stored order, then every other item in stock order. */
    public static List<String> order(List<String> stored) {
        LinkedHashSet<String> order = new LinkedHashSet<>();
        if (stored != null) for (String key : stored) if (KEYS.contains(key)) order.add(key);
        order.addAll(KEYS);
        return new ArrayList<>(order);
    }

    /**
     * Source indices to keep, in their new order: hidden keyed items go; keyed items are sorted by the
     * user's order (equal ranks keep their order); items without a key (null) keep their slots.
     */
    public static int[] arrange(List<String> keys, Set<String> hidden, List<String> stored) {
        List<String> order = order(stored);
        Map<String, Integer> rank = new HashMap<>();
        for (int i = 0; i < order.size(); i++) rank.put(order.get(i), i);
        ArrayList<Integer> keyed = new ArrayList<>();
        ArrayList<Integer> slots = new ArrayList<>();
        ArrayList<Integer> result = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            if (key != null && hidden != null && hidden.contains(key)) continue;
            result.add(i);
            if (key != null && rank.containsKey(key)) {
                keyed.add(i);
                slots.add(result.size() - 1);
            }
        }
        ArrayList<Integer> sorted = new ArrayList<>(keyed);
        Collections.sort(sorted, (a, b) -> Integer.compare(rank.get(keys.get(a)), rank.get(keys.get(b))));
        for (int i = 0; i < slots.size(); i++) result.set(slots.get(i), sorted.get(i));
        int[] out = new int[result.size()];
        for (int i = 0; i < out.length; i++) out[i] = result.get(i);
        return out;
    }
}
