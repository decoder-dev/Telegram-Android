package org.telegram.ui.Cells;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

/** Native Telegram switch with a label that grows instead of being truncated. */
public final class BananaSwitchCell extends TextCheckCell {
    public BananaSwitchCell(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context, resourcesProvider);
        getTextView().setSingleLine(false);
        getTextView().setMaxLines(Integer.MAX_VALUE);
        getTextView().setEllipsize(null);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        TextView text = getTextView();
        ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) text.getLayoutParams();
        int available = Math.max(1, MeasureSpec.getSize(widthSpec) - margins.leftMargin - margins.rightMargin);
        text.measure(MeasureSpec.makeMeasureSpec(available, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        int desired = Math.max(AndroidUtilities.dp(50), text.getMeasuredHeight() + AndroidUtilities.dp(24));
        setHeight((int) Math.ceil(desired / AndroidUtilities.density));
        super.onMeasure(widthSpec, heightSpec);
    }

    public static final class Factory extends UItem.UItemFactory<BananaSwitchCell> {
        static { setup(new Factory()); }

        @Override
        public BananaSwitchCell createView(Context context, RecyclerListView list, int account, int guid,
                                           Theme.ResourcesProvider resourcesProvider) {
            return new BananaSwitchCell(context, resourcesProvider);
        }

        @Override
        public void bindView(View view, UItem item, boolean divider, UniversalAdapter adapter, UniversalRecyclerView list) {
            BananaSwitchCell cell = (BananaSwitchCell) view;
            cell.updateRTL();
            cell.setTextAndCheck(item.text, item.checked, divider);
            cell.setEnabled(item.enabled, null);
            cell.setColors(Theme.key_windowBackgroundWhiteBlackText, Theme.key_switchTrack,
                    Theme.key_switchTrackChecked, Theme.key_windowBackgroundWhite, Theme.key_windowBackgroundWhite);
        }

        public static UItem of(int id, CharSequence title, boolean checked) {
            UItem item = UItem.ofFactory(Factory.class);
            item.id = id;
            item.text = title;
            return item.setChecked(checked);
        }
    }
}
