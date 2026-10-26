package com.exteragram.messenger.utils;

import android.content.Context;
import android.content.DialogInterface;

import org.telegram.ui.ActionBar.AlertDialog;

/**
 * Minimal stub for exteragram popup helpers.
 */
public final class PopupUtils {
    private PopupUtils() {}

    public static void showDialog(CharSequence[] items, CharSequence title, int selected, Context context, DialogInterface.OnClickListener listener) {
        if (context == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        if (title != null) {
            builder.setTitle(title);
        }
        builder.setItems(items, listener);
        builder.show();
    }

    public static void showDialog(CharSequence[] items, int[] icons, CharSequence title, int selected, Context context, DialogInterface.OnClickListener listener) {
        showDialog(items, title, selected, context, listener);
    }
}
