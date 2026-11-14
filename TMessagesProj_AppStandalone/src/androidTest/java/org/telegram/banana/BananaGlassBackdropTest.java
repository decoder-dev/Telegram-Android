package org.telegram.banana;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RecordingCanvas;
import android.graphics.RectF;
import android.graphics.RenderNode;
import android.view.View;
import android.widget.FrameLayout;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LiteMode;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.banana.BananaConfig;
import org.telegram.ui.ActionBar.MessageDrawable;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.blur3.BananaBubbleBackdrop;
import org.telegram.utils.glass.source.GlassSource;
import org.telegram.utils.glass.source.GlassSourceBitmap;
import org.telegram.utils.glass.source.GlassSourceColor;
import org.telegram.utils.glass.source.GlassSourceCrossfade;
import org.telegram.utils.glass.source.GlassSourceWrapped;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.Assert.*;

public class BananaGlassBackdropTest {
    @Test
    public void rectangularRecordingPreservesAspectRatioAndScaledDrawingCoordinates() {
        GlassSourceBitmap source = new GlassSourceBitmap();
        Canvas recording = source.beginRecording(120, 160);
        recording.drawColor(Color.BLUE);
        source.endRecording();
        Bitmap full = source.getBitmap();
        assertEquals(120, full.getWidth());
        assertEquals(160, full.getHeight());
        assertFalse(source.inRecording());

        recording = source.beginRecording(120, 160, 2);
        recording.drawColor(Color.BLUE);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        recording.drawRect(60, 80, 120, 160, paint);
        source.endRecording();
        Bitmap scaled = source.getBitmap();
        try {
            assertEquals(60, scaled.getWidth());
            assertEquals(80, scaled.getHeight());
            assertEquals(Color.BLUE, scaled.getPixel(15, 20));
            assertEquals("The lower-right logical quadrant must survive downsampling", Color.RED, scaled.getPixel(45, 60));
        } finally {
            source.setBitmap(null);
            full.recycle();
            scaled.recycle();
        }
    }

    @Test
    public void generationsFollowBitmapContentCoordinatesAndNestedCompositeSources() {
        GlassSourceBitmap bitmapSource = new GlassSourceBitmap();
        Bitmap bitmap = Bitmap.createBitmap(4, 6, Bitmap.Config.ARGB_8888);
        try {
            bitmapSource.setBitmap(bitmap);
            bitmapSource.setParentSize(40, 60, 0);
            GlassSourceWrapped inner = new GlassSourceWrapped();
            inner.setSource(bitmapSource);
            GlassSourceWrapped outer = new GlassSourceWrapped();
            outer.setSource(inner);
            long generation = outer.getGenerationId();
            assertEquals(generation, outer.getGenerationId());
            bitmap.eraseColor(Color.RED);
            generation = changed(outer, generation);
            bitmapSource.getMatrix().postTranslate(3, 7);
            generation = changed(outer, generation);
            bitmapSource.setParentSize(80, 60, 0);
            generation = changed(outer, generation);
            Matrix replacement = new Matrix();
            replacement.setScale(2, 3);
            bitmapSource.setMatrix(replacement);
            generation = changed(outer, generation);

            GlassSourceColor from = new GlassSourceColor();
            from.setColor(Color.RED);
            GlassSourceColor to = new GlassSourceColor();
            to.setColor(Color.BLUE);
            GlassSourceCrossfade crossfade = new GlassSourceCrossfade(from, to);
            inner.setSource(crossfade);
            generation = changed(outer, generation);
            crossfade.setProgress(0.5f);
            generation = changed(outer, generation);
            to.setColor(Color.GREEN);
            generation = changed(outer, generation);
            inner.setSource(new GlassSourceCrossfade(from, to));
            changed(outer, generation);
        } finally {
            bitmapSource.setBitmap(null);
            bitmap.recycle();
        }
    }

