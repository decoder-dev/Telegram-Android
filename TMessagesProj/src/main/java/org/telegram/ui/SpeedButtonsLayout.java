package org.telegram.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;


import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.SpeedIconDrawable;

public class SpeedButtonsLayout extends LinearLayout {

    static final float[] PRESET_SPEEDS = {0.2f, 0.5f, 1f, 1.5f, 2f, 3f, 4f};
    private final ActionBarMenuSubItem[] speedItems;

    public SpeedButtonsLayout(Context context, Callback callback) {
        super(context);
        setOrientation(VERTICAL);
        speedItems = addSpeedItems(this, callback);

        FrameLayout gap = new FrameLayout(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        };
        gap.setMinimumWidth(AndroidUtilities.dp(196));
        gap.setBackgroundColor(0xff181818);
        addView(gap);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) gap.getLayoutParams();
        if (LocaleController.isRTL) {
            layoutParams.gravity = Gravity.RIGHT;
        }
        layoutParams.width = LayoutHelper.MATCH_PARENT;
        layoutParams.height = AndroidUtilities.dp(8);
        gap.setLayoutParams(layoutParams);
    }

    static ActionBarMenuSubItem[] addSpeedItems(android.view.ViewGroup parent, Callback callback) {
        final int[] icons = {R.drawable.msg_speed_0_2, R.drawable.msg_speed_slow,
                R.drawable.msg_speed_normal, R.drawable.msg_speed_fast, R.drawable.msg_speed_superfast};
        final int[] labels = {R.string.SpeedVerySlow, R.string.SpeedSlow,
                R.string.SpeedNormal, R.string.SpeedFast, R.string.SpeedVeryFast};
        ActionBarMenuSubItem[] items = new ActionBarMenuSubItem[PRESET_SPEEDS.length];
        for (int i = 0; i < items.length; i++) {
            final float speed = PRESET_SPEEDS[i];
            final CharSequence label = i < labels.length ? LocaleController.getString(labels[i]) :
                    SpeedIconDrawable.formatNumber(speed) + "×";
            ActionBarMenuSubItem item = ActionBarMenuItem.addItem(parent,
                    i < icons.length ? icons[i] : 0, label, false, null);
            if (i >= icons.length) {
                SpeedIconDrawable icon = new SpeedIconDrawable();
                icon.setValue(speed, false);
                icon.setColor(0xfffafafa);
                item.setIcon(icon);
            }
            item.setColors(0xfffafafa, 0xfffafafa);
            item.setSelectorColor(0x0fffffff);
            item.setOnClickListener(view -> callback.onSpeedSelected(speed, true, true));
            items[i] = item;
        }
        return items;
    }

    static void updateSpeedItems(ActionBarMenuSubItem[] items, float speed, boolean isFinal) {
        for (int i = 0; i < items.length; i++) {
            final float tolerance = i == 0 ? 0.01f : 0.1f;
            final int color = isFinal && Math.abs(speed - PRESET_SPEEDS[i]) < tolerance ?
                    0xff6BB6F9 : 0xfffafafa;
            items[i].setColors(color, color);
            if (i >= 5) {
                ((SpeedIconDrawable) items[i].getImageView().getDrawable()).setColor(color);
            }
        }
    }

    public void update(float currentVideoSpeed, boolean isFinal) {
        updateSpeedItems(speedItems, currentVideoSpeed, isFinal);
    }

    public interface Callback {
        void onSpeedSelected(float speed, boolean isFinal, boolean closeMenu);
    }
}
