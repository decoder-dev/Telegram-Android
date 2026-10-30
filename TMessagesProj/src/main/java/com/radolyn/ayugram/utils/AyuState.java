/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.utils;

import android.util.LongSparseArray;
import com.radolyn.ayugram.AyuConfig;

import java.util.ArrayList;

public class AyuState {
    private static final AyuStateVariable allowReadPacket = new AyuStateVariable();
    private static final AyuStateVariable automaticallyScheduled = new AyuStateVariable();
    private static final AyuStateVariable hideSelection = new AyuStateVariable();
    private static final LongSparseArray<ArrayList<Integer>> deletePermitted = new LongSparseArray<>();

    public static void setAllowReadPacket(boolean val, int resetAfter) {
        allowReadPacket.set(val, resetAfter);
    }

    public static boolean getAllowReadPacket() {
        return AyuConfig.sendReadPackets || allowReadPacket.process();
    }

    // Reads the client sends on purpose in ghost mode. Keyed by the request itself: the one-shot
    // flag above is shared, so two reads issued close together (two reactions in two chats) let the
    // first one consume it and silently dropped the second.
    private static final java.util.Set<Object> allowedReadRequests =
            java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    public static void allowReadRequest(Object request) {
        synchronized (allowedReadRequests) {
            allowedReadRequests.add(request);
        }
    }

    public static boolean consumeAllowedReadRequest(Object request) {
        synchronized (allowedReadRequests) {
            return allowedReadRequests.remove(request);
        }
    }

    public static void setAutomaticallyScheduled(boolean val, int resetAfter) {
        automaticallyScheduled.set(val, resetAfter);
    }

    public static boolean getAutomaticallyScheduled() {
        return automaticallyScheduled.process();
    }

    public static void setHideSelection(boolean val, int resetAfter) {
        hideSelection.set(val, resetAfter);
    }

    public static boolean getHideSelection() {
        return hideSelection.process();
    }

    public static synchronized void permitDeleteMessage(long dialogId, int messageId) {
        var list = deletePermitted.get(dialogId);
        if (list == null) {
            list = new ArrayList<>();
            deletePermitted.put(dialogId, list);
        }

        list.add(messageId);
    }

    public static synchronized boolean isDeletePermitted(long dialogId, int messageId) {
        var list = deletePermitted.get(dialogId);
        if (list == null) {
            return false;
        }

        return list.contains(messageId);
    }

    public static synchronized void messageDeleted(long dialogId, int messageId) {
        var list = deletePermitted.get(dialogId);
        if (list == null) {
            return;
        }

        list.remove((Object) messageId);
    }
}
