package org.telegram.ui.Components;

import android.content.Context;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Presentation only; the viewer validates account ownership and applies the selected policy. */
public final class StoryPrivacyPrompt {
    public enum Choice { ENABLE_GHOST, OPEN_NORMALLY, CANCEL }
    private StoryPrivacyPrompt() {}

    public static AlertDialog create(Context context, Theme.ResourcesProvider colors, Consumer<Choice> selected) {
        return create(context, colors, selected, () -> {});
    }

    public static AlertDialog create(Context context, Theme.ResourcesProvider colors, Consumer<Choice> selected, Runnable dismissed) {
        AtomicBoolean completed = new AtomicBoolean();
        Consumer<Choice> finish = choice -> {
            if (completed.compareAndSet(false, true)) selected.accept(choice);
        };
        return new AlertDialog.Builder(context, colors)
                .setTitle(LocaleController.getString(R.string.BananaStoryPromptTitle))
                .setMessage(LocaleController.getString(R.string.BananaStoryPromptInfo))
                .setPositiveButton(LocaleController.getString(R.string.BananaStoryEnableGhost), (dialog, which) -> finish.accept(Choice.ENABLE_GHOST))
                .setNeutralButton(LocaleController.getString(R.string.BananaStoryOpenNormally), (dialog, which) -> finish.accept(Choice.OPEN_NORMALLY))
                .setNegativeButton(LocaleController.getString(R.string.Cancel), (dialog, which) -> finish.accept(Choice.CANCEL))
                .setOnDismissListener(dialog -> {
                    try { finish.accept(Choice.CANCEL); }
                    finally { dismissed.run(); }
                })
                .create();
    }
}
