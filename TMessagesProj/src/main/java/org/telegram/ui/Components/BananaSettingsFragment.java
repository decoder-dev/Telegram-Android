package org.telegram.ui.Components;

import android.app.Dialog;
import android.content.Context;
import android.view.View;

import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.LaunchActivity;

import java.util.ArrayList;

/**
 * Base of every Extras screen. Uses Telegram's simple glass layout: a glass header with a centred
 * title and grouped rounded sections, the closest Telegram has to iOS inset grouped lists.
 */
public abstract class BananaSettingsFragment extends UniversalFragment {
    @Override
    protected boolean useSimpleActionBar() {
        return true;
    }

    @Override
    public View createView(Context context) {
        setHasOwnBackground(true);
        View view = super.createView(context);
        listView.setSections();
        listView.setSectionsDrawBackground(true);
        return view;
    }

    @Override
    protected RecyclerListView getListViewForSimpleGlass() {
        return listView;
    }

    @Override
    public boolean isSupportEdgeToEdge() {
        return true;
    }

    private void updateTextRow(View row) {
        if (row instanceof TextCell) ((TextCell) row).updateColors();
    }

    private void updateTextRows() {
        if (listView == null) return;
        // RecyclerView may reattach a bound cached row without calling its adapter.
        // Update those rows too, keeping each TextCell's semantic text/icon tags.
        for (int i = 0; i < listView.getChildCount(); i++) updateTextRow(listView.getChildAt(i));
        for (int i = 0; i < listView.getCachedChildCount(); i++) updateTextRow(listView.getCachedChildAt(i));
        for (int i = 0; i < listView.getHiddenChildCount(); i++) updateTextRow(listView.getHiddenChildAt(i));
        for (int i = 0; i < listView.getAttachedScrapChildCount(); i++) updateTextRow(listView.getAttachedScrapChildAt(i));
        listView.invalidate();
    }

    @Override
    protected void onDialogDismiss(Dialog dialog) {
        super.onDialogDismiss(dialog);
        View view = fragmentView;
        if (view != null) view.post(() -> {
            if (fragmentView == view && getParentLayout() != null && getParentLayout().getLastFragment() == this
                    && getParentActivity() instanceof LaunchActivity) {
                ((LaunchActivity) getParentActivity()).checkSystemBarColors(true, true, true);
            }
        });
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> colors = new ArrayList<>();
        colors.add(new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault));
        colors.add(new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector));
        // The sections paint the rounded cards; giving rows a background here would square their corners.
        colors.add(new ThemeDescription(null, 0, null, null, null, this::updateTextRows, Theme.key_windowBackgroundWhite));
        colors.add(new ThemeDescription(listView, ThemeDescription.FLAG_TEXTCOLOR,
                new Class[]{TextSettingsCell.class, TextCheckCell.class, TextDetailSettingsCell.class},
                new String[]{"textView", "textView", "textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        colors.add(new ThemeDescription(listView, ThemeDescription.FLAG_TEXTCOLOR,
                new Class[]{TextSettingsCell.class, TextCheckCell.class, TextDetailSettingsCell.class},
                new String[]{"valueTextView", "valueTextView", "valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
        colors.add(new ThemeDescription(listView, ThemeDescription.FLAG_TEXTCOLOR,
                new Class[]{HeaderCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueHeader));
        colors.add(new ThemeDescription(listView, ThemeDescription.FLAG_TEXTCOLOR,
                new Class[]{TextInfoPrivacyCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText4));
        colors.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrack));
        colors.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackChecked));
        colors.add(new ThemeDescription(listView, 0, null, null, null, null, Theme.key_divider));
        for (ThemeDescription color : colors) color.resourcesProvider = getResourceProvider();
        return colors;
    }
}
