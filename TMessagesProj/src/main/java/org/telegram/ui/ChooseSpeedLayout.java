package org.telegram.ui;

import android.content.Context;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;


import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.ActionBarMenuSlider;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.PopupSwipeBackLayout;

public class ChooseSpeedLayout {

    public ActionBarPopupWindow.ActionBarPopupWindowLayout speedSwipeBackLayout;
    ActionBarMenuSlider.SpeedSlider slider;

    private static final float MIN_SPEED = ActionBarMenuSlider.SpeedSlider.MIN_SPEED;
    private static final float MAX_SPEED = ActionBarMenuSlider.SpeedSlider.MAX_SPEED;

    private final ActionBarMenuSubItem[] speedItems;
    public ChooseSpeedLayout(Context context, PopupSwipeBackLayout swipeBackLayout, Callback callback) {
        speedSwipeBackLayout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(context, 0, null);
        speedSwipeBackLayout.setFitItems(true);

        ActionBarMenuSubItem backItem = ActionBarMenuItem.addItem(speedSwipeBackLayout, R.drawable.msg_arrow_back, LocaleController.getString(R.string.Back), false, null);
        backItem.setOnClickListener(view -> {
            swipeBackLayout.closeForeground();
        });
        backItem.setColors(0xfffafafa, 0xfffafafa);
        backItem.setSelectorColor(0x0fffffff);

        FrameLayout gap = new FrameLayout(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        };
        gap.setMinimumWidth(AndroidUtilities.dp(196));
        gap.setBackgroundColor(0xff181818);
        speedSwipeBackLayout.addView(gap);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) gap.getLayoutParams();
        if (LocaleController.isRTL) {
            layoutParams.gravity = Gravity.RIGHT;
        }
        layoutParams.width = LayoutHelper.MATCH_PARENT;
        layoutParams.height = AndroidUtilities.dp(8);
        gap.setLayoutParams(layoutParams);

        slider = new ActionBarMenuSlider.SpeedSlider(context, null);
        slider.setMinimumWidth(AndroidUtilities.dp(196));
        slider.setDrawShadow(false);
        slider.setBackgroundColor(0xff222222);
        slider.setTextColor(0xffffffff);
        slider.setOnValueChange((value, isFinal) -> {
            final float speed = MIN_SPEED + (MAX_SPEED - MIN_SPEED) * value;
            callback.onSpeedSelected(speed, isFinal, false);
        });
        speedSwipeBackLayout.addView(slider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 44));

        gap = new FrameLayout(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        };
        gap.setMinimumWidth(AndroidUtilities.dp(196));
        gap.setBackgroundColor(0xff181818);
        speedSwipeBackLayout.addView(gap);
        layoutParams = (LinearLayout.LayoutParams) gap.getLayoutParams();
        if (LocaleController.isRTL) {
            layoutParams.gravity = Gravity.RIGHT;
        }
        layoutParams.width = LayoutHelper.MATCH_PARENT;
        layoutParams.height = AndroidUtilities.dp(8);
        gap.setLayoutParams(layoutParams);

        speedItems = SpeedButtonsLayout.addSpeedItems(speedSwipeBackLayout, callback::onSpeedSelected);
    }

    public void update(float currentVideoSpeed, boolean isFinal) {
        SpeedButtonsLayout.updateSpeedItems(speedItems, currentVideoSpeed, isFinal);
        slider.setSpeed(currentVideoSpeed, true);
    }

    public interface Callback {
        void onSpeedSelected(float speed, boolean isFinal, boolean closeMenu);
    }
}
