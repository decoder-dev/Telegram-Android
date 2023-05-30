package com.radolyn.ayugram.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity()
public class DeletedMessage {
    @PrimaryKey(autoGenerate = true)
    public long fakeId;

    public long userId;
    public long dialogId;
    public long messageId;
    public long date;
}
