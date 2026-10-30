/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.database;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.radolyn.ayugram.AyuConstants;
import com.radolyn.ayugram.database.dao.DeletedMessageDao;
import com.radolyn.ayugram.database.dao.EditedMessageDao;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;

public class AyuData {
    private static AyuDatabase database;
    private static EditedMessageDao editedMessageDao;
    private static DeletedMessageDao deletedMessageDao;

    // Explicit: without a migration Room would refuse to open the database (see create()).
    private static final Migration MIGRATION_21_22 = new Migration(21, 22) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_EditedMessage_userId_dialogId_messageId` ON `EditedMessage` (`userId`, `dialogId`, `messageId`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_DeletedMessage_userId_dialogId_topicId_messageId` ON `DeletedMessage` (`userId`, `dialogId`, `topicId`, `messageId`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_DeletedMessageReaction_deletedMessageId` ON `DeletedMessageReaction` (`deletedMessageId`)");
        }
    };

    static {
        create();
    }

    public static synchronized void create() {
        database = build();
        try {
            // Open now, so a schema Room cannot migrate surfaces here instead of crashing whatever
            // touches the archive first, on every launch.
            database.getOpenHelper().getWritableDatabase();
        } catch (RuntimeException unopenable) {
            FileLog.e(unopenable);
            try {
                database.close();
            } catch (RuntimeException ignore) {
            }
            setAsideBroken();
            database = build();
        }

        editedMessageDao = database.editedMessageDao();
        deletedMessageDao = database.deletedMessageDao();
    }

    /** Keeps an archive that cannot be opened next to the new one instead of deleting it. */
    private static void setAsideBroken() {
        java.io.File file = ApplicationLoader.applicationContext.getDatabasePath(AyuConstants.AYU_DATABASE);
        String suffix = ".broken-" + System.currentTimeMillis();
        for (String extension : new String[]{"", "-wal", "-shm", "-journal"}) {
            java.io.File part = new java.io.File(file.getPath() + extension);
            if (part.exists() && !part.renameTo(new java.io.File(file.getPath() + suffix + extension))) {
                part.delete();
            }
        }
    }

    private static AyuDatabase build() {
        return Room.databaseBuilder(ApplicationLoader.applicationContext, AyuDatabase.class, AyuConstants.AYU_DATABASE)
                .allowMainThreadQueries()
                .addMigrations(MIGRATION_21_22)
                // Every upgrade from 21 on needs an explicit migration: a blanket destructive fallback
                // would silently wipe saved deleted and edited messages on the next schema change.
                // Only a downgrade, or a schema older than this fork ever shipped, starts over.
                .fallbackToDestructiveMigrationOnDowngrade()
                .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20)
                .build();
    }

    public static synchronized AyuDatabase getDatabase() {
        return database;
    }

    public static synchronized EditedMessageDao getEditedMessageDao() {
        return editedMessageDao;
    }

    public static synchronized DeletedMessageDao getDeletedMessageDao() {
        return deletedMessageDao;
    }

    public static synchronized void clean() {
        database.close();

        ApplicationLoader.applicationContext.deleteDatabase(AyuConstants.AYU_DATABASE);
    }
}
