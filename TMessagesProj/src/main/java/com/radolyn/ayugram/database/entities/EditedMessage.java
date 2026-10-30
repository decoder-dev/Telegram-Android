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

// Every lookup is per message; without the index each one scanned the whole table.
@Entity(indices = {@Index({"userId", "dialogId", "messageId"})})
public class EditedMessage extends AyuMessageBase {
    @PrimaryKey(autoGenerate = true)
    public long fakeId;
}
