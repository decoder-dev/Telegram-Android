package org.telegram.ui.Components.blur3;

import android.graphics.Canvas;
import android.graphics.RecordingCanvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.os.Build;
import android.view.View;

import androidx.annotation.RequiresApi;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.MessageDrawable;
import org.telegram.ui.Components.chat.ViewPositionWatcher;
import org.telegram.utils.glass.source.GlassSource;

import java.util.Arrays;

/**
 * A cell-owned lens over the wallpaper only. It never captures the chat/cell itself,
 * allocates no bitmaps, and requests no extra frames. RenderNodes are not shared
 * between cells: recorded rows must not all sample the last row's wallpaper position.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public final class BananaBubbleBackdrop {
    private final RenderNode backdrop = new RenderNode("Banana bubble wallpaper");
    private final RenderNode lens = new RenderNode("Banana bubble lens");
    private LiquidGlassEffect effect;
    private final RectF position = new RectF();
    private final RectF body = new RectF();
    private final RectF recordedBody = new RectF();
    private final float[] radii = new float[4];
    private final float[] recordedRadii = new float[4];
    private float density = -1;
    // Keep cell-owned display lists while both their geometry and wallpaper content are unchanged.
    // Colour/bitmap sources are baked into them, so source identity alone is not enough.
    private float recordedLeft = Float.NaN, recordedTop = Float.NaN;
    private int recordedWidth = -1, recordedHeight = -1;
    private GlassSource recordedSource;
    private long recordedGeneration;
    private static final int DOWNSAMPLE = 3;

    public boolean draw(Canvas canvas, View cell, View root, GlassSource source,
                        MessageDrawable shape, MessageDrawable.PathDrawParams cache) {
        if (!canvas.isHardwareAccelerated() || source == null || root == null || root == cell
                || !ViewPositionWatcher.computeRectInParent(cell, root, position)) return false;
        Rect bounds = shape.getBounds();
        int width = bounds.width(), height = bounds.height();
        if (width <= 0 || height <= 0 || shape.getAlpha() == 0) return false;
        shape.getGlassGeometry(body, radii);
        if (body.isEmpty()) return false;
        if (density != AndroidUtilities.density) {
            density = AndroidUtilities.density;
            backdrop.setRenderEffect(RenderEffect.createBlurEffect(
                    AndroidUtilities.dpf2(6) / DOWNSAMPLE, AndroidUtilities.dpf2(6) / DOWNSAMPLE, Shader.TileMode.CLAMP));
            recordedWidth = -1;
        }
        float left = position.left + bounds.left, top = position.top + bounds.top;
        long generation = source.getGenerationId();
        // Re-recording every frame made HWUI recompute this bubble's blur and refraction each
        // frame, even when only a typing dot or an emoji elsewhere in the chat animated.
        boolean reuse = backdrop.hasDisplayList() && lens.hasDisplayList()
                && left == recordedLeft && top == recordedTop && width == recordedWidth && height == recordedHeight
                && source == recordedSource && generation == recordedGeneration
                && body.equals(recordedBody) && Arrays.equals(radii, recordedRadii);
        lens.setAlpha(shape.getAlpha() / 255f);
        if (!reuse) {
            int nodeWidth = (width + DOWNSAMPLE - 1) / DOWNSAMPLE;
            int nodeHeight = (height + DOWNSAMPLE - 1) / DOWNSAMPLE;
            backdrop.setPosition(0, 0, nodeWidth, nodeHeight);
            RecordingCanvas recording = backdrop.beginRecording(nodeWidth, nodeHeight);
            try {
                recording.scale(1f / DOWNSAMPLE, 1f / DOWNSAMPLE);
                recording.translate(-left, -top);
                source.draw(recording, left, top, left + width, top + height);
            } finally {
                backdrop.endRecording();
            }
            lens.setPosition(0, 0, nodeWidth, nodeHeight);
            float smallestRadius = Math.min(Math.min(radii[0], radii[1]), Math.min(radii[2], radii[3]));
            if (smallestRadius <= 0) {
                // A square corner has no lens thickness. Keep its blurred wallpaper and native
                // outline without asking the refraction shader to divide by a zero thickness.
                lens.setRenderEffect(null);
                effect = null;
            } else {
                if (effect == null) effect = new LiquidGlassEffect(lens);
                float thickness = Math.min(smallestRadius,
                        Math.min(AndroidUtilities.dpf2(9), Math.min(body.width(), body.height()) / 5f));
                effect.update(body.width(), body.height(), body.left, body.top, radii[0], radii[1], radii[2], radii[3],
                        thickness, 0.75f, 1.5f, 0, 0, 0, DOWNSAMPLE);
            }
            recording = lens.beginRecording(nodeWidth, nodeHeight);
            try {
                recording.drawRenderNode(backdrop);
            } finally {
                lens.endRecording();
            }
            recordedLeft = left;
            recordedTop = top;
            recordedWidth = width;
            recordedHeight = height;
            recordedSource = source;
            recordedGeneration = generation;
            recordedBody.set(body);
            System.arraycopy(radii, 0, recordedRadii, 0, radii.length);
        }
        int save = canvas.save();
        try {
            canvas.clipPath(shape.makePath(cache));
            canvas.translate(bounds.left, bounds.top);
            canvas.scale(DOWNSAMPLE, DOWNSAMPLE);
            canvas.drawRenderNode(lens);
        } finally {
            canvas.restoreToCount(save);
        }
        return true;
    }

    public void release() {
        backdrop.discardDisplayList();
        lens.discardDisplayList();
        lens.setRenderEffect(null);
        effect = null;
        recordedSource = null;
        recordedWidth = recordedHeight = -1;
    }
}
