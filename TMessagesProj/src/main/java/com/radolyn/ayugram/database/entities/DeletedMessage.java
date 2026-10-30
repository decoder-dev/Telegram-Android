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
import androidx.room.Index;
import androidx.room.PrimaryKey;

// Chat history merges deleted messages on every page load; the index keeps that a range read.
@Entity(indices = {@Index({"userId", "dialogId", "topicId", "messageId"})})
public class DeletedMessage extends AyuMessageBase {
    @PrimaryKey(autoGenerate = true)
    public long fakeId;
}
