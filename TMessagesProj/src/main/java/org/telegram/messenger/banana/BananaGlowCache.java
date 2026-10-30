package org.telegram.messenger.banana;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.LruCache;

import org.telegram.messenger.AndroidUtilities;

/**
 * Avatar and reaction glow drawn from small cached bitmaps. A stroke with setShadowLayer made the
 * GPU blur the shadow again on every draw of every avatar while the chat list scrolled; the glow
 * looks the same for a given size, radius and colour, so it is rendered once in software.
 */
public final class BananaGlowCache {
    private static final int MAX_BYTES = 3 * 1024 * 1024;
    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(MAX_BYTES) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };
    private static final Paint BITMAP_PAINT = new Paint(Paint.FILTER_BITMAP_FLAG);

    private static final int MAX_CACHED_SIDE = 2048;

    private BananaGlowCache() {}

    /** Draws the glow of {@code glow} (a stroke with a shadow layer) around a rounded rectangle. */
    public static void draw(Canvas canvas, Paint glow, float left, float top, float right, float bottom, float radius) {
        int width = Math.round(right - left), height = Math.round(bottom - top);
        if (width <= 0 || height <= 0) return;
        int pad = AndroidUtilities.dp(12);
        // Avatars and reaction pills are small; anything this large is drawn directly, not cached.
        if (width > MAX_CACHED_SIDE || height > MAX_CACHED_SIDE) {
            canvas.drawRoundRect(left, top, right, bottom, radius, radius, glow);
            return;
        }
        String key = width + "x" + height + "r" + Math.round(radius) + "c" + glow.getColor() + "s" + glow.getStrokeWidth();
        Bitmap bitmap = CACHE.get(key);
        if (bitmap == null) {
            try {
                bitmap = Bitmap.createBitmap(width + pad * 2, height + pad * 2, Bitmap.Config.ARGB_8888);
            } catch (OutOfMemoryError | RuntimeException e) {
                canvas.drawRoundRect(left, top, right, bottom, radius, radius, glow);
                return;
            }
            new Canvas(bitmap).drawRoundRect(pad, pad, pad + width, pad + height, radius, radius, glow);
            CACHE.put(key, bitmap);
        }
        canvas.drawBitmap(bitmap, left - pad, top - pad, BITMAP_PAINT);
    }

    public static void clear() {
        CACHE.evictAll();
    }
}
