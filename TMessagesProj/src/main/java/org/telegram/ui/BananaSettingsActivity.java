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
import org.telegram.ui.Cells.ThemesHorizontalListCell;
import org.telegram.ui.Components.BananaTheme;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.BananaSettingsFragment;

import java.util.ArrayList;

/**
 * BananaGram Extras hub — sections mirror ForkExtrasController on iOS.
 * Ghost preset and silent-sending stay on the hub for quick access.
 */
public final class BananaSettingsActivity extends BananaSettingsFragment {
    private static final int GHOST_PRESET = 1000;
    private static final int SILENT_SENDING = 1001;
    private static final int VALUES = 1003;
    private static final int AYU_SETTINGS = 1004;
    private static final int UPDATES = 1005;
    private static final int SESSION_BACKUP = 1006;
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
        items.add(UItem.asShadow(null));
        // Live mini-chat previews of the four bundled themes, like the iOS theme picker,
        // instead of a plain list of names.
        items.add(UItem.asHeader(LocaleController.getString(R.string.BananaAppearanceTheme)));
        items.add(UItem.asCustom(themesCell(), 148));
        items.add(UItem.asShadow(LocaleController.getString(R.string.BananaHubFooter)));

        String[] order = BananaExtrasCatalog.HUB_ORDER;
        for (int i = 0; i < order.length; i++) {
            String section = order[i];
            items.add(UItem.asButton(SECTION_BASE + i, hubIcon(section), hubTitle(section)));
        }
        items.add(UItem.asShadow(null));
        items.add(UItem.asButton(VALUES, LocaleController.getString(R.string.BananaValues)));
        items.add(UItem.asButton(SESSION_BACKUP, LocaleController.getString(R.string.BananaSessionBackup)));
        items.add(UItem.asButton(UPDATES, LocaleController.getString(R.string.BananaUpdates)));
        items.add(UItem.asButton(AYU_SETTINGS, LocaleController.getString(R.string.AyuPreferences)));
        items.add(UItem.asShadow(LocaleController.getString(R.string.BananaAuthorFooter)));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == SESSION_BACKUP) { presentFragment(new BananaSessionBackupActivity(currentAccount)); return; }
        if (item.id == AYU_SETTINGS) {
            com.radolyn.ayugram.ui.preferences.AyuGramPreferencesActivity preferences =
                    new com.radolyn.ayugram.ui.preferences.AyuGramPreferencesActivity();
            preferences.setCurrentAccount(currentAccount);
            presentFragment(preferences);
            return;
        }
        if (item.id == UPDATES) {
            new com.exteragram.messenger.preferences.updater.UpdaterBottomSheet(
                    getParentActivity(), this, false, null).show();
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
        }
    }

    private CharSequence[] silentLabels() {
        return new CharSequence[]{
                LocaleController.getString(R.string.BananaSilentNormal),
                LocaleController.getString(R.string.BananaSilentGhost),
                LocaleController.getString(R.string.BananaSilentAlways)
        };
    }

    private ThemesHorizontalListCell themesCell;

    public ThemesHorizontalListCell getThemesCell() {
        return themesCell;
    }

    private ThemesHorizontalListCell themesCell() {
        if (themesCell == null) {
            ArrayList<Theme.ThemeInfo> themes = new ArrayList<>();
            for (BananaTheme.Palette palette : BananaTheme.Palette.values()) {
                Theme.ThemeInfo info = Theme.getTheme(BananaTheme.themeName(palette));
                if (info != null) themes.add(info);
            }
            themesCell = new ThemesHorizontalListCell(getParentActivity(), this, ThemeActivity.THEME_TYPE_BASIC, themes, new ArrayList<>()) {
                @Override
                public void selectTheme(Theme.ThemeInfo themeInfo) {
                    BananaTheme.Palette palette = BananaTheme.paletteOf(themeInfo);
                    if (palette == null) return;
                    if (themeInfo.isDark()) {
                        // As BananaTheme.applyThemeInfo: keep the night slot on the chosen dark theme.
                        Theme.setCurrentNightTheme(themeInfo);
                    }
                    super.selectTheme(themeInfo);
                    BananaConfig.setThemePalette(currentAccount, palette.ordinal());
                }

                @Override
                protected void showOptionsForTheme(Theme.ThemeInfo themeInfo) {
                    // Bundled themes have nothing to share, edit or delete.
                }
            };
            themesCell.setDrawDivider(false);
            themesCell.setFocusable(false);
        }
        return themesCell;
    }

    /** Section icons, as next to each hub row on iOS. */
    private static int hubIcon(String section) {
        switch (section) {
            case "NINJA": return R.drawable.msg_saved;
            case "GHOST": return R.drawable.ayu_ghost;
            case "PRIVACY": return R.drawable.msg2_secret;
            case "APPEARANCE": return R.drawable.msg_theme;
            case "CHAT_LIST": return R.drawable.msg_folders;
            case "TAB_BAR": return R.drawable.msg_topics;
            case "MESSAGES": return R.drawable.msg_discussion;
            case "MEDIA": return R.drawable.msg_calls;
            case "PROFILE": return R.drawable.msg_openprofile;
            case "NETWORK": return R.drawable.msg2_data;
            default: return R.drawable.msg_mini_customize;
        }
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
            case "APPEARANCE":
                return LocaleController.getString(R.string.BananaHubAppearance);
            case "NETWORK":
                return LocaleController.getString(R.string.BananaHubNetwork);
            default:
                return section;
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
