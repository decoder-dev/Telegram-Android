/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram;

import com.google.android.exoplayer2.util.Log;

import java.io.File;

public class AyuUtils {
    public static boolean moveFile(File from, File to) {
        boolean success;
        try {
            success = from.renameTo(to);
        } catch (SecurityException e) {
            Log.d("AyuGram", e.toString());
            success = false;
        }

        return success;
    }
}
