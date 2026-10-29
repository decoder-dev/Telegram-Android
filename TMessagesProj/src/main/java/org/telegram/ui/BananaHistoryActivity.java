package org.telegram.ui;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.banana.BananaConfig;
import org.telegram.messenger.banana.HistoryDatabase;
import org.telegram.messenger.banana.MessageHistory;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.BananaValueCell;
import org.telegram.ui.Components.BananaTheme;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.BananaSettingsFragment;

import java.util.ArrayList;
import java.util.List;

/** Local-only archive. It never restores deleted messages to Telegram or sends read receipts. */
public final class BananaHistoryActivity extends BananaSettingsFragment {
    private List<HistoryDatabase.Record> records = new ArrayList<>();
    private String query = "";
    private boolean loading = true, failed, destroyed;
    private int generation;
    private final Runnable search = this::reload;

    public BananaHistoryActivity(int account) { setCurrentAccount(account); }

    @Override
    public Theme.ResourcesProvider getResourceProvider() { return BananaTheme.forExtras(); }

    @Override
    protected CharSequence getTitle() { return LocaleController.getString(R.string.BananaHistory); }

    @Override
    public View createView(Context context) {
        View view = super.createView(context);
        actionBar.createMenu().addItem(1, R.drawable.outline_header_search).setIsSearchField(true)
                .setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {
                    @Override public void onTextChanged(EditText editText) {
                        String text = editText.getText().toString();
                        query = text.substring(0, Math.min(256, text.length()));
                        generation++;
                        AndroidUtilities.cancelRunOnUIThread(search);
                        AndroidUtilities.runOnUIThread(search, 250);
                    }
                }).setSearchFieldHint(LocaleController.getString(R.string.Search));
        reload();
        return view;
    }

    private void reload() {
        int request = ++generation;
        loading = true;
        MessageHistory.load(currentAccount, query, 0, result -> {
            if (destroyed || request != generation) return;
            loading = false;
            failed = result == null;
            records = result == null ? new ArrayList<>() : result;
            if (listView != null) listView.adapter.update(true);
        });
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asShadow(LocaleController.getString(R.string.BananaHistoryIntro)));
        if (loading || failed || records.isEmpty()) {
            items.add(UItem.asShadow(LocaleController.getString(loading ? R.string.Loading : failed ?
                    R.string.BananaHistoryError : query.isEmpty() ? R.string.BananaHistoryEmpty : R.string.BananaHistoryNoMatches)));
        }
        for (int i = 0; i < records.size(); i++) {
            HistoryDatabase.Record record = records.get(i);
            String title = historyMark(record.deleted) + " · " + LocaleController.formatDateAudio(record.savedAt / 1000, true);
            String display = display(record);
            int end = Math.min(500, display.length());
            if (end < display.length() && Character.isHighSurrogate(display.charAt(end - 1))) end--;
            String text = end < display.length() ? display.substring(0, end) + "…" : display;
            items.add(BananaValueCell.Factory.of(100 + i, title, text));
        }
        items.add(UItem.asButton(2, LocaleController.getString(R.string.BananaHistoryClear)).red());
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == 2) {
            showDialog(new AlertDialog.Builder(getParentActivity(), getResourceProvider())
                    .setTitle(LocaleController.getString(R.string.BananaHistoryClear))
                    .setMessage(LocaleController.getString(R.string.BananaHistoryClearInfo))
                    .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
                    .setPositiveButton(LocaleController.getString(R.string.BananaHistoryClear), (dialog, which) -> {
                        MessageHistory.clear(currentAccount, success -> {
                            if (destroyed) return;
                            if (success) reload();
                            else { failed = true; loading = false; listView.adapter.update(true); }
                        });
                    }).create());
        } else if (item.id >= 100 && item.id - 100 < records.size()) {
            HistoryDatabase.Record record = records.get(item.id - 100);
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider())
                    .setTitle(peerTitle(record))
                    .setMessage(display(record))
                    .setNegativeButton(LocaleController.getString(R.string.Close), null);
            // Media-only records have no text to copy.
            if (!record.text.isEmpty()) builder.setPositiveButton(LocaleController.getString(R.string.Copy),
                    (dialog, which) -> AndroidUtilities.addToClipboard(record.text));
            if (record.attachment != null) builder.setNeutralButton(LocaleController.getString(R.string.BananaHistoryOpenAttachment),
                    (dialog, which) -> MessageHistory.attachment(currentAccount, record, file -> {
                        if (destroyed || getParentActivity() == null) return;
                        try {
                            if (file != null && AndroidUtilities.openForView(file, record.attachment.displayName,
                                    record.attachment.mimeType, getParentActivity(), getResourceProvider(), false)) return;
                        } catch (Exception error) { org.telegram.messenger.FileLog.e(error); }
                        showDialog(new AlertDialog.Builder(getParentActivity(), getResourceProvider())
                                .setMessage(LocaleController.getString(R.string.BananaHistoryAttachmentUnavailable))
                                .setPositiveButton(LocaleController.getString(R.string.OK), null).create());
                    }));
            showDialog(builder.create());
        }
    }

    /** Chat name when Telegram already knows the peer; the raw id only as a fallback. */
    private String peerTitle(HistoryDatabase.Record record) {
        String name = null;
        if (record.dialogId > 0) {
            TLRPC.User user = getMessagesController().getUser(record.dialogId);
            if (user != null) name = UserObject.getUserName(user);
        } else {
            TLRPC.Chat chat = getMessagesController().getChat(-record.dialogId);
            if (chat != null) name = chat.title;
        }
        return TextUtils.isEmpty(name) ? LocaleController.formatString(R.string.BananaHistoryPeer, record.dialogId, record.messageId)
                : LocaleController.formatString(R.string.BananaHistoryPeerNamed, name, record.messageId);
    }

    private String historyMark(boolean deleted) {
        if (deleted) {
            String mark = BananaConfig.deletedMessageMark(currentAccount);
            return mark == null || mark.isEmpty() ? LocaleController.getString(R.string.BananaHistoryDeleted) : mark;
        }
        String mark = BananaConfig.editedMessageMark(currentAccount);
        return mark == null || mark.isEmpty() ? LocaleController.getString(R.string.BananaHistoryEdited) : mark;
    }

    private String display(HistoryDatabase.Record record) {
        if (record.attachment == null) return record.text;
        int[] labels = {R.string.BananaHistoryPhoto, R.string.BananaHistoryVideo, R.string.BananaHistoryAudio, R.string.BananaHistoryFile};
        int kind = record.attachment.kind;
        String label = LocaleController.getString(labels[Math.max(0, Math.min(3, kind - 1))]);
        String name = record.attachment.displayName;
        return label + (name.isEmpty() ? "" : " · " + name) +
                (record.attachment.storedName == null ? " · " + LocaleController.getString(R.string.BananaHistoryMediaNotSaved) : "") +
                (record.text.isEmpty() ? "" : "\n" + record.text);
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) { return false; }

    @Override
    public void onFragmentDestroy() {
        destroyed = true;
        generation++;
        AndroidUtilities.cancelRunOnUIThread(search);
        super.onFragmentDestroy();
    }
}
