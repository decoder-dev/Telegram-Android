package org.telegram.banana;

import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.banana.FilterRules;
import org.telegram.messenger.banana.FilterStore;
import org.telegram.messenger.banana.MessageFilters;
import org.telegram.tgnet.TLRPC;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

public class FilterRulesTest {
    private static FilterRules.Rule rule(String id, long dialog, String text) {
        return new FilterRules.Rule(id, dialog, text, false, true, true);
    }
    @Test public void v2RoundTripSupportsSharedPerChatExclusionsAndMaintainerUpdates() throws Exception {
        String id = UUID.randomUUID().toString(), local = UUID.randomUUID().toString();
        FilterRules.Rule shared = rule(id, 0, "(?i)offer"), scoped = rule(id, 42, "different"), other = rule(local, 42, "x");
        FilterRules original = FilterRules.empty().put(shared).put(scoped).put(other).exclude(shared, 42, true);
        FilterRules restored = FilterRules.empty().merge(original.export());
        assertEquals(3, restored.rules.size());
        assertFalse(restored.applies(shared, 42, true, true));
        assertTrue(restored.applies(shared, 43, true, false));
        assertFalse(restored.applies(shared, 43, false, false));
        assertTrue(restored.applies(scoped, 42, false, false));
        FilterRules updated = restored.merge("{\"version\":2,\"removeFiltersById\":[\"" + local + "\"],\"removeExclusions\":[{\"filterId\":\"" + id + "\",\"dialogId\":42}]}");
        assertEquals(2, updated.rules.size());
        assertTrue(updated.applies(shared, 42, false, true));
        assertEquals("Import must be a preview, not mutate current data", 3, original.rules.size());
        assertTrue(original.excluded(shared, 42));
    }
    @Test public void malformedAndOversizedImportsAreAtomicAndNeverCompileRegex() throws Exception {
        FilterRules original = FilterRules.empty().put(rule(UUID.randomUUID().toString(), 0, "original"));
        for (String invalid : new String[]{"{}", "{\"version\":3}", "{\"version\":2.5}", "{\"version\":2,\"filters\":{}}", "{\"version\":2,\"filters\":[{\"id\":\"invalid\",\"text\":\"x\"}]}",
                "{\"version\":2,\"filters\":[{\"id\":\"" + UUID.randomUUID() + "\",\"dialogId\":42.9,\"text\":\"x\"}]}",
                new String(new char[FilterRules.MAX_JSON + 1])}) {
            try { original.merge(invalid); fail("Invalid imports must be rejected"); }
            catch (org.json.JSONException | IllegalArgumentException expected) { }
            assertEquals(1, original.rules.size());
        }
        try { original.merge(new String(new char[FilterRules.MAX_JSON / 2]).replace('\0', '界')); fail("UTF-8 size, not just UTF-16 character count, must be bounded"); }
        catch (IllegalArgumentException expected) { }
        String nested = "{\"unknown\":" + new String(new char[10000]).replace("\0", "[") + "0" + new String(new char[10000]).replace("\0", "]") + ",\"version\":2}";
        for (String unsafe : new String[]{nested, "{\"version\":2} trailing", "{\"version\":2,/* comment */\"filters\":[]}"}) {
            try { original.merge(unsafe); fail("Unsafe JSON must fail before the platform recursive parser"); }
            catch (IllegalArgumentException expected) { }
        }
        FilterRules invalidRegex = original.merge("{\"version\":2,\"filters\":[{\"id\":\"" + UUID.randomUUID() + "\",\"dialogId\":null,\"text\":\"[\"}]}");
        assertEquals("Regex is checked only in the isolated process", 2, invalidRegex.rules.size());
    }
    private MessageObject message(int account, String text) {
        TLRPC.TL_message raw = new TLRPC.TL_message();
        raw.id = 17; raw.date = 1000; raw.message = text;
        raw.peer_id = new TLRPC.TL_peerUser(); raw.peer_id.user_id = 42;
        raw.from_id = new TLRPC.TL_peerUser(); raw.from_id.user_id = 42;
        return new MessageObject(account, raw, false, false);
    }
    private MessageFilters.Verdict verdict(int account, MessageObject message) {
        AtomicReference<MessageFilters.Verdict> result = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> result.set(MessageFilters.verdict(account, message, null)));
        return result.get();
    }
    private void awaitVerdict(int account, MessageObject message, MessageFilters.Verdict expected) throws Exception {
        long deadline = android.os.SystemClock.elapsedRealtime() + 8000;
        while (android.os.SystemClock.elapsedRealtime() < deadline) {
            MessageFilters.Verdict result = verdict(account, message);
            if (result != MessageFilters.Verdict.PENDING) { assertEquals(expected, result); return; }
            Thread.sleep(30);
        }
        fail("The rule verdict did not arrive");
    }
    @Test public void cacheCannotLeakBetweenAccountsContentEditsAndSettingRevisions() throws Exception {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> org.telegram.messenger.ApplicationLoader.postInitApplication());
        FilterStore[] stores = {FilterStore.get(0), FilterStore.get(1)};
        for (int account = 0; account < 2; account++) assertFalse("Only use empty emulator accounts", UserConfig.getInstance(account).isClientActivated());
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                stores[0].save(FilterRules.empty().put(rule(UUID.randomUUID().toString(), 0, "offer")));
                stores[0].setSharedInChats(true); stores[0].setEnabled(true);
                stores[1].save(FilterRules.empty()); stores[1].setEnabled(false);
            });
            MessageObject filtered = message(0, "special OFFER");
            awaitVerdict(0, filtered, MessageFilters.Verdict.HIDDEN);
            org.telegram.ui.ChatActivity[] chat = new org.telegram.ui.ChatActivity[1];
            org.telegram.ui.ChatActivity.ChatActivityAdapter[] adapter = new org.telegram.ui.ChatActivity.ChatActivityAdapter[1];
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                // Construct the native adapter without onFragmentCreate/onResume: no login,
                // server requests or live chat storage. Only a synthetic message-row range.
                chat[0] = new org.telegram.ui.ChatActivity(new android.os.Bundle()); chat[0].setCurrentAccount(0);
                chat[0].messages.add(filtered);
                adapter[0] = chat[0].new ChatActivityAdapter(InstrumentationRegistry.getInstrumentation().getTargetContext());
                adapter[0].messagesStartRow = 0;
                try {
                    java.lang.reflect.Field end = adapter[0].getClass().getDeclaredField("messagesEndRow"); end.setAccessible(true); end.setInt(adapter[0], 1);
                } catch (Exception error) { throw new AssertionError(error); }
                int type = adapter[0].getItemViewType(0);
                assertNotEquals("Native chat hook must replace the message row", filtered.contentType, type);
                androidx.recyclerview.widget.RecyclerView.ViewHolder holder = adapter[0].onCreateViewHolder(new android.widget.FrameLayout(InstrumentationRegistry.getInstrumentation().getTargetContext()), type);
                assertTrue(holder.itemView instanceof org.telegram.ui.Cells.FilteredMessageCell);
                adapter[0].onBindViewHolder(holder, 0);
                holder.itemView.measure(android.view.View.MeasureSpec.makeMeasureSpec(600, android.view.View.MeasureSpec.EXACTLY), 0);
                assertEquals(1, holder.itemView.getMeasuredHeight());
                assertEquals(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS, holder.itemView.getImportantForAccessibility());
                assertEquals(filtered.stableId, adapter[0].getItemId(0));
                try {
                    java.lang.reflect.Method select = chat[0].getClass().getDeclaredMethod("addToSelectedMessages", MessageObject.class, boolean.class);
                    select.setAccessible(true); select.invoke(chat[0], filtered, false);
                    java.lang.reflect.Field selected = chat[0].getClass().getDeclaredField("selectedMessagesIds"); selected.setAccessible(true);
                    for (android.util.SparseArray<?> values : (android.util.SparseArray<?>[]) selected.get(chat[0])) assertEquals("Hidden rows cannot enter copy/forward selection", 0, values.size());
                } catch (Exception error) { throw new AssertionError(error); }
            });
            assertEquals(MessageFilters.Verdict.VISIBLE, verdict(1, message(1, "special OFFER")));
            MessageObject edit = message(0, "normal update"); edit.messageOwner.edit_date = 123;
            awaitVerdict(0, edit, MessageFilters.Verdict.VISIBLE);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                chat[0].messages.set(0, edit);
                assertEquals("Edited text must regain its native message row", edit.contentType, adapter[0].getItemViewType(0));
            });
            assertEquals(MessageFilters.Verdict.HIDDEN, verdict(0, filtered));
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> stores[0].setEnabled(false));
            assertEquals(MessageFilters.Verdict.VISIBLE, verdict(0, filtered));
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                stores[0].save(FilterRules.empty().put(new FilterRules.Rule(UUID.randomUUID().toString(), 42, "allow", true, true, true)));
                stores[0].setSharedInChats(false); stores[0].setEnabled(true);
            });
            awaitVerdict(0, message(0, "allowed"), MessageFilters.Verdict.VISIBLE);
            awaitVerdict(0, message(0, "other message"), MessageFilters.Verdict.HIDDEN);
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                for (FilterStore store : stores) { store.setEnabled(false); store.setSharedInChats(false); store.save(FilterRules.empty()); }
            });
        }
    }
}
