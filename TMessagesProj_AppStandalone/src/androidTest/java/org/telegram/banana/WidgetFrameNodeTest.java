package org.telegram.banana;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RenderNode;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.telegram.utils.twidget.WidgetFrameNode;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class WidgetFrameNodeTest {
    @Test
    public void softwareSnapshotPreservesPixelsAndDiscardReleasesItsCache() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            WidgetFrameNode frame = (WidgetFrameNode) invoke(null, "create",
                    new Class<?>[]{String.class, int.class, int.class, int.class, int.class},
                    "widget-frame-test", 3, 4, 11, 12);
            WidgetFrameNode hardwareConsumer = (WidgetFrameNode) invoke(null, "create",
                    new Class<?>[]{String.class, int.class, int.class, int.class, int.class},
                    "widget-frame-consumer", 0, 0, 16, 16);
            Bitmap target = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888);
            Bitmap hardwareTarget = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888);
            try {
                Canvas recording = (Canvas) invoke(frame, "beginRecording", new Class<?>[0]);
                assertTrue(recording.isHardwareAccelerated());
                recording.drawColor(Color.RED);
                // The painter creates private child RenderNodes for CSS filters/backdrops.
                RenderNode nested = new RenderNode("widget-filter-test");
                nested.setPosition(5, 5, 7, 7);
                nested.beginRecording(2, 2).drawColor(Color.GREEN);
                nested.endRecording();
                recording.drawRenderNode(nested);
                invoke(frame, "endRecording", new Class<?>[0]);
                assertTrue(frame.recorded());
                RenderNode original = (RenderNode) field(frame, "node");
                original.setAlpha(0.25f);

                // RenderNode commands must survive the hardware-to-software copy, at the node's offset.
                frame.draw(new Canvas(target), 0.5f);
                assertTrue("A snapshot must leave the original frame recordable for hardware drawing", frame.recorded());
                assertEquals(0.25f, original.getAlpha(), 0f);
                Bitmap cached = (Bitmap) field(frame, "software");
                assertNotNull("A software draw must materialize the recorded frame", cached);
                assertEquals(Bitmap.Config.ARGB_8888, cached.getConfig());
                assertFalse(cached.isRecycled());
                assertEquals(Color.RED, cached.getPixel(2, 2));
                assertEquals(Color.GREEN, cached.getPixel(6, 6));
                assertEquals(0, target.getPixel(0, 0));
                assertEquals(128, Color.alpha(target.getPixel(5, 6)), 1);
                assertTrue(Color.red(target.getPixel(5, 6)) > 240);
                assertNotNull(field(frame, "softwarePaint"));

                // The copy is independent of the temporary hardware bitmap already released by render().
                target.eraseColor(Color.TRANSPARENT);
                frame.draw(new Canvas(target), 1f);
                assertEquals(Color.RED, target.getPixel(5, 6));
                assertEquals(Color.GREEN, target.getPixel(9, 10));

                Canvas hardwareRecording = (Canvas) invoke(hardwareConsumer, "beginRecording", new Class<?>[0]);
                assertTrue(hardwareRecording.isHardwareAccelerated());
                frame.draw(hardwareRecording, 0.75f);
                invoke(hardwareConsumer, "endRecording", new Class<?>[0]);
                assertEquals(0.75f, original.getAlpha(), 0f);
                hardwareConsumer.draw(new Canvas(hardwareTarget), 1f);
                assertEquals(191, Color.alpha(hardwareTarget.getPixel(5, 6)), 1);
                assertTrue(Color.red(hardwareTarget.getPixel(5, 6)) > 240);
                assertTrue(Color.green(hardwareTarget.getPixel(9, 10)) > 240);
                assertEquals(0.75f, original.getAlpha(), 0f);
                assertTrue(hardwareConsumer.recorded());

                // A containing renderer may also discard this frame's display list when it loses its last parent.
                assertTrue("Animation layers must remain available before the next draw restores their list", frame.recorded());
                target.eraseColor(Color.TRANSPARENT);
                frame.draw(new Canvas(target), 1f);
                assertTrue(frame.recorded());
                assertEquals(Color.RED, target.getPixel(5, 6));

                invoke(frame, "discard", new Class<?>[0]);
                assertFalse(frame.recorded());
                assertTrue("Discard owns and releases the software snapshot", cached.isRecycled());
                assertNull(field(frame, "software"));
                assertNull(field(frame, "softwarePaint"));
                invoke(frame, "discard", new Class<?>[0]);
                target.eraseColor(Color.TRANSPARENT);
                frame.draw(new Canvas(target), 1f);
                assertEquals(0, target.getPixel(5, 6));
            } finally {
                invoke(hardwareConsumer, "discard", new Class<?>[0]);
                invoke(frame, "discard", new Class<?>[0]);
                hardwareTarget.recycle();
                target.recycle();
            }
        });
    }

    private static Object invoke(WidgetFrameNode frame, String name, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = WidgetFrameNode.class.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return method.invoke(frame, args);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }

    private static Object field(WidgetFrameNode frame, String name) {
        try {
            Field field = frame.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(frame);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }
}
