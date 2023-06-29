/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.provider.Settings.Secure;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import androidx.core.util.Pair;
import com.exteragram.messenger.utils.LocaleUtils;
import com.google.android.exoplayer2.util.Log;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.util.ArrayList;

import static org.telegram.messenger.Utilities.random;

public class AyuUtils {
    private static final char[] chars = "abcdefghijklmnopqrstuvwxyz1234567890".toCharArray();

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

    public static String removeExtension(String filename) {
        int index = filename.lastIndexOf('.');
        if (index == -1) { // no extension
            return filename;
        }

        return filename.substring(0, index);
    }

    public static String getExtension(String filename) {
        int index = filename.lastIndexOf('.');
        if (index == -1) { // no extension
            return "";
        }

        return filename.substring(index);
    }

    public static String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            char c = chars[random.nextInt(chars.length)];
            sb.append(c);
        }
        return sb.toString();
    }

    public static String getFilename(TLRPC.Message msg, File attachPathFile) {
        var filename = FileLoader.getDocumentFileName(msg.media.document);
        if (TextUtils.isEmpty(filename)) {
            filename = FileLoader.getMessageFileName(msg);
        }
        if (TextUtils.isEmpty(filename)) {
            filename = attachPathFile.getName();
        }

        var f = AyuUtils.removeExtension(filename);

        if (msg.media instanceof TLRPC.TL_messageMediaPhoto) {
            var photoSize = FileLoader.getClosestPhotoSizeWithSize(msg.media.photo.sizes, AndroidUtilities.getPhotoSize());
            f += "#" + photoSize.w + "x" + photoSize.h;
        }

        f += "@" + AyuUtils.generateRandomString(6);
        f += AyuUtils.getExtension(filename);

        return f;
    }

    public static String getReadableFilename(String name) {
        var ext = AyuUtils.getExtension(name);
        var index = name.lastIndexOf("@");
        if (index == -1) {
            return name;
        }

        return name.substring(0, index) + ext;
    }

    public static Pair<Integer, Integer> extractImageSizeFromName(String name) {
        var start = name.lastIndexOf("#") + 1;
        if (start == 0) {
            return null;
        }

        var end = name.lastIndexOf("@");
        if (end == -1) {
            return null;
        }

        try {
            var size = name.substring(start, end).split("x");
            var w = Integer.parseInt(size[0]);
            var h = Integer.parseInt(size[1]);

            return new Pair<>(w, h);
        } catch (Exception e) {
            Log.d("AyuGram", "extractImageSizeFromName fucked", e);
            return null;
        }
    }

    public static Pair<Integer, Integer> extractImageSizeFromFile(String path) {
        try {
            var options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);

            var w = options.outWidth;
            var h = options.outHeight;

            return new Pair<>(w, h);
        } catch (Exception e) {
            Log.d("AyuGram", "extractImageSizeFromFile fucked", e);
            return null;
        }
    }

    public static String getPackageName() {
        return ApplicationLoader.applicationContext.getPackageName();
    }

    @SuppressLint("HardwareIds")
    public static String getDeviceIdentifier() {
        return Secure.getString(ApplicationLoader.applicationContext.getContentResolver(), Secure.ANDROID_ID);
    }

    public static String getDeviceName() {
        return Build.MANUFACTURER + " " + Build.MODEL;
    }

    public static void killApplication(Activity activity) {
        activity.finishAndRemoveTask();
        int pid = android.os.Process.myPid();
        android.os.Process.killProcess(pid);
    }

    public static int getMinRealId(ArrayList<MessageObject> messages) {
        for (int i = messages.size() - 1; i > 0; i--) {
            var message = messages.get(i);
            if (message.getId() > 0 && !message.isSending()) {
                return message.getId();
            }
        }

        return Integer.MAX_VALUE; // no questions
    }

    public static TLRPC.Message copyMessage(TLRPC.Message oldMessage, long currentUserId) {
        try {
            NativeByteBuffer data = new NativeByteBuffer(oldMessage.getObjectSize());
            oldMessage.serializeToStream(data);
            data.rewind();

            TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
            message.send_state = oldMessage.send_state;
            message.id = oldMessage.id;
            if (message.id > 0 && message.send_state != 0 && message.send_state != 3) {
                message.send_state = 0;
            }
            message.out = oldMessage.out;
            message.unread = oldMessage.unread;
            message.readAttachPath(data, currentUserId);
            data.reuse();
            message.date = oldMessage.date;
            message.dialog_id = oldMessage.dialog_id;
            message.ttl = oldMessage.ttl;

            return message;
        } catch (Exception e) {
            Log.e("AyuGram", "failed to copy message", e);
        }

        return null;
    }

    public static CharSequence htmlToString(String text) {
        Spannable htmlParsed;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            htmlParsed = new SpannableString(Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY));
        } else {
            htmlParsed = new SpannableString(Html.fromHtml(text));
        }

        return LocaleUtils.formatWithURLs(htmlParsed);
    }

    public static boolean isMediaDownloadable(MessageObject message) {
        if (message == null || message.messageOwner == null || message.messageOwner.media == null) {
            return false;
        }

        if (message.messageOwner.media.photo instanceof TLRPC.TL_photoEmpty) {
            return false;
        }

        if (message.messageOwner.media.document instanceof TLRPC.TL_documentEmpty) {
            return false;
        }

        if (MessageObject.isMediaEmpty(message.messageOwner)) {
            return false;
        }

        return message.isSecretMedia() ||
                message.isGif() ||
                message.isNewGif() ||
                message.isRoundVideo() ||
                message.isVideo() ||
                message.isDocument() ||
                message.isMusic() ||
                message.isVoice();
    }
}
