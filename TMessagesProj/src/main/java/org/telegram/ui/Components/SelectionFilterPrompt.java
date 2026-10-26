package org.telegram.ui.Components;

import android.content.Context;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongConsumer;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;

/** Choosing a scope only opens a draft; it never saves rules or enables filtering. */
public final class SelectionFilterPrompt {
    private SelectionFilterPrompt() { }
    public static AlertDialog create(Context context, Theme.ResourcesProvider colors, long dialogId, LongConsumer selected) {
        AtomicBoolean completed = new AtomicBoolean();
        LongConsumer finish = scope -> { if (completed.compareAndSet(false, true)) selected.accept(scope); };
        return new AlertDialog.Builder(context, colors)
                .setTitle(LocaleController.getString(R.string.BananaFilterAdd))
                .setMessage(LocaleController.getString(R.string.BananaFilterSelectionInfo))
                .setPositiveButton(LocaleController.getString(R.string.BananaFilterThisChat), (dialog, which) -> finish.accept(dialogId))
                .setNeutralButton(LocaleController.getString(R.string.BananaFilterShared), (dialog, which) -> finish.accept(0))
                .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
                .setOnDismissListener(dialog -> completed.set(true))
                .create();
    }
}
