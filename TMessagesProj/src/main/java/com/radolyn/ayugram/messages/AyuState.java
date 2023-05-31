package com.radolyn.ayugram.messages;

public class AyuState {
    private static final Object automaticallyScheduledSync = new Object();
    private static boolean automaticallyScheduled;

    private static final Object allowReadPacketSync = new Object();
    private static boolean allowReadPacket;

    public static boolean isAutomaticallyScheduled() {
        synchronized (automaticallyScheduledSync) {
            return automaticallyScheduled;
        }
    }

    public static void resetAutomaticallyScheduled() {
        synchronized (automaticallyScheduledSync) {
            automaticallyScheduled = false;
        }
    }

    public static void setAutomaticallyScheduled() {
        synchronized (automaticallyScheduledSync) {
            automaticallyScheduled = true;
        }
    }

    public static boolean isAllowReadPacket() {
        synchronized (allowReadPacketSync) {
            return allowReadPacket;
        }
    }

    public static void resetAllowReadPacket() {
        synchronized (allowReadPacketSync) {
            allowReadPacket = false;
        }
    }

    public static void setAllowReadPacket() {
        synchronized (allowReadPacketSync) {
            allowReadPacket = true;
        }
    }

    public static void reset() {
        automaticallyScheduled = false;
        allowReadPacket = false;
    }
}
