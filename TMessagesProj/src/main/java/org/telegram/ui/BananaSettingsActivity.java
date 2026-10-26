package org.telegram.ui;

import android.view.View;
import android.graphics.Color;

import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.banana.BananaConfig;
import org.telegram.messenger.banana.BananaConfig.Option;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.BananaSwitchCell;

import java.util.ArrayList;

public final class BananaSettingsActivity extends UniversalFragment {
    private final Theme.ResourcesProvider colors = key -> {
        int color = Theme.getColor(key);
        if (key != Theme.key_windowBackgroundWhiteGrayText4) return color;
        int background = ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_windowBackgroundGray), 255);
        int foreground = ColorUtils.setAlphaComponent(color, 255);
        int text = ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), 255);
        for (int step = 0; step <= 20; step++) {
            int readable = ColorUtils.blendARGB(foreground, text, step / 20f);
            if (ColorUtils.calculateContrast(readable, background) >= 4.5) return readable;
        }
        return ColorUtils.calculateContrast(Color.BLACK, background) > ColorUtils.calculateContrast(Color.WHITE, background) ?
                Color.BLACK : Color.WHITE;
    };

    public BananaSettingsActivity(int account) {
        setCurrentAccount(account);
    }

    @Override
    public Theme.ResourcesProvider getResourceProvider() {
        return colors;
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
        items.add(UItem.asHeader(LocaleController.getString(R.string.BananaGhostMode)));
        addOption(items, Option.HIDE_ONLINE, R.string.BananaHideOnline);
        addOption(items, Option.HIDE_TYPING, R.string.BananaHideTyping);
        addOption(items, Option.HIDE_RECORDING, R.string.BananaHideRecording);
        addOption(items, Option.HIDE_UPLOAD, R.string.BananaHideUpload);
        addOption(items, Option.DONT_READ_MESSAGES, R.string.BananaDontReadMessages);
        addOption(items, Option.DONT_READ_STORIES, R.string.BananaDontReadStories);
        items.add(UItem.asShadow(LocaleController.getString(R.string.BananaGhostModeInfo)));
    }

    private void addOption(ArrayList<UItem> items, Option option, int label) {
        items.add(BananaSwitchCell.Factory.of(option.ordinal() + 1, LocaleController.getString(label),
                BananaConfig.get(currentAccount, option)));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id > 0 && item.id <= Option.values().length) {
            Option option = Option.values()[item.id - 1];
            BananaConfig.set(currentAccount, option, !BananaConfig.get(currentAccount, option));
            if (option == Option.HIDE_ONLINE) {
                Utilities.stageQueue.postRunnable(() -> getMessagesController().updateTimerProc());
            }
            listView.adapter.update(true);
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
