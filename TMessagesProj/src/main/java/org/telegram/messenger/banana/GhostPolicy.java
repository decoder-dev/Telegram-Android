package org.telegram.messenger.banana;

/** Activity codes are the ones accepted by MessagesController.sendTyping. */
public final class GhostPolicy {
    private GhostPolicy() {}

    public static boolean shouldWarnStory(boolean enabled, boolean privateViews, long owner, long target) {
        return enabled && !privateViews && owner > 0 && target != 0 && target != owner;
    }

    public static boolean sendSilently(int mode, boolean ghostActive) {
        return mode == 2 || mode == 1 && ghostActive;
    }

    public static boolean shouldReturnOffline(boolean hideOnline, boolean autoOffline, boolean onlineStatus) {
        return hideOnline && autoOffline && onlineStatus;
    }

    public static boolean suppressActivity(int action, boolean typing, boolean recording, boolean upload, boolean reads) {
        switch (action) {
            case 0: // Typing.
            case 6: // Playing a game.
            case 10: // Choosing a sticker.
                return typing;
            case 1: // Recording voice.
            case 7: // Recording a round video.
                return recording;
            case 3: // Uploading a document.
            case 4: // Uploading a photo.
            case 5: // Uploading a video.
            case 8: // Uploading a round video.
            case 9: // Uploading audio.
                return upload;
            case 11: // A seen emoji interaction is also a read receipt.
                return typing || reads;
            default: // In particular, never suppress cancellation (2).
                return false;
        }
    }
}
