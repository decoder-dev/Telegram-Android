/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.utils;

import android.text.TextUtils;

import org.telegram.tgnet.TLRPC;

import java.io.File;

public class AyuFakeMessageUtils {
    public static void fillMedia(TLRPC.TL_message message, String mediaPath, boolean isDocument, int date) {
        if (TextUtils.isEmpty(mediaPath)) {
            return;
        }

        message.attachPath = mediaPath;
        var file = new File(mediaPath);

        if (isDocument) {
            message.media = new TLRPC.TL_messageMediaDocument();
            message.media.flags |= 1;

            message.media.document = new TLRPC.TL_document();
            message.media.document.date = date;
            message.media.document.localPath = mediaPath;
            message.media.document.file_name = file.getName();
            message.media.document.size = file.length();
        } else {
            message.media = new TLRPC.TL_messageMediaPhoto();
            message.media.flags = 1;
            message.media.photo = new TLRPC.TL_photo();
            message.media.photo.has_stickers = false;
            message.media.photo.date = date;

            TLRPC.TL_photoSize photoSize = new TLRPC.TL_photoSize();
            photoSize.size = (int) file.length();
            photoSize.w = 500;
            photoSize.h = 302;
            photoSize.type = "y";
            photoSize.location = new AyuFileLocation(mediaPath);
            message.media.photo.sizes.add(photoSize);
        }
    }
}
