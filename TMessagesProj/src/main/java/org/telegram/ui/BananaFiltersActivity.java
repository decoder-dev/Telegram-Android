package org.telegram.ui;

import android.content.Context;
import android.text.InputFilter;
import android.view.View;
import java.util.ArrayList;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.banana.FilterRules;
import org.telegram.messenger.banana.FilterStore;
import org.telegram.messenger.banana.MessageFilters;
import org.telegram.messenger.banana.RemoteFilterSource;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.BananaSwitchCell;
import org.telegram.ui.Cells.BananaValueCell;
import org.telegram.ui.Components.BananaTheme;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

/** Native shared/per-chat rule management; imported data always receives a separate preview. */
public final class BananaFiltersActivity extends UniversalFragment {
    private final long dialogId;
    private FilterStore store;
    private final String sourceLink;
    private boolean sourceStarted, loading, destroyed;
    private int requestGeneration;
    private RemoteFilterSource.Task sourceTask;
    private String sourceHost = "";
    private final ArrayList<FilterRules.Rule> displayed = new ArrayList<>();
    private final Runnable changed = () -> { if (listView != null) listView.adapter.update(true); };
    public BananaFiltersActivity(int account, long dialogId) { this(account, dialogId, null); }
    public BananaFiltersActivity(int account, long dialogId, String sourceLink) {
        setCurrentAccount(account); this.dialogId = dialogId; this.sourceLink = sourceLink;
    }
    @Override public Theme.ResourcesProvider getResourceProvider() { return BananaTheme.COLORS; }
    @Override protected CharSequence getTitle() { return text(R.string.BananaFilters); }
    @Override public View createView(Context context) {
        if (store == null) store = FilterStore.get(currentAccount);
        MessageFilters.addListener(currentAccount, changed);
        return super.createView(context);
    }
    @Override public void onResume() {
        super.onResume();
        if (store != null && !store.isCurrent()) { finishFragment(); return; }
        if (listView != null) listView.adapter.update(true);
    }
    @Override public void onBecomeFullyVisible() {
        super.onBecomeFullyVisible();
        if (sourceLink != null && !sourceStarted) { sourceStarted = true; download(sourceLink); }
    }
    @Override protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (store == null) store = FilterStore.get(currentAccount);
        items.add(BananaSwitchCell.Factory.of(1, text(R.string.BananaFiltersEnabled), store.enabled()));
        items.add(BananaSwitchCell.Factory.of(2, text(R.string.BananaFiltersInChats), store.sharedInChats()));
        items.add(UItem.asShadow(text(R.string.BananaFiltersInfo)));
        if (store.error() != 0) items.add(UItem.asShadow(text(R.string.BananaFilterInactiveError)));
        if (dialogId != 0) items.add(UItem.asHeader(DialogObject.getName(currentAccount, dialogId)));
        items.add(UItem.asButton(3, text(R.string.BananaFilterAdd)));
        displayed.clear();
        for (FilterRules.Rule rule : store.rules().rules) {
            if (dialogId != 0 && rule.dialogId != 0 && rule.dialogId != dialogId) continue;
            String scope = rule.dialogId == 0 ? text(R.string.BananaFilterShared) : DialogObject.getName(currentAccount, rule.dialogId);
            if (scope.isEmpty()) scope = text(R.string.BananaFilterPrivateChat);
            String status = text(!rule.enabled || dialogId != 0 && store.rules().excluded(rule, dialogId) ? R.string.BananaFilterOff : R.string.BananaFilterOn);
            displayed.add(rule);
            items.add(BananaValueCell.Factory.of(100 + displayed.size() - 1, shortText(rule.text), scope + " · " + status));
        }
        items.add(UItem.asShadow(text(R.string.BananaFiltersFormatInfo)));
        items.add(UItem.asButton(4, text(R.string.BananaFiltersImport)));
        items.add(UItem.asButton(5, text(R.string.BananaFiltersExport)));
        if (loading) {
            items.add(UItem.asShadow(text(R.string.BananaFiltersDownloading) + "\n" + sourceHost));
            items.add(UItem.asButton(7, text(R.string.Cancel)));
        } else items.add(UItem.asButton(6, text(R.string.BananaFiltersFromLink)));
        if (loading) for (UItem item : items) if (item.id > 0 && item.id != 7) item.enabled = false;
    }
    @Override protected void onClick(UItem item, View view, int position, float x, float y) {
        if (loading && item.id != 7) return;
        if (!store.isCurrent()) { finishFragment(); return; }
        if (item.id == 1) store.setEnabled(!store.enabled());
        else if (item.id == 2) store.setSharedInChats(!store.sharedInChats());
        else if (item.id == 3) {
            if (store.rules().rules.size() >= FilterRules.MAX_RULES) { error(); return; }
            presentFragment(new BananaFilterEditActivity(currentAccount, dialogId, null));
        } else if (item.id == 4) importRules();
        else if (item.id == 6) importLink();
        else if (item.id == 7) {
            requestGeneration++; loading = false;
            if (sourceTask != null) sourceTask.cancel();
        }
        else if (item.id == 5) {
            try {
                String data = store.rules().export();
                showDialog(new AlertDialog.Builder(getParentActivity(), getResourceProvider())
                        .setTitle(text(R.string.BananaFiltersExport)).setMessage(data)
                        .setNegativeButton(text(R.string.Cancel), null)
                        .setPositiveButton(text(R.string.Copy), (dialog, which) -> AndroidUtilities.addToClipboard(data)).create());
            } catch (Exception exception) { error(); }
        } else if (item.id >= 100 && item.id - 100 < displayed.size()) edit(displayed.get(item.id - 100));
        if (listView != null) listView.adapter.update(true);
    }
    private void edit(FilterRules.Rule rule) {
        ArrayList<CharSequence> choices = new ArrayList<>();
        choices.add(text(R.string.Edit));
        choices.add(text(rule.enabled ? R.string.BananaFilterDisable : R.string.BananaFilterEnable));
        choices.add(text(R.string.Delete));
        if (dialogId != 0 && rule.dialogId == 0) choices.add(text(store.rules().excluded(rule, dialogId) ? R.string.BananaFilterInclude : R.string.BananaFilterExclude));
        showDialog(new AlertDialog.Builder(getParentActivity(), getResourceProvider()).setTitle(shortText(rule.text))
                .setItems(choices.toArray(new CharSequence[0]), (dialog, which) -> {
                    if (!store.isCurrent()) return;
                    if (which == 0) presentFragment(new BananaFilterEditActivity(currentAccount, rule.dialogId, rule));
                    if (which == 1) store.save(store.rules().put(rule.withEnabled(!rule.enabled)));
                    if (which == 2) showDialog(new AlertDialog.Builder(getParentActivity(), getResourceProvider())
                            .setTitle(text(R.string.BananaFilterDeleteTitle)).setMessage(shortText(rule.text))
                            .setNegativeButton(text(R.string.Cancel), null)
                            .setPositiveButton(text(R.string.Delete), (d, w) -> { if (store.isCurrent()) store.save(store.rules().remove(rule)); }).create());
                    if (which == 3) store.save(store.rules().exclude(rule, dialogId, !store.rules().excluded(rule, dialogId)));
                }).create());
    }
    private void importRules() {
        EditTextBoldCursor input = BananaFilterEditActivity.field(getParentActivity(), text(R.string.BananaFiltersPaste), 6, getResourceProvider());
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(FilterRules.MAX_JSON)});
        showDialog(new AlertDialog.Builder(getParentActivity(), getResourceProvider()).setTitle(text(R.string.BananaFiltersImport))
                .setView(input).setNegativeButton(text(R.string.Cancel), null)
                .setPositiveButton(text(R.string.BananaFiltersPreview), (dialog, which) -> {
                    previewImport(input.getText().toString());
                }).create());
    }
    private void previewImport(String json) {
        if (destroyed || !store.isCurrent()) return;
        try {
            FilterRules previous = store.rules(), preview = previous.merge(json);
            String description = LocaleController.formatString(R.string.BananaFiltersPreviewInfo, previous.rules.size(), preview.rules.size());
            StringBuilder summary = new StringBuilder(description);
            for (FilterRules.Rule rule : preview.rules) summary.append("\n\n").append(shortText(rule.text));
            showDialog(new AlertDialog.Builder(getParentActivity(), getResourceProvider()).setTitle(text(R.string.BananaFiltersPreview))
                    .setMessage(summary).setNegativeButton(text(R.string.Cancel), null)
                    .setPositiveButton(text(R.string.BananaFiltersApply), (d, w) -> {
                        if (!store.isCurrent() || store.rules() != previous) { error(); return; }
                        store.save(preview);
                    }).create());
        } catch (Exception exception) { error(); }
    }
    private void importLink() {
        EditTextBoldCursor input = BananaFilterEditActivity.field(getParentActivity(), "https://…", 3, getResourceProvider());
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(RemoteFilterSource.MAX_URL)});
        showDialog(new AlertDialog.Builder(getParentActivity(), getResourceProvider()).setTitle(text(R.string.BananaFiltersFromLink))
                .setMessage(text(R.string.BananaFiltersSourceInfo)).setView(input)
                .setNegativeButton(text(R.string.Cancel), null)
                .setPositiveButton(text(R.string.BananaFiltersDownload), (dialog, which) -> download(input.getText().toString())).create());
    }
    private void download(String link) {
        if (destroyed || !store.isCurrent()) return;
        try {
            java.net.URI source = RemoteFilterSource.url(link);
            int revision = ++requestGeneration;
            if (sourceTask != null) sourceTask.cancel();
            loading = true; sourceHost = source.getHost();
            listView.adapter.update(true);
            sourceTask = RemoteFilterSource.fetch(source, result -> AndroidUtilities.runOnUIThread(() -> {
                if (destroyed || revision != requestGeneration || !store.isCurrent()) return;
                loading = false; sourceTask = null; listView.adapter.update(true);
                if (result.json != null) previewImport(result.json); else sourceError();
            }));
        } catch (IllegalArgumentException exception) { sourceError(); }
    }
    private void sourceError() {
        if (destroyed || getParentActivity() == null) return;
        showDialog(new AlertDialog.Builder(getParentActivity(), getResourceProvider()).setMessage(text(R.string.BananaFiltersSourceError))
                .setPositiveButton(text(R.string.OK), null).create());
    }
    private void error() {
        showDialog(new AlertDialog.Builder(getParentActivity(), getResourceProvider()).setMessage(text(R.string.BananaFiltersInvalidData))
                .setPositiveButton(text(R.string.OK), null).create());
    }
    private static String shortText(String value) {
        int end = Math.min(180, value.length());
        if (end < value.length() && Character.isHighSurrogate(value.charAt(end - 1))) end--;
        return value.substring(0, end) + (end < value.length() ? "…" : "");
    }
    private static String text(int id) { return LocaleController.getString(id); }
    @Override protected boolean onLongClick(UItem item, View view, int position, float x, float y) { return false; }
    @Override public void onFragmentDestroy() {
        destroyed = true; requestGeneration++;
        if (sourceTask != null) sourceTask.cancel();
        MessageFilters.removeListener(currentAccount, changed); super.onFragmentDestroy();
    }
}
