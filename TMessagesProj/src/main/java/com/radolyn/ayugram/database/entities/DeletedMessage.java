package com.radolyn.ayugram.database.entities;

import androidx.room.Entity;

@Entity(primaryKeys = {"userId", "dialogId", "messageId", "date"})
public class DeletedMessage {
    public long userId;
    public long dialogId;
    public long messageId;
    public long date;
}
