package org.telegram.utils.glass.source;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public final class GlassSourceBitmap implements GlassSource {
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Matrix bitmapMatrix = new Matrix();
    private @Nullable BitmapShader bitmapShader;
    private @Nullable Bitmap bitmap;
    private @Nullable Bitmap generationBitmap;
    private int bitmapGenerationId;
    private boolean generationBitmapRecycled;
    private final Matrix generationMatrix = new Matrix();
    private long generationId;

    public GlassSourceBitmap() {
        bitmapPaint.setFilterBitmap(true);
    }

    public void setBitmap(Bitmap bitmap) {
        if (this.bitmap == bitmap) {
            return;
        }

        this.bitmap = bitmap;
        generationBitmap = bitmap;
        generationBitmapRecycled = bitmap != null && bitmap.isRecycled();
        bitmapGenerationId = bitmap == null || generationBitmapRecycled ? 0 : bitmap.getGenerationId();
        generationId++;

        bitmapPaint.setShader(null);
        bitmapShader = null;

        if (bitmap != null) {
            bitmapShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            bitmapPaint.setShader(bitmapShader);
            updateMatrix();
        }
    }

    public void setMatrix(Matrix matrix) {
        bitmapMatrix.set(matrix);
    }

    public Matrix getMatrix() {
        return bitmapMatrix;
    }

    public @Nullable Bitmap getBitmap() {
        return bitmap;
    }

    @Override
    public long getGenerationId() {
        final boolean recycled = bitmap != null && bitmap.isRecycled();
        final int contentGeneration = bitmap == null || recycled ? 0 : bitmap.getGenerationId();
        // A wallpaper bitmap can be redrawn in place, and callers can update the returned matrix.
        // Both changes must invalidate captures even when setBitmap receives the same object.
        if (bitmap != generationBitmap || contentGeneration != bitmapGenerationId
                || recycled != generationBitmapRecycled || !bitmapMatrix.equals(generationMatrix)) {
            generationBitmap = bitmap;
            bitmapGenerationId = contentGeneration;
            generationBitmapRecycled = recycled;
            generationMatrix.set(bitmapMatrix);
            generationId++;
        }
        return generationId;
    }


    @Override
    public void draw(@NonNull Canvas canvas, float left, float top, float right, float bottom) {
        if (bitmap == null || bitmap.isRecycled() || bitmapShader == null) {
            return;
        }

        bitmapShader.setLocalMatrix(bitmapMatrix);

        canvas.drawRect(left, top, right, bottom, bitmapPaint);
    }



    private Bitmap bitmapInternal;

    public Canvas beginRecording(int width, int height) {
        return beginRecording(width, height, 1);
    }

    public Canvas beginRecording(int width, int height, float scale) {
        if (width <= 0 || height <= 0 || !Float.isFinite(scale) || scale <= 0) {
            throw new IllegalArgumentException("Recording requires positive dimensions and a finite positive scale");
        }
        final int bitmapWidth = Math.max(1, Math.round(width / scale));
        final int bitmapHeight = Math.max(1, Math.round(height / scale));
        if (bitmapInternal == null || bitmapInternal.isRecycled() || bitmapInternal.getWidth() != bitmapWidth || bitmapInternal.getHeight() != bitmapHeight) {
            bitmapInternal = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
        } else {
            bitmapInternal.eraseColor(0);
        }

        final Canvas canvas = new Canvas(bitmapInternal);
        canvas.scale((float) bitmapWidth / width, (float) bitmapHeight / height);
        return canvas;
    }

    public void endRecording() {
        setBitmap(bitmapInternal);
        bitmapInternal = null;
    }

    public boolean inRecording() {
        return bitmapInternal != null;
    }



    /* Utils For Wallpaper */

    protected int parentWidth, parentHeight, actionBarHeight;

    public final void setParentSize(int width, int height, int actionBarHeight) {
        if (this.parentWidth != width || this.parentHeight != height || this.actionBarHeight != actionBarHeight) {
            this.parentWidth = width;
            this.parentHeight = height;
            this.actionBarHeight = actionBarHeight;
            updateMatrix();
        }
    }

    private void updateMatrix() {
        if (bitmap == null) {
            bitmapMatrix.reset();
            return;
        }

        buildCenterCropMatrix(bitmapMatrix,
            bitmap.getWidth(), bitmap.getHeight(),
            parentWidth, parentHeight, actionBarHeight);
    }

    private static void buildCenterCropMatrix(
            Matrix m,
            int bitmapWidth, int bitmapHeight,
            int parentWidth, int parentHeight,
            int parentPaddingTop
    ) {
        m.reset();

        final int parentInternalWidth = parentWidth;
        final int parentInternalHeight = parentHeight - parentPaddingTop;
        if (bitmapWidth <= 0 || bitmapHeight <= 0 || parentInternalWidth <= 0 || parentInternalHeight <= 0) {
            return;
        }

        float scale = Math.max(
                (float) parentInternalWidth  / (float) bitmapWidth,
                (float) parentInternalHeight / (float) bitmapHeight
        );

        float scaledW = bitmapWidth  * scale;
        float scaledH = bitmapHeight * scale;

        float dx = (parentInternalWidth  - scaledW) * 0.5f;
        float dy = (parentInternalHeight - scaledH) * 0.5f;

        m.setScale(scale, scale);
        m.postTranslate(dx, dy + parentPaddingTop);
    }
}
