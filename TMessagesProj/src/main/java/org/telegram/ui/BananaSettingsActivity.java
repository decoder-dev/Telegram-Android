package org.telegram.ui;

import android.view.View;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.banana.BananaConfig;
import org.telegram.messenger.banana.BananaExtrasCatalog;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.BananaSwitchCell;
import org.telegram.ui.Cells.BananaValueCell;
import org.telegram.ui.Components.BananaTheme;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/**
 * BananaGram Extras hub — sections mirror ForkExtrasController on iOS.
 * Ghost preset and silent-sending stay on the hub for quick access.
 */
public final class BananaSettingsActivity extends UniversalFragment {
    private static final int GHOST_PRESET = 1000;
    private static final int SILENT_SENDING = 1001;
    private static final int THEME_PALETTE = 1002;
    private static final int VALUES = 1003;
    private static final int AYU_SETTINGS = 1004;
    private static final int SECTION_BASE = 2000;

    public BananaSettingsActivity(int account) {
        setCurrentAccount(account);
    }

    @Override
    public Theme.ResourcesProvider getResourceProvider() {
        return BananaTheme.forExtras();
    }

    @Override
    protected CharSequence getTitle() {
        return LocaleController.getString(R.string.BananaExtras);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(LocaleController.getString(R.string.BananaGhostMode)));
        items.add(BananaSwitchCell.Factory.of(GHOST_PRESET, LocaleController.getString(R.string.BananaGhostPreset),
                BananaConfig.isGhostModeActive(currentAccount)));
        items.add(BananaValueCell.Factory.of(SILENT_SENDING, LocaleController.getString(R.string.BananaSilentSending),
                silentLabels()[BananaConfig.silentSending(currentAccount).ordinal()]));
        items.add(BananaValueCell.Factory.of(THEME_PALETTE, LocaleController.getString(R.string.BananaAppearanceTheme),
                themeLabels()[BananaTheme.selectedPalette().ordinal()]));
        items.add(UItem.asShadow(LocaleController.getString(R.string.BananaHubFooter)));

        String[] order = BananaExtrasCatalog.HUB_ORDER;
        for (int i = 0; i < order.length; i++) {
            String section = order[i];
            items.add(UItem.asButton(SECTION_BASE + i, hubTitle(section)));
        }
        items.add(UItem.asShadow(null));
        items.add(UItem.asButton(VALUES, LocaleController.getString(R.string.BananaValues)));
        items.add(UItem.asButton(AYU_SETTINGS, LocaleController.getString(R.string.AyuPreferences)));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == AYU_SETTINGS) {
            com.radolyn.ayugram.ui.preferences.AyuGramPreferencesActivity preferences =
                    new com.radolyn.ayugram.ui.preferences.AyuGramPreferencesActivity();
            preferences.setCurrentAccount(currentAccount);
            presentFragment(preferences);
            return;
        }
        if (item.id == VALUES) {
            presentFragment(new BananaExtrasValuesActivity(currentAccount));
            return;
        }
        if (item.id >= SECTION_BASE && item.id < SECTION_BASE + BananaExtrasCatalog.HUB_ORDER.length) {
            presentFragment(new BananaExtrasSectionActivity(currentAccount,
                    BananaExtrasCatalog.HUB_ORDER[item.id - SECTION_BASE]));
            return;
        }
        if (item.id == GHOST_PRESET) {
            BananaConfig.setGhostMode(currentAccount, !BananaConfig.isGhostModeActive(currentAccount));
            Utilities.stageQueue.postRunnable(() -> getMessagesController().updateTimerProc());
            listView.adapter.update(true);
            return;
        }
        if (item.id == SILENT_SENDING) {
            showDialog(new AlertDialog.Builder(getParentActivity(), getResourceProvider())
                    .setTitle(LocaleController.getString(R.string.BananaSilentSending))
                    .setItems(silentLabels(), (dialog, which) -> {
                        BananaConfig.setSilentSending(currentAccount, BananaConfig.SilentSending.values()[which]);
                        listView.adapter.update(true);
                    }).create());
            return;
        }
        if (item.id == THEME_PALETTE) {
            showDialog(new AlertDialog.Builder(getParentActivity(), getResourceProvider())
                    .setTitle(LocaleController.getString(R.string.BananaAppearanceTheme))
                    .setItems(themeLabels(), (dialog, which) -> {
                        BananaTheme.setPalette(currentAccount, BananaTheme.Palette.values()[which]);
                        listView.adapter.update(true);
                    }).create());
        }
    }

    private CharSequence[] silentLabels() {
        return new CharSequence[]{
                LocaleController.getString(R.string.BananaSilentNormal),
                LocaleController.getString(R.string.BananaSilentGhost),
                LocaleController.getString(R.string.BananaSilentAlways)
        };
    }

    private CharSequence[] themeLabels() {
        return new CharSequence[]{
                LocaleController.getString(R.string.BananaThemeSystem),
                LocaleController.getString(R.string.BananaThemeCream),
                LocaleController.getString(R.string.BananaThemeGraphite)
        };
    }

    private static String hubTitle(String section) {
        switch (section) {
            case "NINJA":
                return LocaleController.getString(R.string.BananaHubNinja);
            case "GHOST":
                return LocaleController.getString(R.string.BananaHubGhost);
            case "PRIVACY":
                return LocaleController.getString(R.string.BananaHubPrivacy);
            case "CHAT_LIST":
                return LocaleController.getString(R.string.BananaHubChatList);
            case "TAB_BAR":
                return LocaleController.getString(R.string.BananaHubTabBar);
            case "MESSAGES":
                return LocaleController.getString(R.string.BananaHubMessages);
            case "MEDIA":
                return LocaleController.getString(R.string.BananaHubMedia);
            case "PROFILE":
                return LocaleController.getString(R.string.BananaHubProfile);
            default:
                return section;
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
