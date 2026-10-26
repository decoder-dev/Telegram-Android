package org.telegram.ui.Cells;

import android.content.Context;
import android.view.View;

/** Keeps Telegram's message positions stable without exposing hidden content to accessibility. */
public final class FilteredMessageCell extends View {
    public FilteredMessageCell(Context context) {
        super(context);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        setFocusable(false); setClickable(false);
    }
    @Override protected void onMeasure(int widthSpec, int heightSpec) { setMeasuredDimension(MeasureSpec.getSize(widthSpec), 1); }
}
