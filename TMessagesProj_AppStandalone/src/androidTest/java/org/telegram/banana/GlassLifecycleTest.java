package org.telegram.banana;

import android.content.Context;
import android.graphics.Color;
import android.graphics.RenderNode;
import android.view.View;
import android.widget.FrameLayout;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;
import org.telegram.utils.glass.GlassCaptureSourceCanvas;
import org.telegram.utils.glass.GlassEngine;
import org.telegram.utils.glass.positions.GlassPositionsArray;
import org.telegram.utils.glass.source.GlassSourceColor;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.Assert.*;

public class GlassLifecycleTest {
    @Test
    public void replacingRootRetiresOldViewsButKeepsNewUnparentedAndSeparateRootViews() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            ApplicationLoader.postInitApplication();
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            GlassEngine engine = new GlassEngine();
            GlassSourceColor source = engine.createThemedColorSource(() -> Color.WHITE);
            FrameLayout current = new FrameLayout(context);
            View old = new View(context);
            current.addView(old);
            engine.registerDrawable(old, new BlurredBackgroundDrawable(source));
            View unparented = new View(context);
            engine.registerDrawable(unparented, new BlurredBackgroundDrawable(source));
            FrameLayout actionBarRoot = new FrameLayout(context);
            View actionBar = new View(context);
            actionBarRoot.addView(actionBar);
            engine.registerDrawable(actionBar, new BlurredBackgroundDrawable(source));
            engine.setRoot(current);
            current.removeView(old);

            View child = null;
            try {
                for (int i = 0; i < 10; i++) {
                    FrameLayout next = new FrameLayout(context);
                    child = new View(context);
                    // Factories can register a replacement before createView parents it.
                    engine.registerDrawable(child, new BlurredBackgroundDrawable(source));
                    engine.setRoot(next);
                    next.addView(child);
                    assertEquals("Only the new row and unrelated registrations may remain", 3,
                            list(engine, "viewPositionStates").size());
                    assertEquals("Constructor-owned color sources survive view replacement", 1,
                            list(engine, "themedSourceColorStates").size());
                    assertNull("The previous tree must no longer subscribe to window draws",
                            current.getTag(R.id.tag_view_on_post_draw_state));
                    current = next;
                }

                FrameLayout movedRoot = new FrameLayout(context);
                current.removeView(child);
                movedRoot.addView(child);
                engine.setRoot(movedRoot);
                assertEquals("A view moved to the new root must keep its registration", 3,
                        list(engine, "viewPositionStates").size());
            } finally {
                engine.setRoot(null);
            }
        });
    }

    @Test
    public void shrinkingCaptureAndReplacingRootDiscardRetiredNativeDisplayLists() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            GlassCaptureSourceCanvas capture = new GlassCaptureSourceCanvas();
            View root = new View(context);
            capture.setRootView(root);
            GlassPositionsArray two = new GlassPositionsArray();
            two.add(new android.graphics.RectF(0, 0, 40, 40));
            two.add(new android.graphics.RectF(0, 60, 40, 100));
            capture.setPositions(two);
            List<?> positions = list(capture, "positions");
            String[] nodeNames = {"renderNode", "renderNodeGlass", "renderNodeGlassFrosted",
                    "renderNodeFiltered", "renderNodeFrostedFiltered"};
            try {
                for (Object position : positions) {
                    for (String name : nodeNames) record((RenderNode) field(position, name));
                }
                RenderNode debug = (RenderNode) field(capture, "debugRenderNode");
                record(debug);
                GlassPositionsArray one = new GlassPositionsArray();
                one.add(new android.graphics.RectF(0, 0, 40, 40));
                capture.setPositions(one);
                for (String name : nodeNames) {
                    assertTrue("The active capture stays recorded", ((RenderNode) field(positions.get(0), name)).hasDisplayList());
                    assertFalse("Unused capture nodes must release their recorded resources",
                            ((RenderNode) field(positions.get(1), name)).hasDisplayList());
                }
                assertFalse(debug.hasDisplayList());

                capture.setRootView(root);
                assertTrue(((RenderNode) field(positions.get(0), "renderNode")).hasDisplayList());
                capture.setRootView(new View(context));
                for (Object position : positions) {
                    for (String name : nodeNames) {
                        assertFalse("A new root must not retain any old tree's display list",
                                ((RenderNode) field(position, name)).hasDisplayList());
                    }
                }
                capture.setPositions(two);
                record((RenderNode) field(positions.get(1), "renderNode"));
                assertTrue("A retired position can be recorded again", ((RenderNode) field(positions.get(1), "renderNode")).hasDisplayList());
            } finally {
                capture.setRootView(null);
            }
        });
    }

    private static void record(RenderNode node) {
        node.beginRecording(16, 16).drawColor(Color.WHITE);
        node.endRecording();
    }

    private static List<?> list(Object owner, String name) {
        return (List<?>) field(owner, name);
    }

    private static Object field(Object owner, String name) {
        try {
            Field field = owner.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(owner);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
