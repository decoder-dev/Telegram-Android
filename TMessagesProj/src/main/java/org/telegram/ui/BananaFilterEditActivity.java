package org.telegram.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import java.util.ArrayList;
import java.util.UUID;
import java.util.regex.Pattern;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.banana.FilterRules;
import org.telegram.messenger.banana.FilterStore;
import org.telegram.messenger.banana.RegexEngine;
import org.telegram.messenger.banana.RegexProtocol;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.BananaSwitchCell;
import org.telegram.ui.Components.BananaTheme;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

/** Regex compilation and preview stay asynchronous, including when pressing Save. */
public final class BananaFilterEditActivity extends UniversalFragment {
    private final long dialogId;
    private final String id, initial;
    private boolean reversed, insensitive, enabled, busy, destroyed;
    private int testStatus;
    private int evaluationGeneration, viewGeneration, lastResultStatus;
    private EditTextBoldCursor expression, sample;
    private RegexEngine engine;
    private FilterStore store;
    public BananaFilterEditActivity(int account, long dialogId, FilterRules.Rule rule) {
        setCurrentAccount(account); this.dialogId = dialogId;
        id = rule == null ? UUID.randomUUID().toString() : rule.id;
        initial = rule == null ? "" : rule.text;
        reversed = rule != null && rule.reversed; insensitive = rule == null || rule.caseInsensitive;
        enabled = rule == null || rule.enabled;
    }
    @Override public Theme.ResourcesProvider getResourceProvider() { return BananaTheme.COLORS; }
    @Override protected CharSequence getTitle() { return text(R.string.BananaFilterEdit); }
    @Override public View createView(Context context) {
        String pattern = expression == null ? initial : expression.getText().toString();
        String example = sample == null ? "" : sample.getText().toString();
        evaluationGeneration++; viewGeneration++;
        if (engine != null) engine.close();
        busy = false;
        if (store == null) store = FilterStore.get(currentAccount);
        engine = new RegexEngine(context);
        expression = field(context, text(R.string.BananaFilterExpression), 5, getResourceProvider());
        expression.setFilters(new InputFilter[]{new InputFilter.LengthFilter(RegexProtocol.MAX_PATTERN)});
        expression.setText(pattern);
        sample = field(context, text(R.string.BananaFilterSample), 4, getResourceProvider());
        sample.setFilters(new InputFilter[]{new InputFilter.LengthFilter(16384)});
        sample.setText(example);
        final int currentView = viewGeneration;
        TextWatcher edited = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable text) {
                if (destroyed || currentView != viewGeneration) return;
                clearPreview();
            }
        };
        expression.addTextChangedListener(edited); sample.addTextChangedListener(edited);
        return super.createView(context);
    }
    private void clearPreview() {
        expression.setError(null);
        if (testStatus != 0) {
            testStatus = 0;
            if (listView != null) listView.adapter.update(false);
        }
    }
    static EditTextBoldCursor field(Context context, String hint, int lines, Theme.ResourcesProvider provider) {
        EditTextBoldCursor field = new EditTextBoldCursor(context);
        field.setTextSize(16); field.setTypeface(Typeface.MONOSPACE);
        field.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, provider));
        field.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText, provider));
        field.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite, provider));
        field.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, provider));
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        field.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(14), AndroidUtilities.dp(20), AndroidUtilities.dp(14));
        field.setHint(hint); field.setContentDescription(hint); field.setMinLines(2); field.setMaxLines(lines);
        field.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        return field;
    }
    @Override protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(text(R.string.BananaFilterExpression)));
        if (expression != null) {
            UItem field = UItem.asCustom(10, expression); field.intValue = android.view.ViewGroup.LayoutParams.WRAP_CONTENT; items.add(field);
        }
        items.add(BananaSwitchCell.Factory.of(1, text(R.string.BananaFilterInsensitive), insensitive));
        items.add(BananaSwitchCell.Factory.of(2, text(R.string.BananaFilterReversed), reversed));
        items.add(BananaSwitchCell.Factory.of(3, text(R.string.BananaFilterEnable), enabled));
        items.add(UItem.asShadow(text(R.string.BananaFilterSyntaxInfo)));
        items.add(UItem.asHeader(text(R.string.BananaFilterSample)));
        if (sample != null) {
            UItem field = UItem.asCustom(11, sample); field.intValue = android.view.ViewGroup.LayoutParams.WRAP_CONTENT; items.add(field);
        }
        items.add(UItem.asButton(4, text(busy ? R.string.Loading : R.string.BananaFilterTest)));
        if (testStatus != 0) items.add(UItem.asShadow(text(testStatus)));
        items.add(UItem.asButton(5, text(R.string.Save)));
        if (busy) for (UItem item : items) if (item.id > 0) item.enabled = false;
    }
    @Override protected void onClick(UItem item, View view, int position, float x, float y) {
        if (busy || destroyed || !store.isCurrent()) return;
        clearPreview();
        if (item.id == 1) insensitive = !insensitive;
        if (item.id == 2) reversed = !reversed;
        if (item.id == 3) enabled = !enabled;
        if (item.id == 4 || item.id == 5) evaluate(item.id == 5);
        listView.adapter.update(true);
    }
    private void evaluate(boolean save) {
        String pattern = expression.getText().toString();
        if (pattern.isEmpty()) { expression.setError(text(R.string.BananaFilterInvalidExpression)); return; }
        busy = true;
        final int revision = ++evaluationGeneration;
        expression.setEnabled(false); sample.setEnabled(false);
        int flags = Pattern.MULTILINE | (insensitive ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0);
        boolean invert = reversed, ignoreCase = insensitive, active = enabled;
        engine.match(pattern, flags, new String[]{sample.getText().toString()}, result -> {
            if (destroyed || revision != evaluationGeneration || !store.isCurrent()) return;
            lastResultStatus = result.status;
            busy = false;
            expression.setEnabled(true); sample.setEnabled(true);
            if (result.status != RegexProtocol.OK) {
                testStatus = result.status == RegexProtocol.TIMEOUT ? R.string.BananaFilterTooSlow :
                        result.status == RegexProtocol.BAD_PATTERN || result.status == RegexProtocol.INPUT_LIMIT ?
                        R.string.BananaFilterInvalidExpression : R.string.BananaFilterUnavailable;
                expression.setError(text(testStatus));
            }
            else if (save) {
                try {
                    store.save(store.rules().put(new FilterRules.Rule(id, dialogId, pattern, invert, ignoreCase, active)));
                    finishFragment(); return;
                } catch (Exception exception) { expression.setError(text(R.string.BananaFiltersInvalidData)); }
            } else testStatus = invert != result.matches[0] ? R.string.BananaFilterHidden : R.string.BananaFilterVisible;
            if (listView != null) listView.adapter.update(true);
        });
    }
    private static String text(int id) { return LocaleController.getString(id); }
    @Override protected boolean onLongClick(UItem item, View view, int position, float x, float y) { return false; }
    @Override public void onResume() {
        super.onResume();
        if (store != null && !store.isCurrent()) finishFragment();
    }
    @Override public void onFragmentDestroy() {
        destroyed = true; evaluationGeneration++; viewGeneration++;
        if (engine != null) engine.close(); super.onFragmentDestroy();
    }
}
