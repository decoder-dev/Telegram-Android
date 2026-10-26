package org.telegram.messenger.banana;

import android.os.Looper;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Pattern;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_keyboard;

/** Asynchronous AyuGram-style filters. UI caches include exact content, owner and config revision. */
public final class MessageFilters {
    public enum Verdict { VISIBLE, PENDING, HIDDEN }
    private static final MessageFilters[] instances = new MessageFilters[UserConfig.MAX_ACCOUNT_COUNT];
    private static final int TEXT_LIMIT = 16384, CACHE_SIZE = 256;
    private final FilterStore store;
    private final RegexEngine engine = new RegexEngine(ApplicationLoader.applicationContext);
    private final LinkedHashMap<Key, Verdict> cache = new LinkedHashMap<>(256, .75f, true);
    private final ArrayDeque<Key> queue = new ArrayDeque<>();
    private final WeakHashMap<Runnable, Boolean> listeners = new WeakHashMap<>();
    private final Set<String> failedRules = new HashSet<>();
    private long generation, storeGeneration = -1;
    private boolean running, notifyPosted;
    private final Runnable drain = this::drain;
    private MessageFilters(FilterStore store) { this.store = store; }

    private static MessageFilters get(int account) {
        FilterStore store = FilterStore.get(account);
        MessageFilters value = instances[account];
        if (value == null || value.store != store) {
            if (value != null) { value.generation++; value.queue.clear(); value.engine.close(); }
            instances[account] = value = new MessageFilters(store);
        }
        if (value.storeGeneration != store.generation()) {
            value.storeGeneration = store.generation(); value.generation++;
            value.cache.clear(); value.queue.clear(); value.failedRules.clear();
        }
        return value;
    }
    public static void settingsChanged(int account) {
        AndroidUtilities.runOnUIThread(() -> get(account).notifyChanged());
    }
    public static void addListener(int account, Runnable listener) { get(account).listeners.put(listener, true); }
    public static void removeListener(int account, Runnable listener) {
        MessageFilters value = instances[account];
        if (value != null) value.listeners.remove(listener);
    }
    /**
     * Best-effort sync hide for the notifications queue (fail-open). Display path uses
     * {@link #verdict} + IsolatedRegexService; this only peeks short message text.
     */
    public static boolean shouldHideNotification(int account, MessageObject message) {
        if (message == null || message.messageOwner == null || message.getId() <= 0 ||
                message.messageOwner instanceof TLRPC.TL_messageService) return false;
        if (!BananaHooks.regexFiltersEnabled(account)) return false;
        FilterStore store = FilterStore.get(account);
        if (store.rules().rules.isEmpty()) return false;
        String body = message.messageOwner.message;
        if (body == null || body.isEmpty() || body.length() > 2048) return false;
        long dialog = message.getDialogId();
        TLRPC.Chat chat = dialog < 0 ? MessagesController.getInstance(account).getChat(-dialog) : null;
        boolean channel = ChatObject.isChannel(chat) && chat != null && !chat.megagroup;
        boolean globalInsensitive = BananaHooks.regexFiltersCaseInsensitive(account);
        for (FilterRules.Rule rule : store.rules().rules) {
            if (!rule.enabled || !store.rules().applies(rule, dialog, channel, store.sharedInChats())) continue;
            try {
                boolean insensitive = rule.caseInsensitive || globalInsensitive;
                int flags = Pattern.MULTILINE | (insensitive ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0);
                boolean matched = Pattern.compile(rule.text, flags).matcher(body).find();
                if (rule.reversed != matched) return true;
            } catch (Throwable ignore) {
                // Fail open: never drop a notification because a rule blew up.
            }
        }
        return false;
    }

