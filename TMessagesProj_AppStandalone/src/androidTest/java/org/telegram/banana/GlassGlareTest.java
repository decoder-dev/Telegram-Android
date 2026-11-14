package org.telegram.banana;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.banana.BananaConfig;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;
import org.telegram.ui.Components.blur3.drawable.GlassRim;
import org.telegram.utils.glass.source.GlassSourceColor;

import static org.junit.Assert.*;

public class GlassGlareTest {
    @Test
    public void settingIsGlobalAndInvalidValuesKeepTheDefaultAppearance() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            SharedPreferences prefs = MessagesController.getGlobalMainSettings();
            Object previous = prefs.getAll().get(BananaConfig.GLASS_GLARE_KEY);
            try {
                prefs.edit().remove(BananaConfig.GLASS_GLARE_KEY).apply();
                assertEquals(BananaConfig.GlassGlare.DEFAULT, BananaConfig.glassGlare());
                BananaConfig.setGlassGlare(BananaConfig.GlassGlare.OUTLINE);
                assertEquals(2, prefs.getInt(BananaConfig.GLASS_GLARE_KEY, -1));
                assertEquals(BananaConfig.GlassGlare.OUTLINE, BananaConfig.glassGlare());
                prefs.edit().putInt(BananaConfig.GLASS_GLARE_KEY, 100).apply();
                assertEquals(BananaConfig.GlassGlare.DEFAULT, BananaConfig.glassGlare());
                prefs.edit().putString(BananaConfig.GLASS_GLARE_KEY, "invalid").apply();
                assertEquals(BananaConfig.GlassGlare.DEFAULT, BananaConfig.glassGlare());
                BananaConfig.setGlassGlare(null);
                assertEquals(1, prefs.getInt(BananaConfig.GLASS_GLARE_KEY, -1));
            } finally {
                restore(prefs, previous);
            }
        });
    }

    @Test
    public void existingDrawableSwitchesOffAndToOutlineWithoutLosingItsCustomRim() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            ApplicationLoader.postInitApplication();
            SharedPreferences prefs = MessagesController.getGlobalMainSettings();
            Object previous = prefs.getAll().get(BananaConfig.GLASS_GLARE_KEY);
            Bitmap original = null, off = null, outline = null, restored = null;
            try {
                GlassSourceColor source = new GlassSourceColor();
                source.setColor(Color.TRANSPARENT);
                BlurredBackgroundDrawable drawable = new BlurredBackgroundDrawable(source).setRadius(12);
                drawable.setShadowParams(0, 0, 0);
                drawable.setBounds(8, 8, 72, 56);
                drawable.setRim(GlassRim.of(GlassRim.LIGHT_TOP, 2, Color.RED, 2, Color.BLUE, 0, 0));

                BananaConfig.setGlassGlare(BananaConfig.GlassGlare.DEFAULT);
                original = draw(drawable);
                assertTrue(nontransparent(original, 0, 0, 80, 64) > 0);
                assertEquals(0, nontransparent(original, 8, 26, 12, 38));

                BananaConfig.setGlassGlare(BananaConfig.GlassGlare.OFF);
                off = draw(drawable);
                assertEquals(0, nontransparent(off, 0, 0, 80, 64));

                BananaConfig.setGlassGlare(BananaConfig.GlassGlare.OUTLINE);
                outline = draw(drawable);
                assertTrue(nontransparent(outline, 8, 26, 12, 38) > 0);
                assertFalse(original.sameAs(outline));

                BananaConfig.setGlassGlare(BananaConfig.GlassGlare.DEFAULT);
                restored = draw(drawable);
                assertTrue(original.sameAs(restored));
            } finally {
                restore(prefs, previous);
                for (Bitmap bitmap : new Bitmap[]{original, off, outline, restored}) {
                    if (bitmap != null) bitmap.recycle();
                }
            }
        });
    }

    private static Bitmap draw(BlurredBackgroundDrawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(80, 64, Bitmap.Config.ARGB_8888);
        drawable.draw(new Canvas(bitmap));
        return bitmap;
    }

    private static int nontransparent(Bitmap bitmap, int left, int top, int right, int bottom) {
        int count = 0;
        for (int y = top; y < bottom; y++) {
            for (int x = left; x < right; x++) {
                if (Color.alpha(bitmap.getPixel(x, y)) != 0) count++;
            }
        }
        return count;
    }

    private static void restore(SharedPreferences prefs, Object previous) {
        SharedPreferences.Editor editor = prefs.edit().remove(BananaConfig.GLASS_GLARE_KEY);
        if (previous instanceof Integer) editor.putInt(BananaConfig.GLASS_GLARE_KEY, (Integer) previous);
        else if (previous instanceof String) editor.putString(BananaConfig.GLASS_GLARE_KEY, (String) previous);
        else if (previous instanceof Boolean) editor.putBoolean(BananaConfig.GLASS_GLARE_KEY, (Boolean) previous);
        else if (previous instanceof Float) editor.putFloat(BananaConfig.GLASS_GLARE_KEY, (Float) previous);
        else if (previous instanceof Long) editor.putLong(BananaConfig.GLASS_GLARE_KEY, (Long) previous);
        editor.apply();
    }
}
