/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity()
public class DeletedMessage {
    @PrimaryKey(autoGenerate = true)
    public long fakeId;

    public long userId;
    public long dialogId;
    public long groupedId;
    public long peerId;
    public long fromId;
    public long topicId;
    public int messageId;
    public int date;
    public int deletedDate;

    public String text; // html formatted
    public String mediaPath; // full path
    public boolean isDocument;
    public int flags;
    public int editDate;
    public boolean editHide;
    public boolean out;
}
