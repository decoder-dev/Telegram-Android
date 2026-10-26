package org.telegram.messenger.banana;

import android.content.SharedPreferences;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;

/** Immutable snapshots, scoped by account slot, actual owner and Telegram backend. */
public final class FilterStore {
    private static final FilterStore[] instances = new FilterStore[UserConfig.MAX_ACCOUNT_COUNT];
    public final int account;
    public final long owner;
    public final boolean test;
    private final SharedPreferences preferences;
    private final String key;
    private volatile FilterRules rules;
    private volatile boolean enabled, sharedInChats;
    private volatile long generation;
    private volatile int error;
    private FilterStore(int account, long owner, boolean test) {
        this.account = account; this.owner = owner; this.test = test;
        preferences = MessagesController.getMainSettings(account);
        key = "bananaFiltersV2." + owner + (test ? ".test" : ".production");
        try { rules = FilterRules.empty().merge(preferences.getString(key, "{\"version\":2}")); }
        catch (Exception ignored) { rules = FilterRules.empty(); error = RegexProtocol.EVALUATION_ERROR; }
        enabled = preferences.getBoolean(key + ".enabled", false);
        sharedInChats = preferences.getBoolean(key + ".chats", false);
    }
    public static synchronized FilterStore get(int account) {
        long owner = UserConfig.getInstance(account).getClientUserId();
        boolean test = ConnectionsManager.getInstance(account).isTestBackend();
        FilterStore value = instances[account];
        if (value == null || value.owner != owner || value.test != test) instances[account] = value = new FilterStore(account, owner, test);
        return value;
    }
    public FilterRules rules() { return rules; }
    public boolean enabled() { return enabled; }
    public boolean sharedInChats() { return sharedInChats; }
    public long generation() { return generation; }
    public int error() { return error; }
    public void error(int status) { error = status; }
    public boolean isCurrent() {
        return UserConfig.getInstance(account).getClientUserId() == owner &&
                ConnectionsManager.getInstance(account).isTestBackend() == test;
    }
    public synchronized void save(FilterRules value) {
        if (!isCurrent()) throw new IllegalStateException("Filter account changed");
        try {
            String json = value.export();
            preferences.edit().putString(key, json).apply();
            rules = value; generation++; error = 0;
        } catch (org.json.JSONException error) { throw new IllegalArgumentException("Invalid filter data"); }
        MessageFilters.settingsChanged(account);
    }
    public synchronized void setEnabled(boolean value) {
        if (!isCurrent()) return;
        preferences.edit().putBoolean(key + ".enabled", value).apply(); enabled = value; generation++; error = 0;
        MessageFilters.settingsChanged(account);
    }
    public synchronized void setSharedInChats(boolean value) {
        if (!isCurrent()) return;
        preferences.edit().putBoolean(key + ".chats", value).apply(); sharedInChats = value; generation++; error = 0;
        MessageFilters.settingsChanged(account);
    }
}
