package com.radolyn.ayugram.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.radolyn.ayugram.database.dao.DeletedMessageDao;
import com.radolyn.ayugram.database.dao.EditedMessageDao;
import com.radolyn.ayugram.database.entities.DeletedMessage;
import com.radolyn.ayugram.database.entities.EditedMessage;

@Database(entities = {
        EditedMessage.class,
        DeletedMessage.class
}, version = 3)
public abstract class AyuDatabase extends RoomDatabase {
    public abstract EditedMessageDao editedMessageDao();

    public abstract DeletedMessageDao deletedMessageDao();
}