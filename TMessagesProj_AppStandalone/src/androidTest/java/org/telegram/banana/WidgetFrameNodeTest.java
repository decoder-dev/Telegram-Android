package org.telegram.banana;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

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
            Bitmap target = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888);
            try {
                Canvas recording = (Canvas) invoke(frame, "beginRecording", new Class<?>[0]);
                assertTrue(recording.isHardwareAccelerated());
                recording.drawColor(Color.RED);
                invoke(frame, "endRecording", new Class<?>[0]);
                assertTrue(frame.recorded());

                // RenderNode commands must survive the hardware-to-software copy, at the node's offset.
                frame.draw(new Canvas(target), 0.5f);
                Bitmap cached = (Bitmap) field(frame, "software");
                assertNotNull("A software draw must materialize the recorded frame", cached);
                assertEquals(Bitmap.Config.ARGB_8888, cached.getConfig());
                assertFalse(cached.isRecycled());
                assertEquals(0, target.getPixel(0, 0));
                assertEquals(128, Color.alpha(target.getPixel(5, 6)), 1);
                assertTrue(Color.red(target.getPixel(5, 6)) > 240);
                assertNotNull(field(frame, "softwarePaint"));

                // The copy is independent of the temporary hardware bitmap already released by render().
                target.eraseColor(Color.TRANSPARENT);
                frame.draw(new Canvas(target), 1f);
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
                invoke(frame, "discard", new Class<?>[0]);
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
