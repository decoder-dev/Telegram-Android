package org.telegram.ui;

import android.view.View;

import org.telegram.messenger.AndroidUtilities;
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
import org.telegram.ui.Components.BananaSettingsFragment;

import java.util.ArrayList;

/** One BananaGram Extras section (Ghost, Ninja, Privacy, …). */
public final class BananaExtrasSectionActivity extends BananaSettingsFragment {
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
        BananaExtrasCatalog.Group[] groups = BananaExtrasCatalog.groupsFor(section);
        for (int g = 0; g < groups.length; g++) {
            BananaExtrasCatalog.Group group = groups[g];
            if (g > 0) items.add(UItem.asShadow(null));
            if (group.title != 0) items.add(UItem.asHeader(LocaleController.getString(group.title)));
            for (Option option : group.options) {
                int label = optionLabel(option);
                items.add(BananaSwitchCell.Factory.of(option.ordinal() + 1,
                        label != 0 ? LocaleController.getString(label) : option.name(),
                        BananaConfig.get(currentAccount, option)));
            }
        }
        if ("PROFILE".equals(section)) {
            items.add(UItem.asShadow(null));
            items.add(BananaValueCell.Factory.of(9100,
                    LocaleController.getString(R.string.BananaVisualLabel),
                    BananaVisualLabels.labelName(currentAccount)));
        } else if ("MESSAGES".equals(section) && BananaConfig.get(currentAccount, Option.MESSAGE_BOOKMARKS)) {
            items.add(UItem.asShadow(null));
            items.add(UItem.asButton(9200, LocaleController.getString(R.string.BananaBookmarks)));
        } else if ("NINJA".equals(section)) {
            items.add(UItem.asShadow(LocaleController.getString(R.string.BananaGhostModeInfo)));
            items.add(UItem.asButton(9001, LocaleController.getString(R.string.BananaHistoryOpen)));
            items.add(UItem.asButton(9002, LocaleController.getString(R.string.BananaFilters)));
            items.add(UItem.asShadow(LocaleController.getString(R.string.BananaHistoryInfo)));
        }
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == 9200) { showBookmarks(); return; }
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
            if (option == Option.REMOVE_PINGS) {
                getMessagesController().sortDialogs(null);
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.dialogsNeedReload);
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces,
                        MessagesController.UPDATE_MASK_READ_DIALOG_MESSAGE);
            }
            if (option == Option.SESSION_KEYCHAIN_BACKUP) {
                if (next) org.telegram.messenger.banana.BananaSessionBackup.schedule(currentAccount);
                else org.telegram.messenger.banana.BananaSessionBackup.clear(currentAccount);
            }
            // Ghost switches stay on this account. Mirroring them into AyuGram's process-wide
            // flags used to switch ghost behavior on or off for every other account too.
            if (option == Option.GHOST_DONT_SEND_TYPING) {
                // The master switch drives its three kinds both ways, so turning it off really
                // lets typing, recording and upload status through again.
                BananaConfig.set(currentAccount, Option.HIDE_TYPING, next);
                BananaConfig.set(currentAccount, Option.HIDE_RECORDING, next);
                BananaConfig.set(currentAccount, Option.HIDE_UPLOAD, next);
            }
            if (option == Option.REGEX_MESSAGE_FILTERS_ENABLED) {
                org.telegram.messenger.banana.FilterStore.get(currentAccount).setEnabled(next);
                org.telegram.messenger.banana.MessageFilters.settingsChanged(currentAccount);
            }
            if (option == Option.REGEX_MESSAGE_FILTERS_CASE_INSENSITIVE) {
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

    private void showBookmarks() {
        ArrayList<String> entries = org.telegram.messenger.banana.BananaHooks.getBookmarks(currentAccount);
        AlertDialog.Builder list = new AlertDialog.Builder(getParentActivity(), getResourceProvider())
                .setTitle(LocaleController.getString(R.string.BananaBookmarks))
                .setNegativeButton(LocaleController.getString(R.string.Close), null);
        if (entries.isEmpty()) {
            list.setMessage(LocaleController.getString(R.string.BananaBookmarksEmpty));
        } else {
            CharSequence[] labels = new CharSequence[entries.size()];
            for (int i = 0; i < entries.size(); i++) {
                android.os.Bundle args = org.telegram.messenger.banana.BananaHooks.bookmarkArguments(entries.get(i));
                long userId = args.getLong("user_id"), chatId = args.getLong("chat_id");
                String name = null;
                if (userId != 0) {
                    org.telegram.tgnet.TLRPC.User user = getMessagesController().getUser(userId);
                    if (user != null) name = org.telegram.messenger.UserObject.getUserName(user);
                } else if (chatId != 0) {
                    org.telegram.tgnet.TLRPC.Chat chat = getMessagesController().getChat(chatId);
                    if (chat != null) name = chat.title;
                }
                labels[i] = android.text.TextUtils.isEmpty(name) ? entries.get(i) :
                        LocaleController.formatString(R.string.BananaHistoryPeerNamed, name, args.getInt("message_id"));
            }
            list.setItems(labels, (dialog, which) -> {
                String entry = entries.get(which);
                showDialog(new AlertDialog.Builder(getParentActivity(), getResourceProvider())
                        .setTitle(labels[which])
                        .setItems(new CharSequence[]{LocaleController.getString(R.string.BananaBookmarkOpen),
                                LocaleController.getString(R.string.BananaBookmarkRemove)}, (actions, action) -> {
                            if (action == 0) {
                                android.os.Bundle args = org.telegram.messenger.banana.BananaHooks.bookmarkArguments(entry);
                                if (args != null && getMessagesController().checkCanOpenChat(args, this)) {
                                    ChatActivity chat = new ChatActivity(args);
                                    chat.setCurrentAccount(currentAccount);
                                    presentFragment(chat);
                                }
                            } else {
                                org.telegram.messenger.banana.BananaHooks.removeBookmark(currentAccount, entry);
                                AndroidUtilities.runOnUIThread(this::showBookmarks);
                            }
                        }).setNegativeButton(LocaleController.getString(R.string.Cancel), null).create());
            });
        }
        showDialog(list.create());
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
