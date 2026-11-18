package org.telegram.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.banana.BananaConfig;
import org.telegram.messenger.banana.BananaConfig.Option;
import org.telegram.messenger.banana.BananaExtrasCatalog;
import org.telegram.messenger.banana.BananaHooks;
import org.telegram.messenger.banana.BananaVisualLabels;
import org.telegram.tgnet.TLRPC;
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
            int badge = BananaConfig.getInt(currentAccount, BananaConfig.IntValue.LOCAL_VERIFICATION_BADGE);
            items.add(BananaValueCell.Factory.of(9101, LocaleController.getString(R.string.BananaLocalVerification),
                    badge == 0 ? LocaleController.getString(R.string.BananaLocalVerificationOff)
                            : org.telegram.messenger.banana.LocalVerificationBadges.TITLES[badge]));
        } else if ("MESSAGES".equals(section)) {
            items.add(UItem.asShadow(null));
            items.add(UItem.asButton(9201, LocaleController.getString(R.string.BananaMessageMenu)));
            if (BananaConfig.get(currentAccount, Option.MESSAGE_BOOKMARKS)) {
                items.add(UItem.asButton(9200, LocaleController.getString(R.string.BananaBookmarks)));
            }
        } else if ("PRIVACY".equals(section)) {
            items.add(UItem.asShadow(LocaleController.getString(R.string.BananaSecurityProtectionsInfo)));
            items.add(UItem.asButton(9300, LocaleController.getString(R.string.BananaDuressTitle)));
            items.add(UItem.asShadow(LocaleController.getString(R.string.BananaSecurityLockInfo)));
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
        if (item.id == 9300) { BananaDuressSettings.show(this); return; }
        if (item.id == 9201) { showMessageMenuEditor(); return; }
        if (item.id == 9101) {
            CharSequence[] badges = {LocaleController.getString(R.string.BananaLocalVerificationOff), "Major", "Hold", "Developer"};
            showDialog(new AlertDialog.Builder(getParentActivity(), getResourceProvider())
                    .setTitle(LocaleController.getString(R.string.BananaLocalVerificationPicker))
                    .setItems(badges, (dialog, which) -> {
                        BananaConfig.setInt(currentAccount, BananaConfig.IntValue.LOCAL_VERIFICATION_BADGE, which);
                        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.mainUserInfoChanged);
                        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_NAME);
                        if (listView != null && listView.adapter != null) listView.adapter.update(true);
                    }).create());
            return;
        }
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
            if (!next && (option == Option.HIDE_TYPING || option == Option.HIDE_RECORDING || option == Option.HIDE_UPLOAD)) {
                // The master suppresses every kind on its own: left on, it kept hiding the kind
                // whose switch now shows off.
                BananaConfig.set(currentAccount, Option.GHOST_DONT_SEND_TYPING, false);
            }
            // "Go offline right away" corrects the status that "Don't send online status" hides;
            // on its own it never sends anything, so the two move together.
            if (option == Option.OFFLINE_AFTER_ACTIVITY && next) {
                BananaConfig.set(currentAccount, Option.HIDE_ONLINE, true);
            }
            if (option == Option.HIDE_ONLINE && !next) {
                BananaConfig.set(currentAccount, Option.OFFLINE_AFTER_ACTIVITY, false);
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

    private static final String[] MENU_LABELS = {
            "BananaMenu_reply", "BananaMenu_editHistory", "BananaMenu_copy", "BananaMenu_translate", "BananaMenu_saveMedia",
            "BananaMenu_saveToFiles", "BananaMenu_convertAudio", "BananaMenu_viewReplies", "BananaMenu_edit", "BananaMenu_pin",
            "BananaMenu_copyLink", "BananaMenu_saveGif", "BananaMenu_forward", "BananaMenu_saveToSavedMessages", "BananaMenu_screenshot",
            "BananaMenu_bookmarks", "BananaMenu_selectFromAuthor", "BananaMenu_repeatMessage", "BananaMenu_messageJson",
            "BananaMenu_report", "BananaMenu_statistics", "BananaMenu_delete"};

    private String menuLabel(String key) {
        int index = org.telegram.messenger.banana.BananaMessageMenu.KEYS.indexOf(key);
        int id = index >= 0 ? getStringId(MENU_LABELS[index]) : 0;
        return id != 0 ? LocaleController.getString(id) : key;
    }

    /** Message menu: tap an item to show, hide or move it; Reset brings back Telegram's order. */
    private void showMessageMenuEditor() {
        if (getParentActivity() == null) return;
        java.util.List<String> order = org.telegram.messenger.banana.BananaMessageMenu.order(
                org.telegram.messenger.banana.BananaMessageMenu.parse(BananaConfig.getString(currentAccount, BananaConfig.StringValue.MESSAGE_MENU_ORDER)));
        java.util.List<String> hidden = org.telegram.messenger.banana.BananaMessageMenu.parse(
                BananaConfig.getString(currentAccount, BananaConfig.StringValue.MESSAGE_MENU_HIDDEN));
        CharSequence[] labels = new CharSequence[order.size()];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = (hidden.contains(order.get(i)) ? "☐ " : "☑ ") + menuLabel(order.get(i));
        }
        showDialog(new AlertDialog.Builder(getParentActivity(), getResourceProvider())
                .setTitle(LocaleController.getString(R.string.BananaMessageMenu))
                .setMessage(LocaleController.getString(R.string.BananaMessageMenuInfo))
                .setItems(labels, (dialog, which) -> editMenuItem(order, hidden, which))
                .setNeutralButton(LocaleController.getString(R.string.BananaMessageMenuReset), (dialog, which) -> {
                    BananaConfig.setString(currentAccount, BananaConfig.StringValue.MESSAGE_MENU_ORDER, "");
                    BananaConfig.setString(currentAccount, BananaConfig.StringValue.MESSAGE_MENU_HIDDEN, "");
                })
                .setPositiveButton(LocaleController.getString(R.string.Done), null)
                .create());
    }

    private void editMenuItem(java.util.List<String> order, java.util.List<String> hidden, int index) {
        String key = order.get(index);
        boolean isHidden = hidden.contains(key);
        CharSequence[] actions = {
                LocaleController.getString(isHidden ? R.string.BananaMessageMenuShow : R.string.BananaMessageMenuHide),
                LocaleController.getString(R.string.BananaMessageMenuUp),
                LocaleController.getString(R.string.BananaMessageMenuDown)};
        showDialog(new AlertDialog.Builder(getParentActivity(), getResourceProvider())
                .setTitle(menuLabel(key))
                .setItems(actions, (dialog, which) -> {
                    java.util.ArrayList<String> nextOrder = new java.util.ArrayList<>(order);
                    java.util.ArrayList<String> nextHidden = new java.util.ArrayList<>(hidden);
                    if (which == 0) {
                        if (isHidden) nextHidden.remove(key); else nextHidden.add(key);
                    } else {
                        int target = which == 1 ? index - 1 : index + 1;
                        if (target >= 0 && target < nextOrder.size()) java.util.Collections.swap(nextOrder, index, target);
                    }
                    BananaConfig.setString(currentAccount, BananaConfig.StringValue.MESSAGE_MENU_ORDER,
                            org.telegram.messenger.banana.BananaMessageMenu.format(nextOrder));
                    BananaConfig.setString(currentAccount, BananaConfig.StringValue.MESSAGE_MENU_HIDDEN,
                            org.telegram.messenger.banana.BananaMessageMenu.format(nextHidden));
                    showMessageMenuEditor();
                }).create());
    }

    private void showBookmarks() {
        ArrayList<String> entries = BananaHooks.getBookmarks(currentAccount);
        AlertDialog.Builder list = new AlertDialog.Builder(getParentActivity(), getResourceProvider())
                .setTitle(LocaleController.getString(R.string.BananaBookmarks))
                .setNegativeButton(LocaleController.getString(R.string.Close), null);
        if (entries.isEmpty()) {
            list.setMessage(LocaleController.getString(R.string.BananaBookmarksEmpty));
        } else {
            CharSequence[] labels = new CharSequence[entries.size()];
            for (int i = 0; i < entries.size(); i++) {
                labels[i] = bookmarkLabel(BananaHooks.bookmarkArguments(entries.get(i)));
            }
            list.setItems(labels, (dialog, which) -> {
                String entry = entries.get(which);
                showDialog(new AlertDialog.Builder(getParentActivity(), getResourceProvider())
                        .setTitle(labels[which])
                        .setItems(new CharSequence[]{LocaleController.getString(R.string.BananaBookmarkOpen),
                                LocaleController.getString(R.string.BananaBookmarkRemove)}, (actions, action) -> {
                            if (action == 0) {
                                Bundle args = BananaHooks.bookmarkArguments(entry);
                                if (args != null && getMessagesController().checkCanOpenChat(args, this)) {
                                    ChatActivity chat = new ChatActivity(args);
                                    chat.setCurrentAccount(currentAccount);
                                    presentFragment(chat);
                                }
                            } else {
                                BananaHooks.removeBookmark(currentAccount, entry);
                                AndroidUtilities.runOnUIThread(this::showBookmarks);
                            }
                        }).setNegativeButton(LocaleController.getString(R.string.Cancel), null).create());
            });
        }
        showDialog(list.create());
    }

    private String bookmarkLabel(Bundle args) {
        int messageId = args.getInt("message_id");
        long userId = args.getLong("user_id");
        long chatId = args.getLong("chat_id");
        int encryptedId = args.getInt("enc_id");
        String name = null;
        if (encryptedId != 0) {
            TLRPC.EncryptedChat encryptedChat = getMessagesController().getEncryptedChat(encryptedId);
            if (encryptedChat != null) {
                userId = encryptedChat.user_id;
            }
        }
        if (userId != 0) {
            TLRPC.User user = getMessagesController().getUser(userId);
            if (user != null) name = UserObject.getUserName(user);
        } else if (chatId != 0) {
            TLRPC.Chat chat = getMessagesController().getChat(chatId);
            if (chat != null) name = chat.title;
        }
        if (TextUtils.isEmpty(name)) {
            return LocaleController.formatString(R.string.BananaBookmarkUnavailable, messageId);
        }
        return LocaleController.formatString(R.string.BananaHistoryPeerNamed, name, messageId);
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
