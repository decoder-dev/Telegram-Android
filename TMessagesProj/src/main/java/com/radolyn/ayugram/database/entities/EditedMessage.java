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
public class EditedMessage {
    @PrimaryKey(autoGenerate = true)
    public long fakeId;

    public long userId;
    public long dialogId;
    public int messageId;
    public int editedDate;

    public String text; // plain text
    public String textEntities; // JSON+TL serialized
    public String mediaPath; // full path
    public int documentType; // see DOCUMENT_TYPE_*
}
