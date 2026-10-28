package org.telegram.banana;

import android.app.Instrumentation;
import android.content.Intent;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.banana.BananaConfig;
import org.telegram.messenger.banana.BananaConfig.Option;
import org.telegram.messenger.banana.TrackingLinks;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.BananaSettingsActivity;
import org.telegram.ui.BananaHistoryActivity;
import org.telegram.ui.BananaExtrasSectionActivity;
import org.telegram.ui.BananaExtrasValuesActivity;
import org.telegram.ui.BananaFiltersActivity;
import org.telegram.ui.BananaFilterEditActivity;
import org.telegram.ui.Components.UniversalFragment;
import org.telegram.messenger.banana.FilterStore;
import org.telegram.messenger.banana.FilterRules;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.Components.VideoPlayer;
import org.telegram.ui.Components.StoryPrivacyPrompt;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Cells.BananaSwitchCell;
import org.telegram.ui.Cells.BananaValueCell;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.concurrent.atomic.AtomicReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

/** Runs without a signed-in user; never sends messages or changes a real account. */
public class BananaSmokeTest {
    @Test
    public void externalThemesCannotReplaceAnyAllowedPalette() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> {
            org.telegram.messenger.ApplicationLoader.postInitApplication();
            assertFalse("Use a fresh emulator", org.telegram.messenger.UserConfig.getInstance(0).isClientActivated());
            for (String name : new String[]{"Classic White", "Banana Cream", "Banana Graphite", "Classic Black"}) {
                Theme.ThemeInfo allowed = Theme.getTheme(name);
                assertTrue(Theme.isBananaTheme(allowed));
                Theme.applyTheme(allowed);
                assertSame(allowed, Theme.getActiveTheme());
                Theme.applyTheme(Theme.getTheme("Blue"));
                assertSame(allowed, Theme.getActiveTheme());
                Theme.ThemeInfo spoofed = new Theme.ThemeInfo(allowed);
                spoofed.name = name;
                spoofed.assetName = "night.attheme";
                assertFalse(Theme.isBananaTheme(spoofed));
                Theme.applyThemeTemporary(spoofed, false);
                assertSame(allowed, Theme.getActiveTheme());
                AtomicBoolean callback = new AtomicBoolean();
                Theme.applyThemeInBackground(spoofed, false, () -> callback.set(true));
                assertTrue(callback.get());
                assertSame(allowed, Theme.getActiveTheme());
                assertNull(Theme.applyThemeFile(new File("missing.attheme"), name, null, true));
                assertSame(allowed, Theme.getActiveTheme());
            }
            assertEquals(4, Theme.themes.size());
            for (Theme.ThemeInfo theme : Theme.themes) assertTrue(Theme.isBananaTheme(theme));
        });
    }

    @Test
    public void ghostPresetAndSilentSendingStayInsideTheirAccount() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> org.telegram.messenger.ApplicationLoader.postInitApplication());
        for (int account : new int[]{0, 1}) {
            assertFalse("Only use unregistered emulator accounts", org.telegram.messenger.UserConfig.getInstance(account).isClientActivated());
            BananaConfig.setGhostMode(account, false);
            BananaConfig.setSilentSending(account, BananaConfig.SilentSending.NORMAL);
        }
        try {
            BananaConfig.setSilentSending(0, BananaConfig.SilentSending.IN_GHOST_MODE);
            assertFalse(BananaConfig.shouldSendSilently(0));
            BananaConfig.setGhostMode(0, true);
            assertTrue(BananaConfig.isGhostModeActive(0));
            assertTrue(BananaConfig.shouldSendSilently(0));
            assertFalse(BananaConfig.isGhostModeActive(1));
            assertFalse(BananaConfig.shouldSendSilently(1));
            BananaConfig.set(0, Option.HIDE_TYPING, false);
            assertFalse("A custom partial preset is not full Ghost Mode", BananaConfig.isGhostModeActive(0));
            assertFalse(BananaConfig.shouldSendSilently(0));
            BananaConfig.setSilentSending(0, BananaConfig.SilentSending.ALWAYS);
            assertTrue(BananaConfig.shouldSendSilently(0));
            BananaConfig.setGhostMode(0, false);
            for (Option option : new Option[]{Option.HIDE_ONLINE, Option.HIDE_TYPING, Option.HIDE_RECORDING,
                    Option.HIDE_UPLOAD, Option.DONT_READ_MESSAGES, Option.DONT_READ_STORIES}) {
                assertFalse(BananaConfig.get(0, option));
            }
        } finally {
            for (int account : new int[]{0, 1}) {
                BananaConfig.setGhostMode(account, false);
                BananaConfig.setSilentSending(account, BananaConfig.SilentSending.NORMAL);
            }
        }
    }
    @Test
    public void fourTimesSpeedReachesTheActualPlaybackBackend() throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        File tone = new File(instrumentation.getTargetContext().getCacheDir(), "banana-smoke-tone.wav");
        final int samples = 16000 * 8;
        ByteBuffer wav = ByteBuffer.allocate(44 + samples * 2).order(ByteOrder.LITTLE_ENDIAN);
        wav.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(36 + samples * 2);
        wav.put("WAVEfmt ".getBytes(StandardCharsets.US_ASCII)).putInt(16).putShort((short) 1).putShort((short) 1);
        wav.putInt(16000).putInt(32000).putShort((short) 2).putShort((short) 16);
        wav.put("data".getBytes(StandardCharsets.US_ASCII)).putInt(samples * 2);
        for (int i = 0; i < samples; i++) wav.putShort((short) (1000 * Math.sin(i * 2 * Math.PI * 440 / 16000)));
        try (FileOutputStream output = new FileOutputStream(tone)) {
            output.write(wav.array());
        }
        VideoPlayer[] player = new VideoPlayer[1];
        AtomicReference<Exception> error = new AtomicReference<>();
        try {
            instrumentation.runOnMainSync(() -> {
                org.telegram.messenger.ApplicationLoader.postInitApplication();
                player[0] = new VideoPlayer(false, false);
                player[0].setDelegate(new VideoPlayer.VideoPlayerDelegate() {
                    @Override public void onStateChanged(boolean ready, int state) {}
                    @Override public void onError(VideoPlayer source, Exception exception) { error.set(exception); }
                    @Override public void onVideoSizeChanged(int width, int height, int rotation, float ratio) {}
                    @Override public void onRenderedFirstFrame() {}
                });
                player[0].preparePlayer(Uri.fromFile(tone), "other");
                player[0].setPlaybackSpeed(4f);
            });
            await(instrumentation, () -> error.get() != null || player[0].getPlaybackState() == androidx.media3.common.Player.STATE_READY,
                    "The real media backend must prepare the local fixture");
            assertNull(error.get());
            instrumentation.runOnMainSync(() -> {
                assertEquals(4f, player[0].getPlaybackSpeed(), 0.001f);
                player[0].play();
            });
            await(instrumentation, () -> error.get() != null || player[0].getCurrentPosition() > 500,
                    "The local media fixture must advance at the selected speed");
            assertNull(error.get());
        } finally {
            instrumentation.runOnMainSync(() -> { if (player[0] != null) player[0].releasePlayer(false); });
            assertTrue(!tone.exists() || tone.delete());
        }
    }

    @Test
    public void platformUrlDetectorPreservesAuthCodeAndRawValues() {
        String[][] cases = {
                {"Read https://example.org/?utm_source=x&id=A%2fb+1#😀", "Read https://example.org/?id=A%2fb+1#😀"},
                {"example.org/?utm_source=x&id=1", "example.org/?id=1"},
                {"https://user:password@example.org/?utm_source=x", "https://user:password@example.org/?utm_source=x"},
                {"https://example.org/?utm_source=x&sig=a", "https://example.org/?utm_source=x&sig=a"},
                {"(https://youtu.be/video?si=share&t=25).", "(https://youtu.be/video?t=25)."}
        };
        for (String[] pair : cases) assertEquals(pair[0], pair[1], TrackingLinks.clean(pair[0], null).text);
    }

    @Test
    public void settingsRenderAndToggleIndependentlyForEachAccount() throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Intent intent = new Intent(instrumentation.getTargetContext(), LaunchActivity.class)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        instrumentation.getTargetContext().startActivity(intent);
        LaunchActivity[] launched = new LaunchActivity[1];
        await(instrumentation, () -> {
            launched[0] = LaunchActivity.instance;
            return launched[0] != null && launched[0].getActionBarLayout() != null && launched[0].hasWindowFocus();
        }, "LaunchActivity must become visible");
        LaunchActivity activity = launched[0];
        await(instrumentation, () -> {
            org.telegram.ui.ActionBar.BaseFragment root = activity.getActionBarLayout().getLastFragment();
            return root != null && root.getFragmentView() != null && root.getFragmentView().isLaidOut() &&
                    !activity.getActionBarLayout().isTransitionAnimationInProgress();
        }, "The initial host must finish its navigation before Extras is opened");
        assertFalse("Use a fresh emulator, never a signed-in account", org.telegram.messenger.UserConfig.getInstance(0).isClientActivated());
        assertFalse("Use a fresh emulator, never a signed-in account", org.telegram.messenger.UserConfig.getInstance(1).isClientActivated());
        // Regression: first initialization used to add PiP through a null menuItem.
        // Recreate the singleton as well as reusing the same Activity.
        instrumentation.runOnMainSync(() -> {
            for (int i = 0; i < 2; i++) {
                org.telegram.ui.PhotoViewer viewer = org.telegram.ui.PhotoViewer.getInstance();
                try {
                    viewer.setParentActivity(activity);
                    viewer.setParentActivity(activity);
                    try {
                        java.lang.reflect.Field caption = org.telegram.ui.PhotoViewer.class.getDeclaredField("captionTextViewSwitcher");
                        java.lang.reflect.Field container = org.telegram.ui.PhotoViewer.class.getDeclaredField("containerView");
                        caption.setAccessible(true); container.setAccessible(true);
                        Object saved = caption.get(viewer);
                        View content = (View) container.get(viewer);
                        try {
                            caption.set(viewer, null);
                            content.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                                    View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY));
                        } finally { caption.set(viewer, saved); }
                    } catch (ReflectiveOperationException failed) { throw new AssertionError(failed); }
                } finally {
                    viewer.destroyPhotoViewer();
                }
            }
        });
        File galleryPhoto = new File(instrumentation.getTargetContext().getCacheDir(), "banana-gallery-fixture.png");
        Bitmap fixtureBitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
        fixtureBitmap.eraseColor(0xff9fc7e8);
        try (FileOutputStream output = new FileOutputStream(galleryPhoto)) {
            assertTrue(fixtureBitmap.compress(Bitmap.CompressFormat.PNG, 100, output));
        } finally { fixtureBitmap.recycle(); }
        org.telegram.ui.PhotoViewer viewer = org.telegram.ui.PhotoViewer.getInstance();
        try {
            java.lang.reflect.Field animation = org.telegram.ui.PhotoViewer.class.getDeclaredField("animationInProgress");
            animation.setAccessible(true);
            for (int repeat = 0; repeat < 2; repeat++) {
                instrumentation.runOnMainSync(() -> {
                    viewer.setParentActivity(activity);
                    java.util.ArrayList<Object> photos = new java.util.ArrayList<>();
                    photos.add(new org.telegram.messenger.MediaController.PhotoEntry(0, 1, 1, galleryPhoto.getAbsolutePath(), 0, false, 64, 64, galleryPhoto.length()));
                    assertTrue(viewer.openPhotoForSelect(photos, 0, 0, false, new org.telegram.ui.PhotoViewer.EmptyPhotoViewerProvider(), null));
                });
                await(instrumentation, () -> {
                    try { return viewer.isVisible() && viewer.centerImage.getBitmap() != null && animation.getInt(viewer) == 0; }
                    catch (IllegalAccessException error) { throw new AssertionError(error); }
                }, "Local gallery image must decode and finish opening");
                capture(instrumentation, "local-photo-" + repeat);
                instrumentation.runOnMainSync(() -> viewer.closePhoto(false, false));
                await(instrumentation, () -> !viewer.isVisible(), "Photo viewer must close before reopening");
            }
        } finally {
            instrumentation.runOnMainSync(viewer::destroyPhotoViewer);
            galleryPhoto.delete();
        }
        java.util.concurrent.atomic.AtomicInteger initialDepth = new java.util.concurrent.atomic.AtomicInteger();
        instrumentation.runOnMainSync(() -> initialDepth.set(activity.getActionBarLayout().getFragmentStack().size()));
        try {
            for (String language : new String[]{"en", "ru"}) {
                for (String theme : new String[]{"Classic White", "Banana Cream", "Banana Graphite", "Classic Black"}) {
                    BananaSettingsActivity[] screen = new BananaSettingsActivity[1];
                    instrumentation.runOnMainSync(() -> {
                        BananaConfig.setGhostMode(0, false);
                        BananaConfig.setGhostMode(1, false);
                        LocaleController.getInstance().applyLanguage(LocaleController.getInstance().languagesDict.get(language), true, true, 0);
                        Theme.applyTheme(Theme.getTheme(theme));
                        screen[0] = new BananaSettingsActivity(0);
                        // This is test-only injection over the unsigned Intro screen, not a user
                        // navigation route. Keep actual nested-screen transitions enabled below.
                        activity.getActionBarLayout().presentFragment(new org.telegram.ui.ActionBar.INavigationLayout.NavigationParams(screen[0]).setNoAnimation(true));
                    });
                    await(instrumentation, () -> screen[0].listView != null && screen[0].listView.isLaidOut() &&
                            screen[0].listView.findViewByPosition(1) != null && !activity.getActionBarLayout().isTransitionAnimationInProgress(), "Extras rows must be laid out");
                    SystemClock.sleep(400);
                    assertEquals(language.equals("ru") ? "Возможности BananaGram" : "BananaGram Extras",
                            LocaleController.getString(R.string.BananaExtras));
                    assertTrue("Footer text needs readable contrast in both themes",
                            androidx.core.graphics.ColorUtils.calculateContrast(
                                    screen[0].getThemedColor(Theme.key_windowBackgroundWhiteGrayText4),
                                    screen[0].getThemedColor(Theme.key_windowBackgroundGray)) >= 4.5);
                    assertPaletteContrast(Theme.key_windowBackgroundWhiteBlueHeader,
                            Theme.key_windowBackgroundWhite);
                    assertPaletteContrast(Theme.key_windowBackgroundWhiteGrayText2,
                            Theme.key_windowBackgroundWhite);
                    Rect bounds = new Rect();
                    instrumentation.runOnMainSync(() -> {
                        View row = screen[0].listView.findViewByPosition(1);
                        assertNotNull(row);
                        assertTrue(row.getGlobalVisibleRect(bounds));
                        int[] location = new int[2];
                        row.getLocationOnScreen(location);
                        bounds.set(location[0], location[1], location[0] + row.getWidth(), location[1] + row.getHeight());
                    });
                    capture(instrumentation, language + "-" + theme + "-before");
                    long now = SystemClock.uptimeMillis();
                    MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, bounds.centerX(), bounds.centerY(), 0);
                    instrumentation.sendPointerSync(down);
                    SystemClock.sleep(80);
                    MotionEvent up = MotionEvent.obtain(now, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, bounds.centerX(), bounds.centerY(), 0);
                    instrumentation.sendPointerSync(up);
                    down.recycle();
                    up.recycle();
                    await(instrumentation, () -> BananaConfig.isGhostModeActive(0), "The visible switch must change its preference");
                    SystemClock.sleep(300);
                    assertTrue("The visible setting must toggle account 0", BananaConfig.isGhostModeActive(0));
                    assertFalse("Another account must keep its own setting", BananaConfig.isGhostModeActive(1));
                    capture(instrumentation, language + "-" + theme);
                    assertEveryRowFits(instrumentation, screen[0]);
                    instrumentation.runOnMainSync(() -> {
                        screen[0].listView.scrollToPosition(screen[0].listView.adapter.getItemCount() - 1);
                    });
                    await(instrumentation, () -> {
                        View last = screen[0].listView.findViewByPosition(screen[0].listView.adapter.getItemCount() - 1);
                        if (last == null || screen[0].listView.canScrollVertically(1)) return false;
                        int[] xy = new int[2];
                        last.getLocationOnScreen(xy);
                        Rect visibleWindow = new Rect();
                        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(visibleWindow);
                        return xy[1] >= visibleWindow.top && xy[1] + last.getHeight() <= visibleWindow.bottom;

                    },
                            "The last options and footer must be reachable by scrolling");
                    SystemClock.sleep(400);
                    capture(instrumentation, language + "-" + theme + "-bottom");
                    tapRow(instrumentation, screen[0], 2000);
                    BananaExtrasSectionActivity[] ninja = new BananaExtrasSectionActivity[1];
                    await(instrumentation, () -> {
                        if (!(activity.getActionBarLayout().getLastFragment() instanceof BananaExtrasSectionActivity) ||
                                activity.getActionBarLayout().isTransitionAnimationInProgress()) return false;
                        ninja[0] = (BananaExtrasSectionActivity) activity.getActionBarLayout().getLastFragment();
                        return ninja[0].listView != null && ninja[0].listView.isLaidOut();
                    }, "Ninja options must open from Extras");
                    assertEveryRowFits(instrumentation, ninja[0]);
                    tapRow(instrumentation, ninja[0], 9001);
                    BananaHistoryActivity[] history = new BananaHistoryActivity[1];
                    await(instrumentation, () -> {
                        if (!(activity.getActionBarLayout().getLastFragment() instanceof BananaHistoryActivity) ||
                                activity.getActionBarLayout().isTransitionAnimationInProgress()) return false;
                        history[0] = (BananaHistoryActivity) activity.getActionBarLayout().getLastFragment();
                        return history[0].listView != null && history[0].listView.adapter.getItemCount() > 1 &&
                                LocaleController.getString(R.string.BananaHistoryEmpty).contentEquals(history[0].listView.adapter.getItem(1).text);
                    }, "The real history screen must show its localized empty state without an account");
                    SystemClock.sleep(400);
                    capture(instrumentation, language + "-" + theme + "-history");
                    instrumentation.runOnMainSync(() -> history[0].finishFragment(false));
                    await(instrumentation, () -> activity.getActionBarLayout().getLastFragment() == ninja[0] &&
                            !activity.getActionBarLayout().isTransitionAnimationInProgress(), "History must finish returning to Ninja options");
                    instrumentation.runOnMainSync(() -> ninja[0].finishFragment(false));
                    await(instrumentation, () -> activity.getActionBarLayout().getLastFragment() == screen[0] &&
                            !activity.getActionBarLayout().isTransitionAnimationInProgress(), "Ninja options must finish returning to Extras");
                    checkStoryPromptChoices(instrumentation, activity, language + "-" + theme);
                    checkFilters(instrumentation, activity, screen[0], language + "-" + theme);
                    for (int sectionIndex = 1; sectionIndex < org.telegram.messenger.banana.BananaExtrasCatalog.HUB_ORDER.length; sectionIndex++) {
                        tapRow(instrumentation, screen[0], 2000 + sectionIndex);
                        final int selectedSection = sectionIndex;
                        BananaExtrasSectionActivity[] sectionScreen = new BananaExtrasSectionActivity[1];
                        await(instrumentation, () -> {
                            if (!(activity.getActionBarLayout().getLastFragment() instanceof BananaExtrasSectionActivity) ||
                                    activity.getActionBarLayout().isTransitionAnimationInProgress()) return false;
                            sectionScreen[0] = (BananaExtrasSectionActivity) activity.getActionBarLayout().getLastFragment();
                            return sectionScreen[0].listView != null && sectionScreen[0].listView.isLaidOut();
                        }, "Extras category " + selectedSection + " must open");
                        assertEveryRowFits(instrumentation, sectionScreen[0]);
                        capture(instrumentation, language + "-" + theme + "-section-" + selectedSection);
                        instrumentation.runOnMainSync(() -> sectionScreen[0].finishFragment(false));
                        await(instrumentation, () -> activity.getActionBarLayout().getLastFragment() == screen[0] &&
                                !activity.getActionBarLayout().isTransitionAnimationInProgress(),
                                "Extras category must return to the hub");
                    }
                    tapRow(instrumentation, screen[0], 1003);
                    BananaExtrasValuesActivity[] valuesScreen = new BananaExtrasValuesActivity[1];
                    await(instrumentation, () -> {
                        if (!(activity.getActionBarLayout().getLastFragment() instanceof BananaExtrasValuesActivity) ||
                                activity.getActionBarLayout().isTransitionAnimationInProgress()) return false;
                        valuesScreen[0] = (BananaExtrasValuesActivity) activity.getActionBarLayout().getLastFragment();
                        return valuesScreen[0].listView != null && valuesScreen[0].listView.isLaidOut();
                    }, "Extras Values must open from the hub");
                    assertEveryRowFits(instrumentation, valuesScreen[0]);
                    capture(instrumentation, language + "-" + theme + "-values");
                    instrumentation.runOnMainSync(() -> valuesScreen[0].finishFragment(false));
                    await(instrumentation, () -> activity.getActionBarLayout().getLastFragment() == screen[0] &&
                            !activity.getActionBarLayout().isTransitionAnimationInProgress(), "Values must return to the Extras hub");
                    instrumentation.runOnMainSync(() -> screen[0].finishFragment(false));
                    await(instrumentation, () -> activity.getActionBarLayout().getFragmentStack().size() == initialDepth.get() &&
                            !activity.getActionBarLayout().isTransitionAnimationInProgress(), "Extras must finish returning to the original host");
                }
            }
        } finally {
            instrumentation.runOnMainSync(() -> {
                for (Option option : Option.values()) {
                    BananaConfig.set(0, option, false);
                    BananaConfig.set(1, option, false);
                }
                BananaConfig.setSilentSending(0, BananaConfig.SilentSending.NORMAL);
                BananaConfig.setSilentSending(1, BananaConfig.SilentSending.NORMAL);
                activity.finish();
            });
        }
    }

    private static void checkStoryPromptChoices(Instrumentation instrumentation, LaunchActivity activity, String name) throws Exception {
        int[] buttons = {DialogInterface.BUTTON_POSITIVE, DialogInterface.BUTTON_NEUTRAL, DialogInterface.BUTTON_NEGATIVE, 0};
        StoryPrivacyPrompt.Choice[] expected = {StoryPrivacyPrompt.Choice.ENABLE_GHOST, StoryPrivacyPrompt.Choice.OPEN_NORMALLY,
                StoryPrivacyPrompt.Choice.CANCEL, StoryPrivacyPrompt.Choice.CANCEL};
        for (int i = 0; i < buttons.length; i++) {
            await(instrumentation, activity::hasWindowFocus, "The host must regain focus before the next dialog");
            AtomicReference<StoryPrivacyPrompt.Choice> selected = new AtomicReference<>();
            java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
            AlertDialog[] dialog = new AlertDialog[1];
            instrumentation.runOnMainSync(() -> {
                dialog[0] = StoryPrivacyPrompt.create(activity, org.telegram.ui.Components.BananaTheme.COLORS, choice -> {
                    calls.incrementAndGet(); selected.set(choice);
                });
                dialog[0].show();
            });
            await(instrumentation, () -> dialog[0].isShowing() && dialog[0].getButton(DialogInterface.BUTTON_POSITIVE).getWidth() > 0,
                    "Story privacy choices must be visible");
            int[] point = new int[2];
            final int button = buttons[i];
            instrumentation.runOnMainSync(() -> {
                Rect[] bounds = new Rect[3];
                for (int j = 0; j < 3; j++) {
                    View view = dialog[0].getButton(buttons[j]);
                    int[] xy = new int[2]; view.getLocationOnScreen(xy);
                    bounds[j] = new Rect(xy[0], xy[1], xy[0] + view.getWidth(), xy[1] + view.getHeight());
                    assertTrue(view.isShown());
                    android.widget.TextView text = (android.widget.TextView) view;
                    assertNotNull(text.getLayout());
                    for (int line = 0; line < text.getLayout().getLineCount(); line++) {
                        assertEquals("A privacy choice must not be truncated", 0, text.getLayout().getEllipsisCount(line));
                    }
                }
                assertFalse("Privacy choices must not overlap", Rect.intersects(bounds[0], bounds[1]));
                assertFalse("Privacy choices must not overlap", Rect.intersects(bounds[0], bounds[2]));
                assertFalse("Privacy choices must not overlap", Rect.intersects(bounds[1], bounds[2]));
                if (button != 0) {
                    View view = dialog[0].getButton(button);
                    view.getLocationOnScreen(point); point[0] += view.getWidth() / 2; point[1] += view.getHeight() / 2;
                }
            });
            SystemClock.sleep(250);
            if (i == 0) capture(instrumentation, name + "-story-prompt");
            if (button == 0) instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
            else tap(instrumentation, point[0], point[1]);
            await(instrumentation, () -> selected.get() != null && !dialog[0].isShowing(), "The selected privacy choice must dismiss the dialog");
            assertEquals(expected[i], selected.get());
            assertEquals("Button plus dismissal must report only one choice", 1, calls.get());
        }
    }
    private static void tap(Instrumentation instrumentation, float x, float y) {
        long now = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0);
        instrumentation.sendPointerSync(down);
        SystemClock.sleep(80);
        MotionEvent up = MotionEvent.obtain(now, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, y, 0);
        instrumentation.sendPointerSync(up);
        down.recycle();
        up.recycle();
    }
    private static void assertVisibleLabelsFit(UniversalFragment screen) {
        for (int i = 0; i < screen.listView.getChildCount(); i++) {
            View child = screen.listView.getChildAt(i);
            if (child instanceof BananaValueCell) {
                BananaValueCell cell = (BananaValueCell) child;
                for (android.widget.TextView text : new android.widget.TextView[]{cell.getTextView(), cell.getValueTextView()}) {
                    assertNotNull(text.getLayout());
                    for (int line = 0; line < text.getLayout().getLineCount(); line++) {
                        assertEquals("A setting and its value must be fully readable", 0, text.getLayout().getEllipsisCount(line));
                    }
                }
                assertTrue("The setting title and value must not overlap", cell.getValueTextView().getTop() >= cell.getTextView().getBottom());
                assertTrue("The value must fit in its row", cell.getValueTextView().getBottom() <= child.getHeight());
            }
            if (!(child instanceof BananaSwitchCell)) continue;
            android.widget.TextView text = ((BananaSwitchCell) child).getTextView();
            assertNotNull(text.getLayout());
            for (int line = 0; line < text.getLayout().getLineCount(); line++) {
                assertEquals("An option label must be readable without truncation", 0, text.getLayout().getEllipsisCount(line));
            }
            assertTrue("A wrapped label must fit inside its row", child.getMeasuredHeight() >=
                    text.getLayout().getHeight() + org.telegram.messenger.AndroidUtilities.dp(16));
        }
    }

    private static void assertPaletteContrast(int foregroundKey, int backgroundKey) {
        int foreground = org.telegram.ui.Components.BananaTheme.forExtras().getColor(foregroundKey);
        int background = org.telegram.ui.Components.BananaTheme.forExtras().getColor(backgroundKey);
        assertTrue("Every allowed palette must keep labels readable at WCAG AA contrast",
                androidx.core.graphics.ColorUtils.calculateContrast(foreground, background) >= 4.5);
    }

    private static void assertEveryRowFits(Instrumentation instrumentation, UniversalFragment screen) {
        int itemCount = screen.listView.adapter.getItemCount();
        int firstUnchecked = 0;
        while (firstUnchecked < itemCount) {
            final int requestedPosition = firstUnchecked;
            instrumentation.runOnMainSync(() -> screen.listView.scrollToPosition(requestedPosition));
            await(instrumentation, () -> !screen.listView.isComputingLayout()
                            && screen.listView.findViewByPosition(requestedPosition) != null,
                    "Extras row " + requestedPosition + " must be laid out while checking long labels");
            int[] nextPosition = {firstUnchecked};
            instrumentation.runOnMainSync(() -> {
                assertVisibleLabelsFit(screen);
                androidx.recyclerview.widget.LinearLayoutManager layoutManager =
                        (androidx.recyclerview.widget.LinearLayoutManager) screen.listView.getLayoutManager();
                nextPosition[0] = layoutManager.findLastVisibleItemPosition() + 1;
            });
            assertTrue("Scrolling the Extras list must make progress", nextPosition[0] > firstUnchecked);
            firstUnchecked = nextPosition[0];
        }
    }

    private static void tapRow(Instrumentation instrumentation, UniversalFragment screen, int id) {
        instrumentation.waitForIdleSync();
        SystemClock.sleep(200);
        instrumentation.runOnMainSync(() -> {
            for (int i = 0; i < screen.listView.adapter.getItemCount(); i++) if (screen.listView.adapter.getItem(i).id == id) {
                screen.listView.scrollToPosition(i); return;
            }
            fail("Missing row " + id);
        });
        instrumentation.waitForIdleSync();
        SystemClock.sleep(200);
        int[] xy = new int[2];
        await(instrumentation, () -> {
            if (screen.listView.isComputingLayout() || screen.listView.getScrollState() != androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE ||
                    screen.listView.getItemAnimator() != null && screen.listView.getItemAnimator().isRunning()) return false;
            for (int i = 0; i < screen.listView.adapter.getItemCount(); i++) if (screen.listView.adapter.getItem(i).id == id) {
                View view = screen.listView.findViewByPosition(i);
                Rect visible = new Rect();
                if (view == null || !view.getGlobalVisibleRect(visible) || visible.height() < view.getHeight()) {
                    // Imports may insert rows after the first position lookup. Resolve by ID again.
                    screen.listView.scrollToPosition(i);
                    return false;
                }
                view.getLocationOnScreen(xy);
                xy[0] += view.getWidth() / 2; xy[1] += view.getHeight() / 2; return true;
            }
            return false;
        }, "Row " + id + " must be completely reachable");
        tap(instrumentation, xy[0], xy[1]);
    }
    private static void checkFilters(Instrumentation instrumentation, LaunchActivity activity, BananaSettingsActivity extras, String name) throws Exception {
        FilterStore store = FilterStore.get(0);
        instrumentation.runOnMainSync(() -> { store.save(FilterRules.empty()); store.setEnabled(false); store.setSharedInChats(false); });
        tapRow(instrumentation, extras, 2000);
        BananaExtrasSectionActivity[] ninja = new BananaExtrasSectionActivity[1];
        await(instrumentation, () -> {
            if (!(activity.getActionBarLayout().getLastFragment() instanceof BananaExtrasSectionActivity) || activity.getActionBarLayout().isTransitionAnimationInProgress()) return false;
            ninja[0] = (BananaExtrasSectionActivity) activity.getActionBarLayout().getLastFragment();
            return ninja[0].listView != null && ninja[0].listView.isLaidOut();
        }, "Ninja options must open filters");
        tapRow(instrumentation, ninja[0], 9002);
        BananaFiltersActivity[] screen = new BananaFiltersActivity[1];
        await(instrumentation, () -> {
            if (!(activity.getActionBarLayout().getLastFragment() instanceof BananaFiltersActivity) || activity.getActionBarLayout().isTransitionAnimationInProgress()) return false;
            screen[0] = (BananaFiltersActivity) activity.getActionBarLayout().getLastFragment();
            return screen[0].listView != null && screen[0].listView.isLaidOut();
        }, "Filters must open from Extras");
        instrumentation.runOnMainSync(() -> assertVisibleLabelsFit(screen[0]));
        capture(instrumentation, name + "-filters");
        tapRow(instrumentation, screen[0], 1);
        await(instrumentation, store::enabled, "The physical filter switch must update its account setting");
        assertFalse("Enabling filters must not change their scope", store.sharedInChats());
        assertFalse("Filtering is account-scoped", FilterStore.get(1).enabled());
        tapRow(instrumentation, screen[0], 3);
        BananaFilterEditActivity[] editor = new BananaFilterEditActivity[1];
        await(instrumentation, () -> {
            if (!(activity.getActionBarLayout().getLastFragment() instanceof BananaFilterEditActivity) || activity.getActionBarLayout().isTransitionAnimationInProgress()) return false;
            editor[0] = (BananaFilterEditActivity) activity.getActionBarLayout().getLastFragment();
            return editor[0].listView != null && editor[0].listView.isLaidOut();
        }, "The native rule editor must open");
        instrumentation.runOnMainSync(() -> {
            for (int i = 0; i < editor[0].listView.adapter.getItemCount(); i++) {
                org.telegram.ui.Components.UItem item = editor[0].listView.adapter.getItem(i);
                if (item.id == 10) ((android.widget.EditText) item.view).setText("(?i)hello");
                if (item.id == 11) ((android.widget.EditText) item.view).setText("HELLO");
            }
            assertVisibleLabelsFit(editor[0]);
            for (int key : new int[]{Theme.key_windowBackgroundWhiteHintText, Theme.key_windowBackgroundWhiteBlueHeader}) {
                assertTrue("Editor hints and headers must remain readable", androidx.core.graphics.ColorUtils.calculateContrast(
                        editor[0].getThemedColor(key), editor[0].getThemedColor(Theme.key_windowBackgroundWhite)) >= 4.5);
            }
        });
        capture(instrumentation, name + "-filter-editor");
        View[] previousView = new View[1];
        instrumentation.runOnMainSync(() -> {
            previousView[0] = editor[0].getFragmentView();
            activity.rebuildAllFragments(true);
        });
        await(instrumentation, () -> editor[0].getFragmentView() != null && editor[0].getFragmentView() != previousView[0] &&
                editor[0].getFragmentView().isLaidOut() && !activity.getActionBarLayout().isTransitionAnimationInProgress(),
                "A native view rebuild must recreate the editor");
        instrumentation.runOnMainSync(() -> {
            for (int i = 0; i < editor[0].listView.adapter.getItemCount(); i++) {
                org.telegram.ui.Components.UItem item = editor[0].listView.adapter.getItem(i);
                if (item.id == 10) assertEquals("Draft expressions must survive view/theme rebuilds", "(?i)hello", ((android.widget.EditText) item.view).getText().toString());
                if (item.id == 11) assertEquals("Draft samples must survive view/theme rebuilds", "HELLO", ((android.widget.EditText) item.view).getText().toString());
            }
        });
        tapRow(instrumentation, editor[0], 4);
        try { await(instrumentation, () -> {
            for (int i = 0; i < editor[0].listView.adapter.getItemCount(); i++) if (LocaleController.getString(R.string.BananaFilterHidden)
                    .contentEquals(editor[0].listView.adapter.getItem(i).text == null ? "" : editor[0].listView.adapter.getItem(i).text)) return true;
            return false;
        }, "The real isolated rule check must update its native preview"); }
        catch (AssertionError failure) {
            String[] detail = {""};
            instrumentation.runOnMainSync(() -> {
                try {
                    java.lang.reflect.Field status = BananaFilterEditActivity.class.getDeclaredField("lastResultStatus"); status.setAccessible(true);
                    detail[0] = "; checker status=" + status.getInt(editor[0]);
                } catch (Exception ignored) { }
                for (int i = 0; i < editor[0].listView.adapter.getItemCount(); i++) {
                    org.telegram.ui.Components.UItem item = editor[0].listView.adapter.getItem(i);
                    if (item.id == 10) detail[0] += "; field error=" + ((android.widget.EditText) item.view).getError();
                }
            });
            throw new AssertionError(failure.getMessage() + detail[0], failure);
        }
        capture(instrumentation, name + "-filter-preview");
        tapRow(instrumentation, editor[0], 5);
        await(instrumentation, () -> activity.getActionBarLayout().getLastFragment() == screen[0] && !activity.getActionBarLayout().isTransitionAnimationInProgress(), "Saving must return to the rule list");
        assertEquals(1, store.rules().rules.size());
        assertEquals("(?i)hello", store.rules().rules.get(0).text);
        assertFalse("Saving a shared rule must not silently enable it in private chats", store.sharedInChats());
        checkImportDialogs(instrumentation, screen[0], store, name);
        checkSelectionDraft(instrumentation, activity, screen[0], store, name);
        instrumentation.runOnMainSync(() -> { store.setEnabled(false); store.save(FilterRules.empty()); screen[0].finishFragment(false); });
        await(instrumentation, () -> activity.getActionBarLayout().getLastFragment() == ninja[0] && !activity.getActionBarLayout().isTransitionAnimationInProgress(), "Filters must return to Ninja options");
        instrumentation.runOnMainSync(() -> ninja[0].finishFragment(false));
        await(instrumentation, () -> activity.getActionBarLayout().getLastFragment() == extras && !activity.getActionBarLayout().isTransitionAnimationInProgress(), "Ninja options must return to Extras");
    }

    private static void checkSelectionDraft(Instrumentation instrumentation, LaunchActivity activity, BananaFiltersActivity screen, FilterStore store, String name) throws Exception {
        final String selected = ".*[sale]\\E Привет 👋";
        FilterRules before = store.rules();
        assertFalse("The selection flow starts with filtering disabled", store.enabled());
        Runnable show = () -> screen.showDialog(org.telegram.ui.Components.SelectionFilterPrompt.create(
                screen.getParentActivity(), screen.getResourceProvider(), 42, scope ->
                        screen.presentFragment(BananaFilterEditActivity.fromSelection(0, scope, selected))));
        instrumentation.runOnMainSync(show);
        AlertDialog cancelled = waitDialog(instrumentation, screen, null);
        capture(instrumentation, name + "-filter-selection-scope");
        tapDialogButton(instrumentation, cancelled, AlertDialog.BUTTON_NEGATIVE);
        assertSame("Cancel must not save or replace a rules snapshot", before, store.rules());
        instrumentation.runOnMainSync(show);
        tapDialogButton(instrumentation, waitDialog(instrumentation, screen, cancelled), AlertDialog.BUTTON_POSITIVE);
        BananaFilterEditActivity[] draft = new BananaFilterEditActivity[1];
        await(instrumentation, () -> {
            if (!(activity.getActionBarLayout().getLastFragment() instanceof BananaFilterEditActivity) || activity.getActionBarLayout().isTransitionAnimationInProgress()) return false;
            draft[0] = (BananaFilterEditActivity) activity.getActionBarLayout().getLastFragment();
            return draft[0].listView != null && draft[0].listView.isLaidOut();
        }, "Choosing this chat must open a reviewable native draft");
        instrumentation.runOnMainSync(() -> {
            for (int i = 0; i < draft[0].listView.adapter.getItemCount(); i++) {
                org.telegram.ui.Components.UItem item = draft[0].listView.adapter.getItem(i);
                if (item.id == 10) assertEquals(java.util.regex.Pattern.quote(selected), ((android.widget.EditText) item.view).getText().toString());
                if (item.id == 11) assertEquals(selected, ((android.widget.EditText) item.view).getText().toString());
            }
        });
        assertSame("Opening a draft must not save", before, store.rules());
        capture(instrumentation, name + "-filter-selection-draft");
        tapRow(instrumentation, draft[0], 4);
        await(instrumentation, () -> {
            for (int i = 0; i < draft[0].listView.adapter.getItemCount(); i++) if (LocaleController.getString(R.string.BananaFilterHidden)
                    .contentEquals(draft[0].listView.adapter.getItem(i).text == null ? "" : draft[0].listView.adapter.getItem(i).text)) return true;
            return false;
        }, "Android's isolated evaluator must match the literal selection, including its quote terminator");
        tapRow(instrumentation, draft[0], 5);
        await(instrumentation, () -> activity.getActionBarLayout().getLastFragment() == screen && !activity.getActionBarLayout().isTransitionAnimationInProgress(), "Saving the selection draft must return to filters");
        assertEquals(before.rules.size() + 1, store.rules().rules.size());
        FilterRules.Rule saved = store.rules().rules.get(store.rules().rules.size() - 1);
        assertEquals(42, saved.dialogId);
        assertEquals(java.util.regex.Pattern.quote(selected), saved.text);
        assertFalse("Saving must not enable filtering", store.enabled());
        FilterRules after = store.rules();
        instrumentation.runOnMainSync(show);
        tapDialogButton(instrumentation, waitDialog(instrumentation, screen, null), AlertDialog.BUTTON_NEUTRAL);
        await(instrumentation, () -> activity.getActionBarLayout().getLastFragment() instanceof BananaFilterEditActivity && !activity.getActionBarLayout().isTransitionAnimationInProgress(), "Shared scope must also open a draft");
        instrumentation.runOnMainSync(() -> {
            BananaFilterEditActivity shared = (BananaFilterEditActivity) activity.getActionBarLayout().getLastFragment();
            try {
                java.lang.reflect.Field scope = BananaFilterEditActivity.class.getDeclaredField("dialogId"); scope.setAccessible(true);
                assertEquals(0, scope.getLong(shared));
            } catch (Exception exception) { throw new AssertionError(exception); }
            shared.finishFragment(false);
        });
        await(instrumentation, () -> activity.getActionBarLayout().getLastFragment() == screen && !activity.getActionBarLayout().isTransitionAnimationInProgress(), "Cancelling a shared draft returns to filters");
        assertSame("Leaving a draft unsaved must not change the rules", after, store.rules());
        assertFalse(store.enabled());
    }

    private static AlertDialog waitDialog(Instrumentation instrumentation, UniversalFragment screen, AlertDialog previous) {
        AlertDialog[] result = new AlertDialog[1];
        await(instrumentation, () -> {
            if (!(screen.getVisibleDialog() instanceof AlertDialog) || screen.getVisibleDialog() == previous) return false;
            result[0] = (AlertDialog) screen.getVisibleDialog();
            return result[0].isShowing() && result[0].getWindow() != null && result[0].getWindow().getDecorView().isLaidOut();
        }, "The next native dialog must become visible");
        return result[0];
    }
    private static android.widget.EditText findInput(View view) {
        if (view instanceof android.widget.EditText) return (android.widget.EditText) view;
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                android.widget.EditText found = findInput(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }
    private static void enterDialog(Instrumentation instrumentation, AlertDialog dialog, String text) {
        instrumentation.runOnMainSync(() -> {
            android.widget.EditText input = findInput(dialog.getWindow().getDecorView());
            assertNotNull("The import dialog must have a real text field", input);
            input.setText(text);
        });
    }
    private static void tapDialogButton(Instrumentation instrumentation, AlertDialog dialog, int button) {
        instrumentation.waitForIdleSync();
        SystemClock.sleep(150);
        int[] xy = new int[2];
        await(instrumentation, () -> {
            View view = dialog.getButton(button);
            Rect visible = new Rect();
            if (view == null || !view.getGlobalVisibleRect(visible) || visible.height() < view.getHeight()) return false;
            view.getLocationOnScreen(xy); xy[0] += view.getWidth() / 2; xy[1] += view.getHeight() / 2;
            return true;
        }, "The native dialog action must be reachable");
        tap(instrumentation, xy[0], xy[1]);
    }
    private static void checkImportDialogs(Instrumentation instrumentation, BananaFiltersActivity screen, FilterStore store, String name) throws Exception {
        tapRow(instrumentation, screen, 1);
        await(instrumentation, () -> !store.enabled(), "Disable filters before testing import");
        FilterRules before = store.rules();
        String json = before.put(new FilterRules.Rule(java.util.UUID.randomUUID().toString(), 0, "extra-fixture", false, true, true)).export();
        for (boolean apply : new boolean[]{false, true}) {
            tapRow(instrumentation, screen, 4);
            AlertDialog input = waitDialog(instrumentation, screen, null);
            enterDialog(instrumentation, input, json);
            tapDialogButton(instrumentation, input, DialogInterface.BUTTON_POSITIVE);
            AlertDialog preview = waitDialog(instrumentation, screen, input);
            assertSame("Previewing must never mutate existing rules", before, store.rules());
            assertFalse("Import preview cannot enable filtering", store.enabled());
            if (!apply) capture(instrumentation, name + "-filter-import-preview");
            tapDialogButton(instrumentation, preview, apply ? DialogInterface.BUTTON_POSITIVE : DialogInterface.BUTTON_NEGATIVE);
            await(instrumentation, () -> screen.getVisibleDialog() == null, "The import decision must close its dialog");
            assertEquals(apply ? 2 : 1, store.rules().rules.size());
        }
        assertFalse("Applying a rule list cannot silently enable filtering", store.enabled());
        tapRow(instrumentation, screen, 5);
        AlertDialog exported = waitDialog(instrumentation, screen, null);
        capture(instrumentation, name + "-filter-export");
        instrumentation.runOnMainSync(() -> assertTrue(exported.getMessageTextView().getText().toString().contains("extra-fixture")));
        tapDialogButton(instrumentation, exported, DialogInterface.BUTTON_NEGATIVE);
        await(instrumentation, () -> screen.getVisibleDialog() == null, "Export must close without changing the clipboard or rules");
        tapRow(instrumentation, screen, 6);
        AlertDialog source = waitDialog(instrumentation, screen, null);
        enterDialog(instrumentation, source, "https://evil.invalid/rules.json");
        tapDialogButton(instrumentation, source, DialogInterface.BUTTON_POSITIVE);
        AlertDialog rejected = waitDialog(instrumentation, screen, source);
        instrumentation.runOnMainSync(() -> assertEquals(LocaleController.getString(R.string.BananaFiltersSourceError), rejected.getMessageTextView().getText().toString()));
        assertEquals("Unsupported sources must not change rules", 2, store.rules().rules.size());
        capture(instrumentation, name + "-filter-source-error");
        tapDialogButton(instrumentation, rejected, DialogInterface.BUTTON_POSITIVE);
        await(instrumentation, () -> screen.getVisibleDialog() == null, "Source errors must remain dismissible");
    }

    private static void capture(Instrumentation instrumentation, String name) throws Exception {
        instrumentation.waitForIdleSync();
        // Capture a rendered frame after native list/text changes, not the previous buffer.
        SystemClock.sleep(150);
        File directory = instrumentation.getTargetContext().getExternalFilesDir("banana-smoke");
        assertNotNull(directory);
        assertTrue(directory.isDirectory() || directory.mkdirs());
        Bitmap screenshot = instrumentation.getUiAutomation().takeScreenshot();
        assertNotNull(screenshot);
        try (FileOutputStream output = new FileOutputStream(new File(directory, name + ".png"))) {
            assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG, 100, output));
        } finally {
            screenshot.recycle();
        }
    }

    private static void await(Instrumentation instrumentation, BooleanSupplier condition, String message) {
        long deadline = SystemClock.uptimeMillis() + 15000;
        AtomicBoolean ready = new AtomicBoolean();
        while (SystemClock.uptimeMillis() < deadline) {
            instrumentation.runOnMainSync(() -> ready.set(condition.getAsBoolean()));
            if (ready.get()) return;
            SystemClock.sleep(50);
        }
        String[] state = {""};
        instrumentation.runOnMainSync(() -> {
            LaunchActivity activity = LaunchActivity.instance;
            if (activity != null && activity.getActionBarLayout() != null) {
                org.telegram.ui.ActionBar.BaseFragment last = activity.getActionBarLayout().getLastFragment();
                state[0] = "; last=" + (last == null ? "none" : last.getClass().getSimpleName()) +
                        "; depth=" + activity.getActionBarLayout().getFragmentStack().size() +
                        "; transition=" + activity.getActionBarLayout().isTransitionAnimationInProgress();
            }
        });
        try { capture(instrumentation, "failure-" + java.util.UUID.randomUUID()); }
        catch (Exception ignored) { }
        fail(message + state[0]);
    }

}
