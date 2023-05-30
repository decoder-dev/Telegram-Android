package com.radolyn.ayugram.database.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity()
public class EditedMessage {
    @PrimaryKey(autoGenerate = true)
    public long fakeId;

    public long userId;
    public long dialogId;
    public long messageId;

    @ColumnInfo(name = "text")
    public String text;
    @ColumnInfo(name = "type")
    public boolean isDocument;
    @ColumnInfo(name = "path")
    public String path;
    @ColumnInfo(name = "date")
    public long date;
}
