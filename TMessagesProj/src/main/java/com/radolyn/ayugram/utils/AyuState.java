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
    // dialog -> message -> expiry. A permit lets the archive skip a message the user is deleting;
    // it used to live forever when the delete failed or was cancelled, so a later remote delete of
    // the same message was not archived, and repeated deletes added duplicates without limit.
    private static final LongSparseArray<java.util.HashMap<Integer, Long>> deletePermitted = new LongSparseArray<>();
    private static final long DELETE_PERMIT_MS = 2 * 60 * 1000;

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
        long now = android.os.SystemClock.elapsedRealtime();
        for (int i = deletePermitted.size() - 1; i >= 0; i--) {
            var permits = deletePermitted.valueAt(i);
            permits.values().removeIf(expiry -> expiry <= now);
            if (permits.isEmpty()) deletePermitted.removeAt(i);
        }
        var permits = deletePermitted.get(dialogId);
        if (permits == null) {
            permits = new java.util.HashMap<>();
            deletePermitted.put(dialogId, permits);
        }
        permits.put(messageId, now + DELETE_PERMIT_MS);
    }

    public static synchronized boolean isDeletePermitted(long dialogId, int messageId) {
        var permits = deletePermitted.get(dialogId);
        if (permits == null) {
            return false;
        }
        Long expiry = permits.get(messageId);
        if (expiry == null) {
            return false;
        }
        if (expiry <= android.os.SystemClock.elapsedRealtime()) {
            permits.remove(messageId);
            return false;
        }
        return true;
    }

    public static synchronized void messageDeleted(long dialogId, int messageId) {
        var permits = deletePermitted.get(dialogId);
        if (permits == null) {
            return;
        }
        permits.remove(messageId);
        if (permits.isEmpty()) {
            deletePermitted.remove(dialogId);
        }
    }
}
