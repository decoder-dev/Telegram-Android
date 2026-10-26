package org.telegram.ui;

import android.view.View;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.banana.BananaConfig;
import org.telegram.messenger.banana.BananaConfig.Option;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;
import org.telegram.ui.Components.BananaTheme;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Cells.BananaSwitchCell;
import org.telegram.ui.Cells.BananaValueCell;

import java.util.ArrayList;

public final class BananaSettingsActivity extends UniversalFragment {
    private static final int GHOST_PRESET = 1000;
    private static final int SILENT_SENDING = 1001;
    private static final int MESSAGE_HISTORY = 1002;

    public BananaSettingsActivity(int account) {
        setCurrentAccount(account);
    }

    @Override
    public Theme.ResourcesProvider getResourceProvider() {
        return BananaTheme.COLORS;
    }

    @Override
    protected CharSequence getTitle() {
        return LocaleController.getString(R.string.BananaExtras);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(LocaleController.getString(R.string.BananaSending)));
        addOption(items, Option.CLEAN_TRACKING_LINKS, R.string.BananaCleanTrackingLinks);
        items.add(UItem.asShadow(LocaleController.getString(R.string.BananaCleanTrackingLinksInfo)));
        items.add(BananaValueCell.Factory.of(SILENT_SENDING, LocaleController.getString(R.string.BananaSilentSending),
                silentLabels()[BananaConfig.silentSending(currentAccount).ordinal()]));
        items.add(UItem.asShadow(LocaleController.getString(R.string.BananaSilentSendingInfo)));
        items.add(UItem.asHeader(LocaleController.getString(R.string.BananaGhostMode)));
        items.add(BananaSwitchCell.Factory.of(GHOST_PRESET, LocaleController.getString(R.string.BananaGhostPreset),
                BananaConfig.isGhostModeActive(currentAccount)));
        addOption(items, Option.HIDE_ONLINE, R.string.BananaHideOnline);
        addOption(items, Option.HIDE_TYPING, R.string.BananaHideTyping);
        addOption(items, Option.HIDE_RECORDING, R.string.BananaHideRecording);
        addOption(items, Option.HIDE_UPLOAD, R.string.BananaHideUpload);
        addOption(items, Option.DONT_READ_MESSAGES, R.string.BananaDontReadMessages);
        addOption(items, Option.DONT_READ_STORIES, R.string.BananaDontReadStories);
        addOption(items, Option.OFFLINE_AFTER_ACTIVITY, R.string.BananaOfflineAfterActivity);
        items.add(UItem.asShadow(LocaleController.getString(R.string.BananaGhostModeInfo)));
        items.add(UItem.asHeader(LocaleController.getString(R.string.BananaHistory)));
        addOption(items, Option.SAVE_DELETED, R.string.BananaSaveDeleted);
        addOption(items, Option.SAVE_EDITS, R.string.BananaSaveEdits);
        addOption(items, Option.SAVE_BOT_HISTORY, R.string.BananaSaveBotHistory);
        items.add(UItem.asButton(MESSAGE_HISTORY, LocaleController.getString(R.string.BananaHistoryOpen)));
        items.add(UItem.asShadow(LocaleController.getString(R.string.BananaHistoryInfo)));
    }

    private void addOption(ArrayList<UItem> items, Option option, int label) {
        items.add(BananaSwitchCell.Factory.of(option.ordinal() + 1, LocaleController.getString(label),
                BananaConfig.get(currentAccount, option)));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == MESSAGE_HISTORY) {
            presentFragment(new BananaHistoryActivity(currentAccount));
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
        if (item.id > 0 && item.id <= Option.values().length) {
            Option option = Option.values()[item.id - 1];
            BananaConfig.set(currentAccount, option, !BananaConfig.get(currentAccount, option));
            if (option == Option.HIDE_ONLINE) {
                Utilities.stageQueue.postRunnable(() -> getMessagesController().updateTimerProc());
            }
            listView.adapter.update(true);
        }
    }

    private CharSequence[] silentLabels() {
        return new CharSequence[]{LocaleController.getString(R.string.BananaSilentNormal),
                LocaleController.getString(R.string.BananaSilentGhost), LocaleController.getString(R.string.BananaSilentAlways)};
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
