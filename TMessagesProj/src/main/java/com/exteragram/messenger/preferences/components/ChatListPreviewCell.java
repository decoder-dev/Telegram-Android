package com.exteragram.messenger.preferences.components;

import android.content.Context;
import android.widget.FrameLayout;

public class ChatListPreviewCell extends FrameLayout {
    public ChatListPreviewCell(Context context) {
        super(context);
        setMinimumHeight(1);
    }

    public void updateStatus(boolean animated) {}

    public void updateTitle(boolean animated) {}

    public void updateCenteredTitle(boolean animated) {}
}
