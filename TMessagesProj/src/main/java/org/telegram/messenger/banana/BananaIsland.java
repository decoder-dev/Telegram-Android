package org.telegram.messenger.banana;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
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
 *
 * Supports ActionBar chips and a lightweight Activity overlay used for chat presence.
 */
public final class BananaIsland {
    private static final String TAG = "banana_island";
    private static Chip overlayChip;
    private static FrameLayout overlayHost;

    private BananaIsland() {}

    /**
     * Attaches a top-center island chip to an ActionBar when enabled.
     * Returns the chip view, or null when disabled / already attached.
     */
    public static View attachToActionBar(ActionBar actionBar, int account) {
        if (actionBar == null || !BananaHooks.bananaIslandEnabled(account)) {
            return null;
        }
        if (actionBar.findViewWithTag(TAG) != null) {
            return actionBar.findViewWithTag(TAG);
        }
        Chip chip = new Chip(actionBar.getContext(), account, null);
        chip.setTag(TAG);
        int top = actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0;
        FrameLayout.LayoutParams lp = LayoutHelper.createFrame(
                LayoutHelper.WRAP_CONTENT, 28,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                0, Math.max(2, (top > 0 ? (top - AndroidUtilities.dp(28)) / 2 : 4)), 0, 0);
        actionBar.addView(chip, lp);
        return chip;
    }

    public static View attachToActionBar(ActionBar actionBar) {
        return attachToActionBar(actionBar, UserConfig.selectedAccount);
    }

    public static void detach(ViewGroup parent) {
        if (parent == null) {
            return;
        }
        View existing = parent.findViewWithTag(TAG);
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

    /** Ensure an Activity-level overlay host exists (no-op when Extra off). */
    public static void attach(Activity activity) {
        if (activity == null || !BananaHooks.bananaIslandEnabled(UserConfig.selectedAccount)) {
            return;
        }
        Window window = activity.getWindow();
        if (window == null) {
            return;
        }
        View decor = window.getDecorView();
        if (!(decor instanceof ViewGroup)) {
            return;
        }
        ViewGroup root = (ViewGroup) decor;
        if (overlayHost != null && overlayHost.getParent() == root) {
            return;
        }
        hide();
        overlayHost = new FrameLayout(activity);
        overlayHost.setTag(TAG + "_host");
        overlayHost.setClickable(false);
        overlayHost.setFocusable(false);
        FrameLayout.LayoutParams hostLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        root.addView(overlayHost, hostLp);
        overlayChip = new Chip(activity, UserConfig.selectedAccount, null);
        overlayChip.setTag(TAG);
        overlayChip.setAlpha(0f);
        overlayChip.setVisibility(View.GONE);
        int top = AndroidUtilities.statusBarHeight > 0 ? Math.max(2, (AndroidUtilities.statusBarHeight - AndroidUtilities.dp(28)) / 2) : AndroidUtilities.dp(8);
        FrameLayout.LayoutParams chipLp = LayoutHelper.createFrame(
                LayoutHelper.WRAP_CONTENT, 28,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                0, top, 0, 0);
        overlayHost.addView(overlayChip, chipLp);
    }

    /**
     * Shows / updates the Activity overlay with an optional chat title when SHOWS_NAME is on.
     * When FOLLOWS_ICON is on, the chip uses the active launcher icon.
     */
    public static void show(Activity activity, String chatTitle) {
        int account = UserConfig.selectedAccount;
        if (activity == null || !BananaHooks.bananaIslandEnabled(account)) {
            hide();
            return;
        }
        attach(activity);
        if (overlayChip == null) {
            return;
        }
        String label = null;
        if (BananaHooks.bananaIslandShowsName(account)) {
            if (chatTitle != null && chatTitle.length() > 0) {
                label = chatTitle;
            } else {
                label = LocaleController.getString(R.string.AppName);
            }
        }
        overlayChip.updateLabel(label);
        overlayChip.setVisibility(View.VISIBLE);
        overlayChip.animate().cancel();
        overlayChip.animate().alpha(1f).setDuration(180).start();
    }

    public static void hide() {
        if (overlayChip != null) {
            overlayChip.animate().cancel();
            overlayChip.setAlpha(0f);
            overlayChip.setVisibility(View.GONE);
        }
        if (overlayHost != null) {
            ViewGroup parent = (ViewGroup) overlayHost.getParent();
            if (parent != null) {
                parent.removeView(overlayHost);
            }
        }
        overlayHost = null;
        overlayChip = null;
    }

    private static final class Chip extends LinearLayout {
        private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int account;
        private TextView labelView;
        private boolean lightBar;

        Chip(Context context, int account, String initialLabel) {
            super(context);
            this.account = account;
            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);
            int padH = AndroidUtilities.dp(8);
            setPadding(padH, AndroidUtilities.dp(4), padH, AndroidUtilities.dp(4));
            setWillNotDraw(false);
            setClickable(false);
            setFocusable(false);

            boolean followsIcon = BananaHooks.bananaIslandFollowsIcon(account);

            if (followsIcon) {
                ImageView icon = new ImageView(context);
                int res = R.mipmap.ic_launcher;
                for (LauncherIconController.LauncherIcon launcherIcon : LauncherIconController.LauncherIcon.values()) {
                    if (LauncherIconController.isEnabled(launcherIcon)) {
                        res = launcherIcon.foreground;
                        break;
                    }
                }
                try {
                    icon.setImageResource(res);
                } catch (Throwable ignore) {
                    try {
                        icon.setImageResource(R.mipmap.ic_launcher);
                    } catch (Throwable ignored) {
                    }
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

            labelView = new TextView(context);
            labelView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            labelView.setTypeface(Typeface.DEFAULT_BOLD);
            labelView.setTextColor(0xFFFFFFFF);
            labelView.setSingleLine(true);
            labelView.setPadding(AndroidUtilities.dp(6), 0, 0, 0);
            labelView.setVisibility(View.GONE);
            addView(labelView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

            bgPaint.setColor(0xE6121212);
            try {
                int accent = Theme.getColor(Theme.key_actionBarDefault);
                lightBar = AndroidUtilities.computePerceivedBrightness(accent) > 0.7f;
                bgPaint.setColor(lightBar ? 0xE6121212 : 0xE6FFFFFF);
                labelView.setTextColor(lightBar ? 0xFFFFFFFF : 0xFF121212);
            } catch (Throwable ignore) {
                lightBar = true;
            }

            if (initialLabel == null && BananaHooks.bananaIslandShowsName(account)) {
                updateLabel(LocaleController.getString(R.string.AppName));
            } else {
                updateLabel(initialLabel);
            }
        }

        void updateLabel(String text) {
            if (labelView == null) {
                return;
            }
            if (text == null || text.length() == 0 || !BananaHooks.bananaIslandShowsName(account)) {
                labelView.setVisibility(View.GONE);
                return;
            }
            if (text.length() > 24) {
                text = text.substring(0, 23) + "…";
            }
            labelView.setText(text);
            labelView.setVisibility(View.VISIBLE);
            requestLayout();
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
