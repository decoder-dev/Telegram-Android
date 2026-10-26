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

/** Native two-line setting; the value never competes with the title for width. */
public final class BananaValueCell extends TextDetailSettingsCell {
    private final Theme.ResourcesProvider resourcesProvider;

    public BananaValueCell(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;
        setMultilineDetail(true);
        for (TextView text : new TextView[]{getTextView(), getValueTextView()}) {
            text.setSingleLine(false);
            text.setMaxLines(Integer.MAX_VALUE);
            text.setEllipsize(null);
            text.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
        }
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        TextView title = getTextView();
        ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) title.getLayoutParams();
        int available = Math.max(1, MeasureSpec.getSize(widthSpec) - margins.leftMargin - margins.rightMargin);
        title.measure(MeasureSpec.makeMeasureSpec(available, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        ((ViewGroup.MarginLayoutParams) getValueTextView().getLayoutParams()).topMargin =
                margins.topMargin + title.getMeasuredHeight() + AndroidUtilities.dp(4);
        super.onMeasure(widthSpec, heightSpec);
    }

    public static final class Factory extends UItem.UItemFactory<BananaValueCell> {
        static { setup(new Factory()); }

        @Override
        public BananaValueCell createView(Context context, RecyclerListView list, int account, int guid,
                                          Theme.ResourcesProvider resourcesProvider) {
            return new BananaValueCell(context, resourcesProvider);
        }

        @Override
        public void bindView(View view, UItem item, boolean divider, UniversalAdapter adapter, UniversalRecyclerView list) {
            BananaValueCell cell = (BananaValueCell) view;
            cell.setTextAndValue(item.text, item.textValue, divider);
            cell.getTextView().setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, cell.resourcesProvider));
            cell.getValueTextView().setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, cell.resourcesProvider));
            cell.setEnabled(item.enabled);
        }

        public static UItem of(int id, CharSequence title, CharSequence value) {
            UItem item = UItem.ofFactory(Factory.class);
            item.id = id;
            item.text = title;
            item.textValue = value;
            return item;
        }
    }
}
