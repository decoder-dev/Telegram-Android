/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.easy;

import org.telegram.messenger.NotificationCenter;

public class DummyFileUploadWaiter extends EasyWaiter {
    private final String path;

    public DummyFileUploadWaiter(int currentAccount, String path) {
        super(currentAccount);
        this.path = path;

        notifications.add(NotificationCenter.fileUploaded);
        notifications.add(NotificationCenter.fileUploadFailed);
    }

    private void process(String path) {
        if (this.path != null && this.path.equals(path)) {
            unsubscribe();
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.fileUploaded) {
            final String location = (String) args[0];

            process(location);
        } else if (id == NotificationCenter.fileUploadFailed) {
            final String location = (String) args[0];

            process(location);
        }
    }
}
