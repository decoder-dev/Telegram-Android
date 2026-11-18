package org.telegram.ui.Components;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.telegram.messenger.ContactsController;
import org.telegram.messenger.LiteMode;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;
import org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundColorProvider;
import org.telegram.ui.Components.blur3.drawable.color.impl.BlurredBackgroundProviderImpl;
import org.telegram.ui.Components.chat.ViewPositionWatcher;
import org.telegram.ui.Stories.recorder.StoryEntry;
import org.telegram.utils.glass.source.GlassSourceColor;
import org.telegram.utils.glass.source.GlassSourceRenderNode;
import org.telegram.utils.glass.source.GlassSourceWrapped;

/**
 * A chat header and composer in liquid glass over the current wallpaper, so the highlight style and the glass
 * settings show as they are chosen (after Cherrygram's glare preview). It is a picture only: touches go nowhere.
 */
@SuppressLint("ViewConstructor")
public final class BananaGlassPreviewView extends FrameLayout {
    public static final int HEIGHT_DP = 150;

    private Drawable backgroundDrawable;
    private BackgroundGradientDrawable.Disposable backgroundGradientDisposable;

    public BananaGlassPreviewView(Context context, BaseFragment fragment, int account, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        setWillNotDraw(false);
        setClipChildren(false);

        final BlurredBackgroundDrawableViewFactory factory = createFactory(resourcesProvider);
        final BlurredBackgroundColorProvider colors = BlurredBackgroundProviderImpl.topPanelChatActivity(resourcesProvider);

        ActionBar actionBar = new ActionBar(context, resourcesProvider);
        actionBar.setOccupyStatusBar(false);
        actionBar.setCastShadows(false);
        actionBar.setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon, resourcesProvider), false);
        actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSelector, resourcesProvider), false);
        actionBar.setBackButtonDrawable(new BackDrawable(false));
        ActionBarMenu menu = actionBar.createMenu();
        menu.addItem(0, R.drawable.ic_ab_other).setContentDescription(LocaleController.getString(R.string.AccDescrMoreOptions));
        actionBar.setupGlass(factory, colors);
        addView(actionBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 70, Gravity.TOP, 0, 4, 0, 0));

        ChatAvatarContainer avatar = new ChatAvatarContainer(context, fragment, false, resourcesProvider);
        avatar.setOccupyStatusBar(false);
        avatar.setGlassMode();
        avatar.allowShorterStatus = true;
        TLRPC.User user = previewUser(account);
        avatar.setUserAvatar(user);
        avatar.setTitle(previewTitle(user));
        avatar.setSubtitle(LocaleController.getString(R.string.Online));
        actionBar.addView(avatar, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.START | Gravity.TOP, 54, 0, 54, 0));
        actionBar.setChatAvatarContainer(avatar);

        final int iconColor = Theme.getColor(Theme.key_glass_defaultIcon, resourcesProvider);

        FrameLayout field = new FrameLayout(context);
        BlurredBackgroundDrawable fieldGlass = factory.create(field, colors);
        fieldGlass.setRadius(dp(22));
        field.setBackground(fieldGlass);
        field.addView(icon(context, R.drawable.input_smile, iconColor), LayoutHelper.createFrame(44, 44, Gravity.START | Gravity.CENTER_VERTICAL));
        TextView hint = new TextView(context);
        hint.setText(LocaleController.getString(R.string.TypeMessage));
        hint.setTextColor(Theme.getColor(Theme.key_chat_messagePanelHint, resourcesProvider));
        hint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        hint.setSingleLine(true);
        hint.setEllipsize(TextUtils.TruncateAt.END);
        field.addView(hint, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.START | Gravity.CENTER_VERTICAL, 48, 0, 48, 0));
        field.addView(icon(context, R.drawable.input_attach, iconColor), LayoutHelper.createFrame(44, 44, Gravity.END | Gravity.CENTER_VERTICAL));
        addView(field, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 44, Gravity.BOTTOM, 10, 0, 62, 14));

        ImageView mic = icon(context, R.drawable.input_mic, iconColor);
        BlurredBackgroundDrawable micGlass = factory.create(mic, colors);
        micGlass.setRadius(dp(22));
        mic.setBackground(micGlass);
        addView(mic, LayoutHelper.createFrame(44, 44, Gravity.BOTTOM | Gravity.END, 0, 0, 10, 14));
    }

    /** Cherrygram's set-up for glass outside a chat: the blur of the drawn wallpaper where the device has it. */
    private BlurredBackgroundDrawableViewFactory createFactory(Theme.ResourcesProvider resourcesProvider) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            GlassSourceWrapped wallpaper = new GlassSourceWrapped();
            GlassSourceRenderNode source = new GlassSourceRenderNode(wallpaper);
            source.setUnderSource(wallpaper);
            BlurredBackgroundDrawableViewFactory factory = new BlurredBackgroundDrawableViewFactory(source);
            factory.setLiquidGlassEffectAllowed(LiteMode.isEnabled(LiteMode.FLAG_LIQUID_GLASS));
            factory.setSourceRootView(new ViewPositionWatcher(this), this);
            return factory;
        }
        GlassSourceColor color = new GlassSourceColor();
        color.setColor(Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider));
        return new BlurredBackgroundDrawableViewFactory(color);
    }

    private static ImageView icon(Context context, int res, int color) {
        ImageView view = new ImageView(context);
        view.setScaleType(ImageView.ScaleType.CENTER);
        view.setImageResource(res);
        view.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
        return view;
    }

    /** A copy of this account's user: the shared object must not lose its self flag for the sake of a preview. */
    private static TLRPC.User previewUser(int account) {
        TLRPC.TL_user user = new TLRPC.TL_user();
        TLRPC.User self = UserConfig.getInstance(account).getCurrentUser();
        if (self != null) {
            user.id = self.id;
            user.first_name = self.first_name;
            user.last_name = self.last_name;
            user.photo = self.photo;
        }
        return user;
    }

    private static CharSequence previewTitle(TLRPC.User user) {
        if (!TextUtils.isEmpty(user.first_name) || !TextUtils.isEmpty(user.last_name)) {
            return ContactsController.formatName(user.first_name, user.last_name);
        }
        return LocaleController.getString(R.string.AppName);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(dp(HEIGHT_DP), MeasureSpec.EXACTLY));
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        Drawable drawable = Theme.getCachedWallpaperNonBlocking();
        if (Theme.wallpaperLoadTask != null) {
            invalidate();
        }
        if (drawable != backgroundDrawable) {
            disposeGradient();
            backgroundDrawable = drawable;
        }
        if (drawable == null) {
            canvas.drawColor(Theme.getColor(Theme.key_chat_wallpaper));
        } else if (drawable instanceof ColorDrawable || drawable instanceof GradientDrawable || drawable instanceof MotionBackgroundDrawable) {
            drawable.setAlpha(255);
            drawable.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
            if (drawable instanceof BackgroundGradientDrawable) {
                disposeGradient();
                backgroundGradientDisposable = ((BackgroundGradientDrawable) drawable).drawExactBoundsSize(canvas, this);
            } else {
                drawable.draw(canvas);
            }
        } else if (drawable instanceof BitmapDrawable && drawable.getIntrinsicWidth() > 0 && drawable.getIntrinsicHeight() > 0) {
            drawable.setAlpha(255);
            ((BitmapDrawable) drawable).setFilterBitmap(true);
            float scale = Math.max((float) getMeasuredWidth() / drawable.getIntrinsicWidth(), (float) getMeasuredHeight() / drawable.getIntrinsicHeight());
            int width = (int) Math.ceil(drawable.getIntrinsicWidth() * scale);
            int height = (int) Math.ceil(drawable.getIntrinsicHeight() * scale);
            int x = (getMeasuredWidth() - width) / 2;
            int y = (getMeasuredHeight() - height) / 2;
            drawable.setBounds(x, y, x + width, y + height);
            drawable.draw(canvas);
        } else {
            drawable.setAlpha(255);
            StoryEntry.drawBackgroundDrawable(canvas, drawable, getWidth(), getHeight());
        }
        super.onDraw(canvas);
    }

    private void disposeGradient() {
        if (backgroundGradientDisposable != null) {
            backgroundGradientDisposable.dispose();
            backgroundGradientDisposable = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        disposeGradient();
        super.onDetachedFromWindow();
    }
}
