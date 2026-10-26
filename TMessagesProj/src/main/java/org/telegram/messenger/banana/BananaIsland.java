package org.telegram.messenger.banana;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.LauncherIconController;

/**
 * Compact Dynamic-Island style chip for BananaGram Extras.
 * Gates: BANANA_ISLAND_ENABLED / SHOWS_NAME / FOLLOWS_ICON.
 */
public final class BananaIsland {
    private BananaIsland() {}

    /**
     * Attaches a top-center island chip to an ActionBar when enabled.
     * Returns the chip view, or null when disabled / already attached.
     */
    public static View attachToActionBar(ActionBar actionBar, int account) {
        if (actionBar == null || !BananaHooks.bananaIslandEnabled(account)) {
            return null;
        }
        if (actionBar.findViewWithTag("banana_island") != null) {
            return actionBar.findViewWithTag("banana_island");
        }
        Chip chip = new Chip(actionBar.getContext(), account);
        chip.setTag("banana_island");
        int top = actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0;
        FrameLayout.LayoutParams lp = LayoutHelper.createFrame(
                LayoutHelper.WRAP_CONTENT, 28,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                0, Math.max(2, (top > 0 ? (top - AndroidUtilities.dp(28)) / 2 : 4)), 0, 0);
        actionBar.addView(chip, lp);
        return chip;
    }

    /** Convenience for the selected account. */
    public static View attachToActionBar(ActionBar actionBar) {
        return attachToActionBar(actionBar, UserConfig.selectedAccount);
    }

    public static void detach(ViewGroup parent) {
        if (parent == null) {
            return;
        }
        View existing = parent.findViewWithTag("banana_island");
        if (existing != null) {
            parent.removeView(existing);
        }
    }

    public static void refresh(ActionBar actionBar, int account) {
        if (actionBar == null) {
            return;
        }
        detach(actionBar);
        attachToActionBar(actionBar, account);
    }

    private static final class Chip extends LinearLayout {
        private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int account;

        Chip(Context context, int account) {
            super(context);
            this.account = account;
            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);
            int padH = AndroidUtilities.dp(BananaHooks.bananaIslandShowsName(account) ? 10 : 8);
            setPadding(padH, AndroidUtilities.dp(4), padH, AndroidUtilities.dp(4));
            setWillNotDraw(false);
            setClickable(false);
            setFocusable(false);

            boolean followsIcon = BananaHooks.bananaIslandFollowsIcon(account);
            boolean showsName = BananaHooks.bananaIslandShowsName(account);

            if (followsIcon) {
                ImageView icon = new ImageView(context);
                int res = R.drawable.ic_launcher_dr;
                for (LauncherIconController.LauncherIcon launcherIcon : LauncherIconController.LauncherIcon.values()) {
                    if (LauncherIconController.isEnabled(launcherIcon)) {
                        res = launcherIcon.foreground;
                        break;
                    }
                }
                try {
                    icon.setImageResource(res);
                } catch (Throwable ignore) {
                    icon.setImageResource(R.mipmap.ic_launcher);
                }
                icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
                addView(icon, LayoutHelper.createLinear(18, 18, Gravity.CENTER_VERTICAL));
            } else {
                View dot = new View(context) {
                    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                    @Override
                    protected void onDraw(Canvas canvas) {
                        p.setColor(0xFFFFD54F);
                        float r = Math.min(getWidth(), getHeight()) / 2f;
                        canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, r, p);
                    }
                };
                addView(dot, LayoutHelper.createLinear(10, 10, Gravity.CENTER_VERTICAL));
            }

            if (showsName) {
                TextView label = new TextView(context);
                label.setText(LocaleController.getString(R.string.AppName));
                label.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
                label.setTypeface(Typeface.DEFAULT_BOLD);
                label.setTextColor(0xFFFFFFFF);
                label.setSingleLine(true);
                label.setPadding(AndroidUtilities.dp(followsIcon || true ? 6 : 0), 0, 0, 0);
                addView(label, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));
            }

            bgPaint.setColor(0xE6121212);
            try {
                int accent = Theme.getColor(Theme.key_actionBarDefault);
                // Dark pill; keep readable over light/dark action bars.
                boolean light = AndroidUtilities.computePerceivedBrightness(accent) > 0.7f;
                bgPaint.setColor(light ? 0xE6121212 : 0xE6FFFFFF);
                if (getChildCount() > 0) {
                    for (int i = 0; i < getChildCount(); i++) {
                        View child = getChildAt(i);
                        if (child instanceof TextView) {
                            ((TextView) child).setTextColor(light ? 0xFFFFFFFF : 0xFF121212);
                        }
                    }
                }
            } catch (Throwable ignore) {
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float r = getHeight() / 2f;
            canvas.drawRoundRect(0, 0, getWidth(), getHeight(), r, r, bgPaint);
            super.onDraw(canvas);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(28), MeasureSpec.EXACTLY));
        }
    }
}
