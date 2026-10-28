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
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSource;
import org.telegram.ui.Components.chat.ViewPositionWatcher;

/**
 * A cell-owned lens over the wallpaper only. It never captures the chat/cell itself,
 * allocates no bitmaps, and requests no extra frames. RenderNodes are not shared
 * between cells: recorded rows must not all sample the last row's wallpaper position.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public final class BananaBubbleBackdrop {
    private final RenderNode backdrop = new RenderNode("Banana bubble wallpaper");
    private final RenderNode lens = new RenderNode("Banana bubble lens");
    private final LiquidGlassEffect effect = new LiquidGlassEffect(lens);
    private final RectF position = new RectF();
    private float density = -1;

    public boolean draw(Canvas canvas, View cell, View root, BlurredBackgroundSource source,
                        MessageDrawable shape, MessageDrawable.PathDrawParams cache) {
        if (!canvas.isHardwareAccelerated() || source == null || root == null || root == cell
                || !ViewPositionWatcher.computeRectInParent(cell, root, position)) return false;
        Rect bounds = shape.getBounds();
        int width = bounds.width(), height = bounds.height();
        if (width <= 0 || height <= 0 || shape.getAlpha() == 0) return false;
        if (density != AndroidUtilities.density) {
            density = AndroidUtilities.density;
            backdrop.setRenderEffect(RenderEffect.createBlurEffect(
                    AndroidUtilities.dpf2(6), AndroidUtilities.dpf2(6), Shader.TileMode.CLAMP));
        }
        float left = position.left + bounds.left, top = position.top + bounds.top;
        backdrop.setPosition(0, 0, width, height);
        RecordingCanvas recording = backdrop.beginRecording(width, height);
        try {
            recording.translate(-left, -top);
            source.draw(recording, left, top, left + width, top + height);
        } finally {
            backdrop.endRecording();
        }
        lens.setPosition(0, 0, width, height);
        lens.setAlpha(shape.getAlpha() / 255f);
        float radius = Math.min(AndroidUtilities.dpf2(18), Math.min(width, height) / 2f);
        effect.update(0, 0, width, height, radius, radius, radius, radius,
                Math.max(1, Math.min(AndroidUtilities.dpf2(9), Math.min(width, height) / 5f)),
                0.75f, 1.5f, 0);
        recording = lens.beginRecording(width, height);
        try {
            recording.drawRenderNode(backdrop);
        } finally {
            lens.endRecording();
        }
        int save = canvas.save();
        try {
            canvas.clipPath(shape.makePath(cache));
            canvas.translate(bounds.left, bounds.top);
            canvas.drawRenderNode(lens);
        } finally {
            canvas.restoreToCount(save);
        }
        return true;
    }

    public void release() {
        backdrop.discardDisplayList();
        lens.discardDisplayList();
    }
}