    @Test
    public void backdropReusesUnchangedCapturesAndRefreshesContentPositionAndShape() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = prepare();
            FrameLayout root = new FrameLayout(context);
            View cell = new View(context);
            root.addView(cell);
            root.layout(0, 0, 600, 800);
            cell.layout(20, 30, 420, 330);
            MessageDrawable shape = shape();
            CountingSource source = new CountingSource();
            GlassSourceWrapped inner = new GlassSourceWrapped();
            inner.setSource(source);
            GlassSourceWrapped outer = new GlassSourceWrapped();
            outer.setSource(inner);
            BananaBubbleBackdrop renderer = new BananaBubbleBackdrop();
            RenderNode frame = new RenderNode("glass regression frame");
            int oldRadius = SharedConfig.bubbleRadius;
            try {
                SharedConfig.bubbleRadius = 18;
                draw(renderer, frame, cell, root, outer, shape);
                assertEquals(1, source.draws);
                assertEquals(67, node(renderer, "backdrop").getWidth());
                assertEquals(35, node(renderer, "backdrop").getHeight());
                assertArrayEquals("Capture coordinates must stay in wallpaper space after downsampling",
                        new float[]{0, 0, 67, 103f / 3}, source.mappedBounds, 0.01f);
                draw(renderer, frame, cell, root, outer, shape);
                assertEquals("An unrelated frame must reuse the existing capture", 1, source.draws);
                source.generation++;
                draw(renderer, frame, cell, root, outer, shape);
                assertEquals(2, source.draws);
                cell.setTranslationY(1.5f);
                draw(renderer, frame, cell, root, outer, shape);
                assertEquals(3, source.draws);
                shape.setRoundRadius(AndroidUtilities.dp(7));
                draw(renderer, frame, cell, root, outer, shape);
                assertEquals(4, source.draws);
                shape.setRoundRadius(0);
                shape.setTop(0, 600, 800, true, false);
                draw(renderer, frame, cell, root, outer, shape);
                assertEquals(5, source.draws);
                SharedConfig.bubbleRadius = 0;
                draw(renderer, frame, cell, root, outer, shape);
                assertNull("Square corners keep the blur without a zero-thickness refraction effect", field(renderer, "effect"));
                renderer.release();
                assertFalse(node(renderer, "backdrop").hasDisplayList());
                assertFalse(node(renderer, "lens").hasDisplayList());
            } finally {
                SharedConfig.bubbleRadius = oldRadius;
                renderer.release();
                frame.discardDisplayList();
            }
        });
    }

    @Test
    public void geometryUsesGroupedCornersAndPreservesTheNativeTailBody() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            prepare();
            int oldRadius = SharedConfig.bubbleRadius;
            try {
                SharedConfig.bubbleRadius = 18;
                MessageDrawable shape = shape();
                shape.setBounds(0, 0, AndroidUtilities.dp(200), AndroidUtilities.dp(120));
                RectF body = new RectF();
                float[] radii = new float[4];
                shape.setTop(0, 600, 800, true, false);
                shape.getGlassGeometry(body, radii);
                assertEquals(AndroidUtilities.dp(8), body.left, 0);
                assertEquals(AndroidUtilities.dp(200) - AndroidUtilities.dp(2), body.right, 0);
                assertEquals(AndroidUtilities.dp(6), radii[0], 0);
                assertEquals(AndroidUtilities.dp(18), radii[1], 0);
                shape.setBotButtonsBottom(true);
                shape.getGlassGeometry(body, radii);
                assertEquals(AndroidUtilities.dp(6), radii[2], 0);

                MessageDrawable media = new MessageDrawable(MessageDrawable.TYPE_MEDIA, true, false);
                media.setBounds(0, 0, AndroidUtilities.dp(200), AndroidUtilities.dp(120));
                media.setTop(0, 600, 800, false, false);
                media.getGlassGeometry(body, radii);
                assertEquals(AndroidUtilities.dp(18), radii[2], 0);
                media.setTop(0, 600, 800, false, true);
                media.getGlassGeometry(body, radii);
                assertEquals(AndroidUtilities.dp(6), radii[2], 0);
            } finally {
                SharedConfig.bubbleRadius = oldRadius;
            }
        });
    }

    @Test
    public void hidingACellReleasesItsCaptureAndCompactModeDoesNotRecreateIt() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = prepare();
            SharedPreferences accountPrefs = MessagesController.getMainSettings(0);
            SharedPreferences globalPrefs = MessagesController.getGlobalMainSettings();
            Map<String, ?> oldAccount = accountPrefs.getAll();
            Map<String, ?> oldGlobal = globalPrefs.getAll();
            int oldAccountId = UserConfig.selectedAccount;
            int oldFlags = LiteMode.getValue(true);
            int oldPowerSaver = LiteMode.getPowerSaverLevel();
            boolean[] compact = {false};
            ChatMessageCell cell = new ChatMessageCell(context, 0) {
                @Override public boolean isCompactMode() { return compact[0]; }
            };
            FrameLayout root = new FrameLayout(context);
            root.addView(cell);
            root.layout(0, 0, 600, 800);
            cell.layout(20, 30, 420, 330);
            cell.currentBackgroundDrawable = shape();
            CountingSource source = new CountingSource();
            cell.setDelegate(new ChatMessageCell.ChatMessageCellDelegate() {
                @Override public GlassSource getBananaWallpaperSource() { return source; }
                @Override public View getBananaWallpaperRoot() { return root; }
            });
            RenderNode frame = new RenderNode("glass cell regression");
            try {
                UserConfig.selectedAccount = 0;
                BananaConfig.set(0, BananaConfig.Option.GLASS_BUBBLES, true);
                BananaConfig.set(0, BananaConfig.Option.POWER_SAVING, false);
                LiteMode.setPowerSaverLevel(0);
                LiteMode.setAllFlags(oldFlags | LiteMode.FLAG_LIQUID_GLASS);
                assertTrue(LiteMode.isEnabled(LiteMode.FLAG_LIQUID_GLASS));
                drawCell(frame, cell);
                assertEquals("Positive control: an ordinary bubble captures wallpaper", 1, source.draws);
                BananaBubbleBackdrop renderer = (BananaBubbleBackdrop) cellField(cell, "bananaBubbleBackdrop");
                assertNotNull(renderer);
                cell.onVisibilityAggregated(false);
                assertNull(cellField(cell, "bananaBubbleBackdrop"));
                assertFalse(node(renderer, "backdrop").hasDisplayList());
                assertFalse(node(renderer, "lens").hasDisplayList());
                compact[0] = true;
                drawCell(frame, cell);
                assertEquals("Compact mode must not put the removed bubble back as a lens", 1, source.draws);
                assertNull(cellField(cell, "bananaBubbleBackdrop"));
            } finally {
                cell.onVisibilityAggregated(false);
                frame.discardDisplayList();
                LiteMode.setAllFlags(oldFlags);
                LiteMode.setPowerSaverLevel(oldPowerSaver);
                restoreBoolean(accountPrefs, oldAccount, BananaConfig.Option.GLASS_BUBBLES.key);
                restoreBoolean(accountPrefs, oldAccount, BananaConfig.Option.POWER_SAVING.key);
                restoreInt(globalPrefs, oldGlobal, "lite_mode6");
                restoreInt(globalPrefs, oldGlobal, "lite_mode_battery_level");
                UserConfig.selectedAccount = oldAccountId;
            }
        });
    }

    private static Context prepare() {
        ApplicationLoader.postInitApplication();
        assertFalse("Use a fresh emulator without a signed-in account", UserConfig.getInstance(0).isClientActivated());
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    private static MessageDrawable shape() {
        MessageDrawable shape = new MessageDrawable(MessageDrawable.TYPE_TEXT, false, false);
        shape.shadow = false;
        shape.overrideColor = Color.WHITE;
        shape.setBounds(7, 11, 208, 114);
        shape.setTop(0, 600, 800, false, false);
        return shape;
    }

    private static long changed(GlassSource source, long previous) {
        long current = source.getGenerationId();
        assertNotEquals(previous, current);
        assertEquals("No mutation must keep the same generation", current, source.getGenerationId());
        return current;
    }

    private static void draw(BananaBubbleBackdrop renderer, RenderNode frame, View cell,
                             View root, GlassSource source, MessageDrawable shape) {
        RecordingCanvas canvas = frame.beginRecording(600, 800);
        try {
            assertTrue(renderer.draw(canvas, cell, root, source, shape, new MessageDrawable.PathDrawParams()));
        } finally {
            frame.endRecording();
        }
    }

    private static void drawCell(RenderNode frame, ChatMessageCell cell) {
        RecordingCanvas canvas = frame.beginRecording(600, 800);
        try {
            Method draw = ChatMessageCell.class.getDeclaredMethod("drawBananaMessageBackground",
                    Canvas.class, MessageDrawable.PathDrawParams.class, Paint.class);
            draw.setAccessible(true);
            draw.invoke(cell, canvas, new MessageDrawable.PathDrawParams(), null);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        } finally {
            frame.endRecording();
        }
    }

    private static Object field(Object object, String name) {
        try {
            Field field = object.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(object);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }

    private static Object cellField(ChatMessageCell cell, String name) {
        try {
            Field field = ChatMessageCell.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(cell);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }

    private static RenderNode node(BananaBubbleBackdrop renderer, String name) {
        return (RenderNode) field(renderer, name);
    }

    private static void restoreBoolean(SharedPreferences prefs, Map<String, ?> old, String key) {
        SharedPreferences.Editor editor = prefs.edit().remove(key);
        if (old.get(key) instanceof Boolean) editor.putBoolean(key, (Boolean) old.get(key));
        editor.apply();
    }

    private static void restoreInt(SharedPreferences prefs, Map<String, ?> old, String key) {
        SharedPreferences.Editor editor = prefs.edit().remove(key);
        if (old.get(key) instanceof Integer) editor.putInt(key, (Integer) old.get(key));
        editor.apply();
    }

    private static final class CountingSource implements GlassSource {
        int draws;
        long generation;
        float[] mappedBounds;
        final Paint paint = new Paint();

        @Override public long getGenerationId() { return generation; }

        @Override public void draw(Canvas canvas, float left, float top, float right, float bottom) {
            draws++;
            Matrix matrix = new Matrix();
            canvas.getMatrix(matrix);
            mappedBounds = new float[]{left, top, right, bottom};
            matrix.mapPoints(mappedBounds);
            paint.setColor(Color.BLUE);
            canvas.drawRect(left, top, right, bottom, paint);
        }
    }
}
