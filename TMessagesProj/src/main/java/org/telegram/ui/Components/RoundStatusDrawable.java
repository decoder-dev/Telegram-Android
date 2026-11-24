/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Components;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

public class RoundStatusDrawable extends StatusDrawable {

    private boolean isChat = false;
    private long lastUpdateTime = 0;
    private boolean started = false;
    private float progress;
    private int progressDirection = 1;

    private Paint currentPaint;

    private final boolean centerChatTitle = org.telegram.messenger.banana.BananaConfig.glassCenteredHeader();
    private org.telegram.ui.ChatActivity chatActivity;

    public RoundStatusDrawable(boolean createPaint) {
        this(createPaint, null);
    }

    public RoundStatusDrawable(boolean createPaint, org.telegram.ui.ChatActivity chatActivity) {
        this.chatActivity = chatActivity;
        if (createPaint) {
            currentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        }
    }

    @Override
    public void setColor(int color) {
        if (currentPaint != null) {
            currentPaint.setColor(color);
        }
    }

    public void setIsChat(boolean value) {
        isChat = value;
    }

    private void update() {
        long newTime = System.currentTimeMillis();
        long dt = newTime - lastUpdateTime;
        lastUpdateTime = newTime;
        if (dt > 50) {
            dt = 50;
        }
        progress += progressDirection * dt / 400.0f;
        if (progressDirection > 0 && progress >= 1.0f) {
            progressDirection = -1;
            progress = 1;
        } else if (progressDirection < 0 && progress <= 0) {
            progressDirection = 1;
            progress = 0;
        }

        invalidateLimited();
    }

    public void start() {
        lastUpdateTime = System.currentTimeMillis();
        started = true;
        invalidateSelf();
    }

    public void stop() {
        started = false;
    }

    @Override
    public void draw(Canvas canvas) {
        Paint paint = currentPaint == null ? Theme.chat_statusPaint : currentPaint;
        paint.setAlpha(55 + (int) (200 * progress));
        if (centerChatTitle && chatActivity != null) {
            float centerX = getBounds().centerX() - AndroidUtilities.dp(1);
            float centerY = AndroidUtilities.dp(isChat ? 8 : 9);
            canvas.drawCircle(centerX, centerY, AndroidUtilities.dp(4), paint);
        } else {
            canvas.drawCircle(AndroidUtilities.dp(6), AndroidUtilities.dp(isChat ? 8 : 9), AndroidUtilities.dp(4), paint);
        }
        if (started) {
            update();
        }
    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(ColorFilter cf) {

    }

    @Override
    public int getOpacity() {
        return 0;
    }

    @Override
    public int getIntrinsicWidth() {
        return centerChatTitle && chatActivity != null ? AndroidUtilities.dp(8) : AndroidUtilities.dp(12);
    }

    @Override
    public int getIntrinsicHeight() {
        return AndroidUtilities.dp(10);
    }
}
