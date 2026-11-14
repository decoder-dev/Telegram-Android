package org.telegram.banana;

import android.content.Context;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Cells.StickerEmojiCell;
import org.telegram.ui.Cells.StickerSetNameCell;
import org.telegram.ui.Components.StickersRecyclerListView;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class StickerListViewTest {
    @Test
    public void headerAndStickerRowsCreateBindAndUpdateInTheRightDirection() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            ApplicationLoader.postInitApplication();
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            StickersRecyclerListView list = new StickersRecyclerListView(context, null);
            RecyclerView.Adapter adapter = list.getAdapter();
            int[] insertedAndRemoved = new int[2];
            adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                @Override
                public void onItemRangeInserted(int start, int count) {
                    insertedAndRemoved[0] += count;
                }

                @Override
                public void onItemRangeRemoved(int start, int count) {
                    insertedAndRemoved[1] += count;
                }
            });

            TLRPC.StickerSet set = new TLRPC.TL_stickerSet();
            set.id = 42;
            set.title = "Fixture pack";
            TLRPC.Document document = new TLRPC.TL_document();
            document.id = 43;
            document.mime_type = "image/webp";
            ArrayList<StickersRecyclerListView.Item> items = new ArrayList<>();
            items.add(new StickersRecyclerListView.StickerHeaderItem(set));
            items.add(new StickersRecyclerListView.StickerItem(document));
            list.setItems(items);
            assertEquals(2, adapter.getItemCount());
            assertEquals(2, insertedAndRemoved[0]);
            assertEquals(0, insertedAndRemoved[1]);

            RecyclerView.ViewHolder header = adapter.createViewHolder(list, adapter.getItemViewType(0));
            RecyclerView.ViewHolder sticker = adapter.createViewHolder(list, adapter.getItemViewType(1));
            adapter.bindViewHolder(header, 0);
            adapter.bindViewHolder(sticker, 1);
            assertTrue(header.itemView instanceof StickerSetNameCell);
            assertEquals(set.title, ((StickerSetNameCell) header.itemView).getTextView().getText().toString());
            assertTrue(sticker.itemView instanceof StickerEmojiCell);
            assertSame(document, ((StickerEmojiCell) sticker.itemView).getSticker());
            assertFalse(items.get(0).selectable);
            assertTrue(items.get(1).selectable);

            GridLayoutManager layout = (GridLayoutManager) list.getLayoutManager();
            assertEquals(5, layout.getSpanSizeLookup().getSpanSize(0));
            assertEquals(1, layout.getSpanSizeLookup().getSpanSize(1));
            list.setSpanCount(3);
            assertEquals(3, layout.getSpanSizeLookup().getSpanSize(0));

            items.set(0, new StickersRecyclerListView.StickerHeaderItem((TLRPC.StickerSet) null));
            items.set(1, new StickersRecyclerListView.StickerItem(null));
            list.setItems(items);
            adapter.bindViewHolder(header, 0);
            adapter.bindViewHolder(sticker, 1);
            assertEquals("", ((StickerSetNameCell) header.itemView).getTextView().getText().toString());
            assertEquals(View.INVISIBLE, sticker.itemView.getVisibility());
            assertFalse(items.get(1).selectable);

            int removedBeforeClear = insertedAndRemoved[1];
            list.setItems(new ArrayList<StickersRecyclerListView.Item>());
            assertEquals(0, adapter.getItemCount());
            assertEquals(removedBeforeClear + 2, insertedAndRemoved[1]);
        });
    }

    @Test
    public void emptyItemsHaveReflexiveEquality() {
        StickersRecyclerListView.StickerHeaderItem header =
                new StickersRecyclerListView.StickerHeaderItem((TLRPC.StickerSet) null);
        StickersRecyclerListView.StickerItem sticker = new StickersRecyclerListView.StickerItem(null);
        assertEquals(header, header);
        assertEquals(header, new StickersRecyclerListView.StickerHeaderItem((TLRPC.TL_messages_stickerSet) null));
        assertEquals(sticker, sticker);
        assertEquals(sticker, new StickersRecyclerListView.StickerItem(null));
        assertNotEquals(header, sticker);
        assertNotEquals(header, null);
        assertNotEquals(sticker, null);
    }
}
