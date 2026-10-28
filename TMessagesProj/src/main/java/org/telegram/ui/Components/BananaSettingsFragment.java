package org.telegram.ui.Components;

import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;

import java.util.ArrayList;

/** Theme animation bindings shared by every Extras screen, including the palette chooser. */
public abstract class BananaSettingsFragment extends UniversalFragment {
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
        colors.add(new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUND,
                new Class[]{HeaderCell.class, TextSettingsCell.class, TextCheckCell.class, TextDetailSettingsCell.class},
                null, null, null, Theme.key_windowBackgroundWhite));
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
        colors.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, null, null, null, Theme.key_switchTrack));
        colors.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, null, null, null, Theme.key_switchTrackChecked));
        colors.add(new ThemeDescription(listView, 0, null, null, null, null, Theme.key_divider));
        for (ThemeDescription color : colors) color.resourcesProvider = getResourceProvider();
        return colors;
    }
}