    public static Verdict verdict(int account, MessageObject message, MessageObject.GroupedMessages group) {
        if (message == null || message.messageOwner == null || message.getId() <= 0 ||
                message.messageOwner instanceof TLRPC.TL_messageService || Looper.myLooper() != Looper.getMainLooper()) return Verdict.VISIBLE;
        FilterStore store = FilterStore.get(account);
        if (!BananaHooks.regexFiltersEnabled(account) || store.rules().rules.isEmpty()) return Verdict.VISIBLE;
        MessageFilters filters = get(account);
        long dialog = message.getDialogId();
        TLRPC.Chat chat = dialog < 0 ? MessagesController.getInstance(account).getChat(-dialog) : null;
        boolean channel = ChatObject.isChannel(chat) && !chat.megagroup;
        boolean applicable = false;
        for (FilterRules.Rule rule : store.rules().rules) if (!filters.failedRules.contains(rule.key()) &&
                store.rules().applies(rule, dialog, channel, store.sharedInChats())) { applicable = true; break; }
        if (!applicable) return Verdict.VISIBLE;
        String text = text(message, group);
        if (text == null) return Verdict.VISIBLE;
        Key key = new Key(dialog, group != null ? message.getGroupId() : message.getId(), channel, text);
        Verdict result = filters.cache.get(key);
        if (result != null) return result;
        if (filters.queue.size() >= CACHE_SIZE) return Verdict.VISIBLE;
        filters.cache.put(key, Verdict.PENDING);
        while (filters.cache.size() > CACHE_SIZE) filters.cache.remove(filters.cache.keySet().iterator().next());
        filters.queue.add(key);
        AndroidUtilities.cancelRunOnUIThread(filters.drain);
        AndroidUtilities.runOnUIThread(filters.drain);
        return Verdict.PENDING;
    }
    /** The documented button/type tags are appended without modifying Telegram's MessageObject. */
    private static String text(MessageObject message, MessageObject.GroupedMessages group) {
        StringBuilder text = new StringBuilder();
        if (group != null && group.messages != null) {
            for (MessageObject child : group.messages) if (!append(text, child)) return null;
        } else if (!append(text, message)) return null;
        return text.toString();
    }
    private static boolean append(StringBuilder text, MessageObject message) {
        TLRPC.Message raw = message.messageOwner;
        if (raw == null || raw.message != null && raw.message.length() + text.length() > TEXT_LIMIT) return false;
        if (raw.message != null) text.append(raw.message);
        text.append('\n');
        if (raw.reply_markup instanceof TLRPC.TL_replyInlineMarkup) {
            int buttons = 0;
            for (TL_keyboard.KeyboardInlineButtonRow row : ((TLRPC.TL_replyInlineMarkup) raw.reply_markup).rows) {
                for (TL_keyboard.KeyboardInlineButton button : row.buttons) {
                    if (++buttons > 64 || button.text != null && text.length() + button.text.length() > TEXT_LIMIT) return false;
                    text.append("<button>").append(button.text == null ? "" : button.text);
                    String url = button.getUrl();
                    if (url != null && text.length() + url.length() > TEXT_LIMIT) return false;
                    if (url != null) text.append(' ').append(url);
                    text.append("</button>\n");
                }
            }
        }
        text.append("<type>").append(message.type).append("</type>\n");
        return text.length() <= TEXT_LIMIT;
    }
    private void drain() {
        if (running || queue.isEmpty() || !store.isCurrent()) return;
        running = true;
        ArrayList<Key> batch = new ArrayList<>();
        Key first = queue.peek();
        while (!queue.isEmpty() && batch.size() < 16 && queue.peek().dialog == first.dialog && queue.peek().channel == first.channel) batch.add(queue.remove());
        ArrayList<FilterRules.Rule> rules = new ArrayList<>();
        for (FilterRules.Rule rule : store.rules().rules) if (!failedRules.contains(rule.key()) &&
                store.rules().applies(rule, first.dialog, first.channel, store.sharedInChats())) rules.add(rule);
        evaluate(batch, new boolean[batch.size()], rules, 0, generation);
    }
    private void evaluate(ArrayList<Key> batch, boolean[] hidden, ArrayList<FilterRules.Rule> rules, int index, long revision) {
        if (revision != generation || !store.isCurrent()) { finishStale(); return; }
        if (index == rules.size()) {
            for (int i = 0; i < batch.size(); i++) if (cache.containsKey(batch.get(i))) cache.put(batch.get(i), hidden[i] ? Verdict.HIDDEN : Verdict.VISIBLE);
            running = false; notifyChanged(); AndroidUtilities.runOnUIThread(drain); return;
        }
        FilterRules.Rule rule = rules.get(index);
        String[] text = new String[batch.size()];
        for (int i = 0; i < text.length; i++) text[i] = batch.get(i).text;
        boolean insensitive = rule.caseInsensitive || BananaHooks.regexFiltersCaseInsensitive(store.account);
        int flags = Pattern.MULTILINE | (insensitive ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0);
        engine.match(rule.text, flags, text, result -> {
            if (revision != generation || !store.isCurrent()) { finishStale(); return; }
            if (result.status != RegexProtocol.OK) {
                // Quarantine this rule until the user edits or reapplies settings; never loop on ReDoS.
                failedRules.add(rule.key()); store.error(result.status); generation++;
                cache.clear(); queue.clear(); running = false; notifyChanged(); return;
            }
            for (int i = 0; i < hidden.length; i++) hidden[i] |= rule.reversed != result.matches[i];
            evaluate(batch, hidden, rules, index + 1, revision);
        });
    }
    private void finishStale() { running = false; AndroidUtilities.runOnUIThread(drain); }
    private void notifyChanged() {
        if (notifyPosted) return;
        notifyPosted = true;
        AndroidUtilities.runOnUIThread(() -> {
            notifyPosted = false;
            if (!store.isCurrent()) return;
            for (Runnable listener : new ArrayList<>(listeners.keySet())) if (listener != null) listener.run();
            NotificationCenter.getInstance(store.account).postNotificationName(NotificationCenter.dialogsNeedReload);
        }, 100);
    }
    private static final class Key {
        final long dialog, id;
        final boolean channel;
        final String text;
        Key(long dialog, long id, boolean channel, String text) { this.dialog = dialog; this.id = id; this.channel = channel; this.text = text; }
        @Override public boolean equals(Object value) {
            if (!(value instanceof Key)) return false;
            Key other = (Key) value;
            return dialog == other.dialog && id == other.id && channel == other.channel && text.equals(other.text);
        }
        @Override public int hashCode() { return Objects.hash(dialog, id, channel, text); }
    }
}
