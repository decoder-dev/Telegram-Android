package org.telegram.ui.Components;

import android.app.Dialog;
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

/** Theme animation bindings shared by every Extras screen, including the palette chooser. */
public abstract class BananaSettingsFragment extends UniversalFragment {
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
        colors.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));
        colors.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));
        colors.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        colors.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SUBTITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultSubtitle));
        colors.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        colors.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));
        colors.add(new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault));
        colors.add(new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector));
        colors.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR,
                new Class[]{HeaderCell.class, TextCell.class, TextSettingsCell.class, TextCheckCell.class, TextDetailSettingsCell.class},
                null, null, this::updateTextRows, Theme.key_windowBackgroundWhite));
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
