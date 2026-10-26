package com.exteragram.messenger.preferences.components;

import android.content.Context;
import android.widget.FrameLayout;

import org.telegram.ui.ActionBar.INavigationLayout;

public class AvatarCornersPreviewCell extends FrameLayout {
    public AvatarCornersPreviewCell(Context context, INavigationLayout parentLayout) {
        super(context);
        setMinimumHeight(1);
    }
}
