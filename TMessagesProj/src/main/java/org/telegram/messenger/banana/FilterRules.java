package org.telegram.messenger.banana;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** AyuGram's documented v2 interchange format. Parsing never compiles untrusted regex. */
public final class FilterRules {
    public static final int MAX_RULES = 64, MAX_JSON = 262144, MAX_EXCLUSIONS = 2048;
    public static final class Rule {
        public final String id, text;
        public final long dialogId;
        public final boolean reversed, caseInsensitive, enabled;
        public Rule(String id, long dialogId, String text, boolean reversed, boolean caseInsensitive, boolean enabled) {
            if (id == null || !UUID.fromString(id).toString().equalsIgnoreCase(id) || text == null ||
                    text.isEmpty() || text.length() > RegexProtocol.MAX_PATTERN) throw new IllegalArgumentException("Invalid filter");
            this.id = id.toLowerCase(java.util.Locale.ROOT); this.dialogId = dialogId; this.text = text;
            this.reversed = reversed; this.caseInsensitive = caseInsensitive; this.enabled = enabled;
        }
        public String key() { return id + ":" + dialogId; }
        public Rule withEnabled(boolean value) { return new Rule(id, dialogId, text, reversed, caseInsensitive, value); }
    }
    public final List<Rule> rules;
    private final Set<String> exclusions;
    public FilterRules(List<Rule> rules, Set<String> exclusions) {
        if (rules.size() > MAX_RULES || exclusions.size() > MAX_EXCLUSIONS) throw new IllegalArgumentException("Too many filters");
        this.rules = Collections.unmodifiableList(new ArrayList<>(rules));
        this.exclusions = Collections.unmodifiableSet(new HashSet<>(exclusions));
    }
    public static FilterRules empty() { return new FilterRules(Collections.emptyList(), Collections.emptySet()); }
    public boolean applies(Rule rule, long dialogId, boolean channel, boolean sharedInChats) {
        return rule.enabled && (rule.dialogId == dialogId || rule.dialogId == 0 &&
                (channel || sharedInChats) && !exclusions.contains(rule.id + ":" + dialogId));
    }
    public FilterRules put(Rule rule) {
        ArrayList<Rule> result = new ArrayList<>(rules);
        result.removeIf(old -> old.key().equals(rule.key())); result.add(rule);
        return new FilterRules(result, exclusions);
    }
    public FilterRules remove(Rule rule) {
        ArrayList<Rule> result = new ArrayList<>(rules);
        result.removeIf(old -> old.key().equals(rule.key()));
        return new FilterRules(result, exclusions);
    }
    public boolean excluded(Rule rule, long dialog) { return exclusions.contains(rule.id + ":" + dialog); }
    public FilterRules exclude(Rule rule, long dialog, boolean excluded) {
        HashSet<String> values = new HashSet<>(exclusions);
        if (excluded) values.add(rule.id + ":" + dialog); else values.remove(rule.id + ":" + dialog);
        return new FilterRules(rules, values);
    }
    public String export() throws JSONException {
        JSONObject root = new JSONObject().put("version", 2);
        JSONArray filters = new JSONArray(), excluded = new JSONArray();
        for (Rule rule : rules) filters.put(new JSONObject().put("id", rule.id).put("dialogId", rule.dialogId == 0 ? JSONObject.NULL : rule.dialogId)
                .put("text", rule.text).put("reversed", rule.reversed).put("caseInsensitive", rule.caseInsensitive).put("enabled", rule.enabled));
        ArrayList<String> sorted = new ArrayList<>(exclusions); Collections.sort(sorted);
        for (String key : sorted) {
            int colon = key.indexOf(':');
            excluded.put(new JSONObject().put("filterId", key.substring(0, colon)).put("dialogId", Long.parseLong(key.substring(colon + 1))));
        }
        String result = root.put("filters", filters).put("exclusions", excluded).toString(2);
        if (result.length() > MAX_JSON || result.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_JSON) throw new IllegalArgumentException("Filter export too large");
        return result;
    }
    /** Returns a preview; the caller must explicitly confirm before persisting imports. */
    public FilterRules merge(String json) throws JSONException {
        if (json == null || json.length() > MAX_JSON || json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_JSON) throw new IllegalArgumentException("Filter import too large");
        checkNesting(json);
        JSONObject root = new JSONObject(json);
        Object version = root.get("version");
        if (!(version instanceof Integer || version instanceof Long) || ((Number) version).longValue() != 2) throw new IllegalArgumentException("Unsupported filter version");
        LinkedHashMap<String, Rule> values = new LinkedHashMap<>();
        for (Rule rule : rules) values.put(rule.key(), rule);
        JSONArray remove = array(root, "removeFiltersById", MAX_RULES);
        for (int i = 0; i < remove.length(); i++) {
            String id = UUID.fromString(remove.getString(i)).toString();
            values.values().removeIf(rule -> rule.id.equals(id));
        }
        JSONArray filters = array(root, "filters", MAX_RULES);
        Set<String> incoming = new HashSet<>();
        for (int i = 0; i < filters.length(); i++) {
            JSONObject item = filters.getJSONObject(i);
            Rule rule = new Rule(item.getString("id"), item.isNull("dialogId") ? 0 : peer(item.get("dialogId")),
                    item.getString("text"), bool(item, "reversed", false), bool(item, "caseInsensitive", false), bool(item, "enabled", true));
            if (!incoming.add(rule.key())) throw new IllegalArgumentException("Duplicate filter");
            values.put(rule.key(), rule);
        }
        HashSet<String> excluded = new HashSet<>(exclusions);
        for (String field : new String[]{"removeExclusions", "exclusions"}) {
            JSONArray items = array(root, field, MAX_EXCLUSIONS);
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                String key = UUID.fromString(item.getString("filterId")).toString() + ":" + peer(item.get("dialogId"));
                if (field.equals("exclusions")) excluded.add(key); else excluded.remove(key);
            }
        }
        return new FilterRules(new ArrayList<>(values.values()), excluded);
    }
    /** Android's recursive JSON parser has no nesting bound. Reject deep input before invoking it. */
    private static void checkNesting(String json) {
        int depth = 0;
        boolean string = false, escaped = false, started = false, finished = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (string) {
                if (c < 32) throw new IllegalArgumentException("Invalid JSON string");
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == '"') string = false;
                continue;
            }
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') continue;
            if (finished || !started && c != '{') throw new IllegalArgumentException("Invalid JSON root");
            started = true;
            if (c == '"') string = true;
            else if (c == '{' || c == '[') {
                if (++depth > 24) throw new IllegalArgumentException("Filter JSON too deeply nested");
            } else if (c == '}' || c == ']') {
                if (--depth < 0) throw new IllegalArgumentException("Unbalanced JSON");
                if (depth == 0) finished = true;
            } else if (c == '/' || c == '\'' || c == '#') {
                // Lenient JSONTokener comments/single quotes could confuse the lexical bound.
                throw new IllegalArgumentException("Use standard JSON");
            }
        }
        if (!finished || string || depth != 0) throw new IllegalArgumentException("Incomplete JSON");
    }
    private static JSONArray array(JSONObject root, String key, int max) throws JSONException {
        JSONArray values = root.has(key) ? root.getJSONArray(key) : new JSONArray();
        if (values.length() > max) throw new IllegalArgumentException("Too many filter entries");
        return values;
    }
    private static long peer(Object value) {
        if (value instanceof Integer || value instanceof Long) return ((Number) value).longValue();
        if (value instanceof String) {
            String text = (String) value;
            int start = text.startsWith("-") ? 1 : 0;
            if (text.length() <= start || text.length() > 20) throw new IllegalArgumentException("Invalid dialog id");
            for (int i = start; i < text.length(); i++) if (text.charAt(i) < '0' || text.charAt(i) > '9') throw new IllegalArgumentException("Invalid dialog id");
            return Long.parseLong(text);
        }
        throw new IllegalArgumentException("Invalid dialog id");
    }
    private static boolean bool(JSONObject item, String key, boolean fallback) throws JSONException {
        if (!item.has(key)) return fallback;
        Object value = item.get(key);
        if (!(value instanceof Boolean)) throw new IllegalArgumentException("Invalid filter option");
        return (Boolean) value;
    }
}
