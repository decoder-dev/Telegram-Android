/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.easy;

import android.text.TextUtils;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class DummyFileLoadWaiter extends EasyWaiter {

    private final Set<String> pending = new HashSet<>();

    public DummyFileLoadWaiter(int currentAccount, ArrayList<MessageObject> messages) {
        super(currentAccount);
        // Own the pending names: callbacks must never mutate the caller's iteration/list.
        for (MessageObject message : messages) {
            if (com.radolyn.ayugram.AyuUtils.getLocalMessageFile(message) != null) continue;
            String name;
            if (message.getDocument() != null) {
                name = FileLoader.getAttachFileName(message.getDocument());
            } else {
                var photo = MessageObject.getPhoto(message.messageOwner);
                name = photo == null ? "" : FileLoader.getAttachFileName(
                        FileLoader.getClosestPhotoSizeWithSize(photo.sizes, AndroidUtilities.getPhotoSize()));
            }
            if (!TextUtils.isEmpty(name)) pending.add(name);
        }

        notifications.add(NotificationCenter.fileLoaded);
        notifications.add(NotificationCenter.fileLoadFailed);
        notifications.add(NotificationCenter.appDidLogout);
    }

    @Override
    public void subscribe() {
        super.subscribe();
        AndroidUtilities.runOnUIThread(() -> {
            if (pending.isEmpty()) unsubscribe();
        });
    }

    private void process(String name) {
        if (pending.remove(name) && pending.isEmpty()) unsubscribe();
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (account != currentAccount) return;
        if (id == NotificationCenter.appDidLogout) { fail("Forward account changed"); return; }
        if (args.length == 0 || !(args[0] instanceof String)) return;
        if (id == NotificationCenter.fileLoaded) {
            var name = (String) args[0];
            process(name);
        } else if (id == NotificationCenter.fileLoadFailed) {
            var name = (String) args[0];

            if (pending.contains(name)) fail("Forward download failed");
        }
    }
}
