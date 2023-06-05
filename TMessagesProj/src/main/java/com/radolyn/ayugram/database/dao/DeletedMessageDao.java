package com.radolyn.ayugram.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.radolyn.ayugram.database.entities.DeletedMessage;

import java.util.List;

@Dao
public interface DeletedMessageDao {
    @Query("SELECT EXISTS(SELECT * FROM deletedmessage WHERE userId = :userId AND dialogId = :dialogId AND messageId = :messageId)")
    boolean isDeleted(long userId, long dialogId, long messageId);

    @Query("SELECT EXISTS(SELECT * FROM deletedmessage WHERE userId = :userId AND dialogId = :dialogId AND messageId in (:messageIds))")
    boolean isDeleted(long userId, long dialogId, List<Integer> messageIds);

    @Insert
    void insert(DeletedMessage msg);

    @Query("DELETE FROM deletedmessage")
    void cleanTable();
}
