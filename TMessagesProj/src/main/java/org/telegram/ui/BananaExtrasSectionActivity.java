package org.telegram.ui;

import android.view.View;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.banana.BananaConfig;
import org.telegram.messenger.banana.BananaConfig.Option;
import org.telegram.messenger.banana.BananaExtrasCatalog;
import org.telegram.messenger.banana.BananaVisualLabels;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.BananaSwitchCell;
import org.telegram.ui.Cells.BananaValueCell;
import org.telegram.ui.Components.BananaTheme;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/** One BananaGram Extras section (Ghost, Ninja, Privacy, …). */
public final class BananaExtrasSectionActivity extends UniversalFragment {
    private final String section;

    public BananaExtrasSectionActivity(int account, String section) {
        setCurrentAccount(account);
        this.section = section;
    }

    @Override
    public Theme.ResourcesProvider getResourceProvider() {
        return BananaTheme.forExtras();
    }

    @Override
    protected CharSequence getTitle() {
        int id = getStringId("BananaCat_" + section);
        return id != 0 ? LocaleController.getString(id) : section;
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        Option[] options = BananaExtrasCatalog.optionsFor(section);
        for (Option option : options) {
            int label = optionLabel(option);
            boolean supported = true;
            items.add(BananaSwitchCell.Factory.of(option.ordinal() + 1,
                    label != 0 ? LocaleController.getString(label) : option.name(),
                    supported && BananaConfig.get(currentAccount, option)).setEnabled(supported));
            if (!supported) items.add(UItem.asShadow(LocaleController.getString(R.string.BananaKeychainUnavailable)));
        }
        if ("PROFILE".equals(section)) {
            items.add(BananaValueCell.Factory.of(9100,
                    LocaleController.getString(R.string.BananaVisualLabel),
                    BananaVisualLabels.labelName(currentAccount)));
        } else if ("GHOST".equals(section)) {
            items.add(UItem.asShadow(LocaleController.getString(R.string.BananaGhostModeInfo)));
        } else if ("NINJA".equals(section)) {
            items.add(UItem.asButton(9001, LocaleController.getString(R.string.BananaHistoryOpen)));
            items.add(UItem.asButton(9002, LocaleController.getString(R.string.BananaFilters)));
            items.add(UItem.asShadow(LocaleController.getString(R.string.BananaHistoryInfo)));
        }
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == 9100) {
            CharSequence[] labels = new CharSequence[]{
                    LocaleController.getString(R.string.BananaVisualLabelNone),
                    LocaleController.getString(R.string.BananaVisualLabelScam),
                    LocaleController.getString(R.string.BananaVisualLabelFake),
                    LocaleController.getString(R.string.BananaVisualLabelSupport),
                    LocaleController.getString(R.string.BananaVisualLabelBot)
            };
            showDialog(new AlertDialog.Builder(getParentActivity(), getResourceProvider())
                    .setTitle(LocaleController.getString(R.string.BananaVisualLabel))
                    .setItems(labels, (dialog, which) -> {
                        BananaVisualLabels.set(currentAccount, which);
                        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.mainUserInfoChanged);
                        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_NAME);
                        if (listView != null && listView.adapter != null) {
                            listView.adapter.update(true);
                        }
                    }).create());
            return;
        }
        if (item.id == 9001) {
            presentFragment(new BananaHistoryActivity(currentAccount));
            return;
        }
        if (item.id == 9002) {
            presentFragment(new BananaFiltersActivity(currentAccount, 0));
            return;
        }
        if (item.id > 0 && item.id <= Option.values().length) {
            Option option = Option.values()[item.id - 1];

            boolean next = !BananaConfig.get(currentAccount, option);
            BananaConfig.set(currentAccount, option, next);
            if (option == Option.SESSION_KEYCHAIN_BACKUP) {
                if (next) org.telegram.messenger.banana.BananaSessionBackup.schedule(currentAccount);
                else org.telegram.messenger.banana.BananaSessionBackup.clear(currentAccount);
            }
            if (option == Option.GHOST_DONT_SEND_TYPING) {
                if (next) {
                    BananaConfig.set(currentAccount, Option.HIDE_TYPING, true);
                    BananaConfig.set(currentAccount, Option.HIDE_RECORDING, true);
                    BananaConfig.set(currentAccount, Option.HIDE_UPLOAD, true);
                }
                // Keep Ayu sendUploadProgress / typing suppression in sync
                com.radolyn.ayugram.AyuConfig.sendUploadProgress = !next;
                com.radolyn.ayugram.AyuConfig.editor.putBoolean("sendUploadProgress", !next).apply();
            }
            if (option == Option.HIDE_ADS) {
                com.radolyn.ayugram.AyuConfig.disableAds = next;
                com.radolyn.ayugram.AyuConfig.editor.putBoolean("disableAds", next).apply();
            }
            if (option == Option.LOCAL_PREMIUM) {
                com.radolyn.ayugram.AyuConfig.localPremium = next;
                com.radolyn.ayugram.AyuConfig.editor.putBoolean("localPremium", next).apply();
            }
            if (option == Option.DONT_READ_MESSAGES) {
                com.radolyn.ayugram.AyuConfig.sendReadPackets = !next;
                com.radolyn.ayugram.AyuConfig.editor.putBoolean("sendReadPackets", !next).apply();
            }
            if (option == Option.GHOST_READ_ON_INTERACT) {
                com.radolyn.ayugram.AyuConfig.markReadAfterSend = next;
                com.radolyn.ayugram.AyuConfig.editor.putBoolean("markReadAfterSend", next).apply();
            }
            if (option == Option.HIDE_ONLINE) {
                com.radolyn.ayugram.AyuConfig.sendOnlinePackets = !next;
                com.radolyn.ayugram.AyuConfig.editor.putBoolean("sendOnlinePackets", !next).apply();
            }
            if (option == Option.GHOST_SCHEDULE_MESSAGES) {
                // Send path schedules only when ghost mode is active AND this option is on.
                boolean schedule = next && BananaConfig.isGhostModeActive(currentAccount);
                com.radolyn.ayugram.AyuConfig.useScheduledMessages = schedule;
                com.radolyn.ayugram.AyuConfig.editor.putBoolean("useScheduledMessages", schedule).apply();
            }
            if (option == Option.REGEX_MESSAGE_FILTERS_ENABLED) {
                org.telegram.messenger.banana.FilterStore.get(currentAccount).setEnabled(next);
                com.radolyn.ayugram.AyuConfig.regexFiltersEnabled = next;
                com.radolyn.ayugram.AyuConfig.editor.putBoolean("regexFiltersEnabled", next).apply();
                com.radolyn.ayugram.AyuFilter.rebuildCache();
                org.telegram.messenger.banana.MessageFilters.settingsChanged(currentAccount);
            }
            if (option == Option.REGEX_MESSAGE_FILTERS_CASE_INSENSITIVE) {
                com.radolyn.ayugram.AyuConfig.regexFiltersCaseInsensitive = next;
                com.radolyn.ayugram.AyuConfig.editor.putBoolean("regexFiltersCaseInsensitive", next).apply();
                com.radolyn.ayugram.AyuFilter.rebuildCache();
                org.telegram.messenger.banana.MessageFilters.settingsChanged(currentAccount);
            }
            if (option == Option.HIDE_ONLINE || option == Option.OFFLINE_AFTER_ACTIVITY) {
                Utilities.stageQueue.postRunnable(() -> getMessagesController().updateTimerProc());
            }
            if (option == Option.GLASS_BUBBLES) {
                Theme.reloadAllResources(getParentActivity());
                if (parentLayout != null) parentLayout.rebuildAllFragmentViews(true, true);
                return;
            }
            listView.adapter.update(true);
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }

    private static int optionLabel(Option option) {
        return getStringId("BananaOpt_" + option.name());
    }

    private static int getStringId(String name) {
        try {
            return R.string.class.getField(name).getInt(null);
        } catch (Exception e) {
            return 0;
        }
    }
}
