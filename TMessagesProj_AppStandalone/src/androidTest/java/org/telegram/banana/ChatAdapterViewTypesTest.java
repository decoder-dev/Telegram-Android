package org.telegram.banana;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.platform.app.InstrumentationRegistry;

import com.radolyn.ayugram.AyuConfig;
import com.radolyn.ayugram.ui.DummyView;

import org.junit.Test;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.banana.BananaConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Cells.BotAskCell;
import org.telegram.ui.Cells.BotHelpCell;
import org.telegram.ui.Cells.ChatActionCell;
import org.telegram.ui.Cells.ChatLoadingCell;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Cells.ChatMessageUnsupportedCell;
import org.telegram.ui.Cells.ChatUnreadCell;
import org.telegram.ui.Cells.FilteredMessageCell;
import org.telegram.ui.Cells.UserInfoCell;
import org.telegram.ui.Components.RecyclerListView;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

/** Exercises the row-type producer and RecyclerView factory without opening a real chat. */
public class ChatAdapterViewTypesTest {
    private static final int ACCOUNT = 0;
    private static final long SYNTHETIC_USER = 4_000_000_042L;

    @Test
    public void supplementaryRowsCreateTheCellsRequiredByTheirBinders() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = prepare();
            ChatActivity chat = newChat();
            ChatActivity.ChatActivityAdapter adapter = chat.new ChatActivityAdapter(context);
            RecyclerListView parent = new RecyclerListView(context);
            String[] rows = {"userInfoRow", "userNameTimeRow", "userPhotoTimeRow",
                    "botForumStartThreadRow", "botInfoRow", "hintRow", "hintRow2", "loadingUpRow"};
            Class<?>[] cells = {UserInfoCell.class, ChatActionCell.class, ChatActionCell.class,
                    BotAskCell.class, BotHelpCell.class, ChatActionCell.class,
                    ChatActionCell.class, ChatLoadingCell.class};
            // These slots are normally assigned by updateRowsInternal after peer settings arrive.
            // Drive getItemViewType as RecyclerView does, instead of calling the factory with literals.
            for (int i = 0; i < rows.length; i++) setInt(adapter, rows[i], i);
            for (int i = 0; i < rows.length; i++) {
                assertCell(adapter, parent, i, cells[i]);
            }
        });
    }

    @Test
    public void messageRowsUseTheActiveListAndKeepHiddenRowsDistinctFromDummyRows() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = prepare();
            ChatActivity chat = newChat();
            ChatActivity.ChatActivityAdapter adapter = chat.new ChatActivityAdapter(context);
            RecyclerListView parent = new RecyclerListView(context);
            SharedPreferences prefs = MessagesController.getMainSettings(ACCOUNT);
            String key = BananaConfig.Option.HIDE_BLOCKED_MESSAGES.key;
            Object oldHideBlocked = prefs.getAll().get(key);
            boolean oldAyuFilters = AyuConfig.regexFiltersEnabled;
            MessagesController controller = MessagesController.getInstance(ACCOUNT);
            int blockedIndex = controller.blockePeers.indexOfKey(SYNTHETIC_USER);
            int oldBlocked = controller.blockePeers.get(SYNTHETIC_USER);
            try {
                AyuConfig.regexFiltersEnabled = false;
                BananaConfig.set(ACCOUNT, BananaConfig.Option.HIDE_BLOCKED_MESSAGES, false);

                MessageObject text = message(false);
                MessageObject unsupported = message(true);
                assertEquals("Unsupported protocol media must reach its own factory", 10, unsupported.contentType);
                chat.messages.add(text);
                adapter.filteredMessages.add(unsupported);
                MessageObject unread = message(false);
                unread.contentType = 2; // The same synthetic separator used by ChatActivity.
                adapter.frozenMessages.add(unread);
                adapter.messagesStartRow = 0;
                setInt(adapter, "messagesEndRow", 1);

                adapter.isFiltered = true;
                assertCell(adapter, parent, 0, ChatMessageUnsupportedCell.class);
                adapter.isFrozen = true;
                assertCell(adapter, parent, 0, ChatUnreadCell.class);
                adapter.isFrozen = false;
                adapter.isFiltered = false;
                assertCell(adapter, parent, 0, ChatMessageCell.class);

                controller.blockePeers.put(SYNTHETIC_USER, 1);
                BananaConfig.set(ACCOUNT, BananaConfig.Option.HIDE_BLOCKED_MESSAGES, true);
                int hiddenType = adapter.getItemViewType(0);
                assertCell(adapter, parent, 0, FilteredMessageCell.class);

                BananaConfig.set(ACCOUNT, BananaConfig.Option.HIDE_BLOCKED_MESSAGES, false);
                text.contentType = -1000; // AyuGram's one-pixel message placeholder.
                int dummyType = adapter.getItemViewType(0);
                assertNotEquals("RecyclerView must never recycle a filtered cell as a DummyView", hiddenType, dummyType);
                RecyclerView.ViewHolder dummy = assertCell(adapter, parent, 0, DummyView.class);
                adapter.onBindViewHolder(dummy, 0);
                assertSame(text, ((DummyView) dummy.itemView).getMessageObject());
            } finally {
                AyuConfig.regexFiltersEnabled = oldAyuFilters;
                if (blockedIndex < 0) controller.blockePeers.delete(SYNTHETIC_USER);
                else controller.blockePeers.put(SYNTHETIC_USER, oldBlocked);
                SharedPreferences.Editor restore = prefs.edit().remove(key);
                if (oldHideBlocked instanceof Boolean) restore.putBoolean(key, (Boolean) oldHideBlocked);
                restore.apply();
            }
        });
    }

    private static Context prepare() {
        ApplicationLoader.postInitApplication();
        assertFalse("Use a fresh emulator without a signed-in account", UserConfig.getInstance(ACCOUNT).isClientActivated());
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    private static ChatActivity newChat() {
        Bundle args = new Bundle();
        args.putLong("user_id", SYNTHETIC_USER);
        ChatActivity chat = new ChatActivity(args);
        chat.setCurrentAccount(ACCOUNT);
        // Deliberately do not call onFragmentCreate: no observers, loading or server requests.
        return chat;
    }

    private static MessageObject message(boolean unsupported) {
        TLRPC.TL_message raw = new TLRPC.TL_message();
        raw.id = 42;
        raw.message = "adapter regression";
        raw.peer_id = new TLRPC.TL_peerUser();
        raw.peer_id.user_id = SYNTHETIC_USER;
        raw.from_id = new TLRPC.TL_peerUser();
        raw.from_id.user_id = SYNTHETIC_USER;
        if (unsupported) raw.media = new TLRPC.TL_messageMediaUnsupported();
        return new MessageObject(ACCOUNT, raw, false, false);
    }

    private static RecyclerView.ViewHolder assertCell(ChatActivity.ChatActivityAdapter adapter,
                                                     RecyclerListView parent, int position, Class<?> expected) {
        int type = adapter.getItemViewType(position);
        RecyclerView.ViewHolder holder = adapter.createViewHolder(parent, type);
        assertTrue("Row " + position + " (type " + type + ") needs " + expected.getSimpleName(),
                expected.isInstance(holder.itemView));
        assertTrue(holder.itemView.getLayoutParams() instanceof RecyclerView.LayoutParams);
        assertEquals(RecyclerView.LayoutParams.MATCH_PARENT, holder.itemView.getLayoutParams().width);
        assertEquals(RecyclerView.LayoutParams.WRAP_CONTENT, holder.itemView.getLayoutParams().height);
        return holder;
    }

    private static void setInt(Object target, String name, int value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.setInt(target, value);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }
}
