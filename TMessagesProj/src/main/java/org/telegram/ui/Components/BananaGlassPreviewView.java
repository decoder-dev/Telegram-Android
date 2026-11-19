package org.telegram.ui.Components;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import org.telegram.messenger.ContactsController;
import org.telegram.messenger.LiteMode;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.banana.BananaConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory;
import org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundColorProvider;
import org.telegram.ui.Components.blur3.drawable.color.impl.BlurredBackgroundProviderImpl;
import org.telegram.ui.Components.chat.ChatInputViewsContainer;
import org.telegram.ui.Components.chat.ViewPositionWatcher;
import org.telegram.ui.Components.inset.WindowInsetsStateHolder;
import org.telegram.ui.Stories.recorder.StoryEntry;
import org.telegram.utils.glass.source.GlassSourceColor;
import org.telegram.utils.glass.source.GlassSourceRenderNode;
import org.telegram.utils.glass.source.GlassSourceWrapped;

/**
 * A chat header and message field in liquid glass over the current wallpaper, so the highlight style, the field
 * layout and the glass settings show as they are chosen (after Cherrygram's glare and input previews). It is a
 * picture only: touches go nowhere. Rebuild it when the field layout changes, since that is fixed at creation.
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

        // The real message field, as in Cherrygram's preview, so both field layouts look exactly as in a chat.
        SizeNotifierFrameLayout composerRoot = new SizeNotifierFrameLayout(context);
        composerRoot.setClipChildren(false);
        composerRoot.setClipToPadding(false);
        addView(composerRoot, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, COMPOSER_HEIGHT_DP, Gravity.BOTTOM));

        inputContainer = new ChatInputViewsContainer(context);
        inputContainer.setClipChildren(false);
        inputContainer.setWindowInsetsProvider(insets);
        inputContainer.setInputIslandBubbleDrawable(factory.create(inputContainer, colors));
        inputContainer.setUnderKeyboardBackgroundDrawable(factory.create(inputContainer, colors));
        final boolean split = BananaConfig.glassComposer();
        if (split) {
            inputContainer.setLeftBubbleDrawable(factory.create(inputContainer, colors));
            inputContainer.setRightBubbleDrawable(factory.create(inputContainer, colors));
        }
        composerRoot.addView(inputContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        FrameLayout bubble = inputContainer.getInputIslandBubbleContainer();
        bubble.setClipChildren(false);
        bubble.setClipToPadding(false);

        enterView = new ChatActivityEnterView(fragment.getParentActivity(), composerRoot, null, true, resourcesProvider, split) {
            @Override
            public boolean dispatchTouchEvent(MotionEvent ev) {
                return false;
            }
        };
        enterView.setChatInputViewsContainer(inputContainer);
        enterView.shouldDrawBackground = false;
        enterView.setAllowStickersAndGifs(true, true, true);
        enterView.setDialogId(UserConfig.getInstance(account).getClientUserId(), account);
        enterView.updateAudioVideoButton();
        enterView.setDelegate(new PreviewDelegate());
        enterView.setFocusable(false);
        enterView.setClickable(false);
        bubble.addView(enterView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM, 7, 0, 7, 1));
        if (enterView.messageEditText != null) {
            enterView.messageEditText.setText(null);
            enterView.messageEditText.setHintText(LocaleController.getString(R.string.TypeMessage));
            enterView.messageEditText.setFocusable(false);
            enterView.messageEditText.setClickable(false);
            enterView.messageEditText.setCursorVisible(false);
        }
        updateBubbles();
    }

    private static final int COMPOSER_HEIGHT_DP = 76;
    private final WindowInsetsStateHolder insets = new WindowInsetsStateHolder(this::onInsetsChanged);
    private final ChatInputViewsContainer inputContainer;
    private final ChatActivityEnterView enterView;

    private void onInsetsChanged() {
        if (inputContainer != null) inputContainer.checkInsets();
    }

    private void updateBubbles() {
        if (inputContainer == null || enterView == null) return;
        inputContainer.checkInsets();
        inputContainer.setInputBubbleHeight(dp(44));
        enterView.checkBubbles();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateBubbles();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) updateBubbles();
    }

    /** Nothing in the preview sends, records or opens anything. */
    private static final class PreviewDelegate implements ChatActivityEnterView.ChatActivityEnterViewDelegate {
        @Override public void onMessageSend(CharSequence message, boolean notify, int scheduleDate, int scheduleRepeatPeriod, long payStars) {}
        @Override public void needSendTyping() {}
        @Override public void onTextChanged(CharSequence text, boolean bigChange, boolean fromDraft) {}
        @Override public void onTextSelectionChanged(int start, int end) {}
        @Override public void onTextSpansChanged(CharSequence text) {}
        @Override public void onAttachButtonHidden() {}
        @Override public void onAttachButtonShow() {}
        @Override public void onWindowSizeChanged(int size) {}
        @Override public void onStickersTab(boolean opened) {}
        @Override public void onMessageEditEnd(boolean loading) {}
        @Override public void didPressAttachButton() {}
        @Override public void needStartRecordVideo(int state, boolean notify, int scheduleDate, int scheduleRepeatPeriod, int ttl, long effectId, long stars) {}
        @Override public void toggleVideoRecordingPause() {}
        @Override public boolean isVideoRecordingPaused() { return false; }
        @Override public void needChangeVideoPreviewState(int state, float seekProgress) {}
        @Override public void onSwitchRecordMode(boolean video) {}
        @Override public void onPreAudioVideoRecord() {}
        @Override public void needStartRecordAudio(int state) {}
        @Override public void needShowMediaBanHint() {}
        @Override public void onStickersExpandedChange() {}
        @Override public void onUpdateSlowModeButton(android.view.View button, boolean show, CharSequence time) {}
        @Override public void onSendLongClick() {}
        @Override public void onAudioVideoInterfaceUpdated() {}
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
