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
        java.util.concurrent.atomic.AtomicInteger initialDepth = new java.util.concurrent.atomic.AtomicInteger();
        instrumentation.runOnMainSync(() -> initialDepth.set(activity.getActionBarLayout().getFragmentStack().size()));
        try {
            for (String language : new String[]{"en", "ru"}) {
                for (String theme : new String[]{"Blue", "Night"}) {
                    BananaSettingsActivity[] screen = new BananaSettingsActivity[1];
                    instrumentation.runOnMainSync(() -> {
                        BananaConfig.set(0, Option.CLEAN_TRACKING_LINKS, false);
                        BananaConfig.set(1, Option.CLEAN_TRACKING_LINKS, false);
                        LocaleController.getInstance().applyLanguage(LocaleController.getInstance().languagesDict.get(language), true, true, 0);
                        Theme.applyTheme(Theme.getTheme(theme));
                        screen[0] = new BananaSettingsActivity(0);
                        activity.presentFragment(screen[0]);
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
                    await(instrumentation, () -> BananaConfig.get(0, Option.CLEAN_TRACKING_LINKS), "The visible switch must change its preference");
                    SystemClock.sleep(300);
                    assertTrue("The visible setting must toggle account 0", BananaConfig.get(0, Option.CLEAN_TRACKING_LINKS));
                    assertFalse("Another account must keep its own setting", BananaConfig.get(1, Option.CLEAN_TRACKING_LINKS));
                    capture(instrumentation, language + "-" + theme);
                    instrumentation.runOnMainSync(() -> {
                        assertVisibleLabelsFit(screen[0]);
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
                    instrumentation.runOnMainSync(() -> assertVisibleLabelsFit(screen[0]));
                    SystemClock.sleep(400);
                    capture(instrumentation, language + "-" + theme + "-bottom");
                    instrumentation.runOnMainSync(() -> {
                        for (int i = 0; i < screen[0].listView.adapter.getItemCount(); i++) {
                            if (screen[0].listView.adapter.getItem(i).id == 1002) {
                                screen[0].listView.scrollToPosition(i);
                                break;
                            }
                        }
                    });
                    int[] historyButton = new int[2];
                    await(instrumentation, () -> {
                        for (int i = 0; i < screen[0].listView.adapter.getItemCount(); i++) {
                            if (screen[0].listView.adapter.getItem(i).id != 1002) continue;
                            View row = screen[0].listView.findViewByPosition(i);
                            if (row == null || !row.getGlobalVisibleRect(new Rect())) return false;
                            row.getLocationOnScreen(historyButton);
                            historyButton[0] += row.getWidth() / 2;
                            historyButton[1] += row.getHeight() / 2;
                            return true;
                        }
                        return false;
                    }, "History must have a reachable entry from Extras");
                    SystemClock.sleep(300);
                    tap(instrumentation, historyButton[0], historyButton[1]);
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
                    await(instrumentation, () -> activity.getActionBarLayout().getLastFragment() == screen[0] &&
                            !activity.getActionBarLayout().isTransitionAnimationInProgress(), "History must finish returning to Extras");
                    checkStoryPromptChoices(instrumentation, activity, language + "-" + theme);
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
    private static void assertVisibleLabelsFit(BananaSettingsActivity screen) {
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

    private static void capture(Instrumentation instrumentation, String name) throws Exception {
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
