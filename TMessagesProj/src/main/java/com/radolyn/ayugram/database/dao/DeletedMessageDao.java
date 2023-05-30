package com.radolyn.ayugram.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.radolyn.ayugram.database.entities.DeletedMessage;

@Dao
public interface DeletedMessageDao {
    @Query("SELECT EXISTS(SELECT * FROM deletedmessage WHERE userId = :userId AND dialogId = :dialogId AND messageId = :messageId)")
    boolean isDeleted(long userId, long dialogId, long messageId);

    @Insert
    void insert(DeletedMessage msg);
}
