package org.telegram.ui;

import android.view.View;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.banana.BananaConfig;
import org.telegram.messenger.banana.BananaConfig.Option;
import org.telegram.messenger.banana.BananaExtrasCatalog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.BananaSwitchCell;
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
        return BananaTheme.COLORS;
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
            items.add(BananaSwitchCell.Factory.of(option.ordinal() + 1,
                    label != 0 ? LocaleController.getString(label) : option.name(),
                    BananaConfig.get(currentAccount, option)));
        }
        if ("GHOST".equals(section)) {
            items.add(UItem.asShadow(LocaleController.getString(R.string.BananaGhostModeInfo)));
        } else if ("NINJA".equals(section)) {
            items.add(UItem.asButton(9001, LocaleController.getString(R.string.BananaHistoryOpen)));
            items.add(UItem.asButton(9002, LocaleController.getString(R.string.BananaFilters)));
            items.add(UItem.asShadow(LocaleController.getString(R.string.BananaHistoryInfo)));
        }
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
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
                com.radolyn.ayugram.AyuConfig.useScheduledMessages = next;
                com.radolyn.ayugram.AyuConfig.editor.putBoolean("useScheduledMessages", next).apply();
            }
            if (option == Option.HIDE_ONLINE || option == Option.OFFLINE_AFTER_ACTIVITY) {
                Utilities.stageQueue.postRunnable(() -> getMessagesController().updateTimerProc());
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
