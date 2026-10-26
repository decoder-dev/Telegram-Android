package org.telegram.messenger.banana;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Looper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.text.Normalizer;
import java.nio.charset.StandardCharsets;

/** Private, bounded archive independent of Telegram's disposable message cache. */
public final class HistoryDatabase extends SQLiteOpenHelper {
    public static final int MAX_RECORDS = 5000;
    public static final int MAX_REVISIONS = 32;
    public static final int MAX_CONTENT_BYTES = 128 * 1024;
    private static final long MAX_ARCHIVE_BYTES = 32L * 1024 * 1024;

    public static final class Record {
        public final long id, dialogId, authorId, topicId, savedAt;
        public final int messageId, date;
        public final boolean deleted;
        public final String text;
        public final byte[] formatting;

        public Record(long dialogId, int messageId, long authorId, long topicId, int date,
                      boolean deleted, String text, byte[] formatting) {
            this(0, dialogId, messageId, authorId, topicId, date, deleted, text, formatting, System.currentTimeMillis());
        }

        private Record(long id, long dialogId, int messageId, long authorId, long topicId, int date,
                       boolean deleted, String text, byte[] formatting, long savedAt) {
            this.id = id;
            this.dialogId = dialogId;
            this.messageId = messageId;
            this.authorId = authorId;
            this.topicId = topicId;
            this.date = date;
            this.deleted = deleted;
            this.text = text == null ? "" : text;
            this.formatting = formatting == null ? new byte[0] : formatting.clone();
            this.savedAt = savedAt;
        }
    }

    /** The caller supplies a private app directory; account slot numbers are never file identities. */
    public HistoryDatabase(Context context, File directory, long ownerId, boolean testBackend) {
        super(context, archivePath(directory, ownerId, testBackend), null, 1);
        requireWorker();
    }

    private static String archivePath(File directory, long ownerId, boolean testBackend) {
        requireWorker();
        if (ownerId <= 0) throw new IllegalArgumentException("An archive requires its owning user ID");
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IllegalStateException("Cannot create private history directory");
        return new File(directory, ownerId + (testBackend ? "-test" : "-production") + ".sqlite").getAbsolutePath();
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        try (Cursor cursor = db.rawQuery("PRAGMA secure_delete=ON", null)) {
            if (!cursor.moveToFirst() || cursor.getInt(0) != 1) throw new IllegalStateException("Cannot enable secure history deletion");
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE records (id INTEGER PRIMARY KEY AUTOINCREMENT, dialog_id INTEGER NOT NULL, " +
                "message_id INTEGER NOT NULL, author_id INTEGER NOT NULL, topic_id INTEGER NOT NULL, " +
                "message_date INTEGER NOT NULL, deleted INTEGER NOT NULL, text TEXT NOT NULL, formatting BLOB NOT NULL, " +
                "saved_at INTEGER NOT NULL, byte_size INTEGER NOT NULL, search_text TEXT NOT NULL)");
        db.execSQL("CREATE INDEX message_revisions ON records(dialog_id,message_id,deleted,id)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("Unsupported history schema migration");
    }

    public boolean append(Record record) {
        requireWorker();
        String normalizedText = searchKey(record.text);
        long size = record.text.getBytes(StandardCharsets.UTF_8).length +
                (long) normalizedText.getBytes(StandardCharsets.UTF_8).length + record.formatting.length;
        if (record.dialogId == 0 || record.messageId <= 0 || size > MAX_CONTENT_BYTES ||
                record.text.isEmpty() && record.formatting.length == 0) return false;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            String[] key = {Long.toString(record.dialogId), Integer.toString(record.messageId), record.deleted ? "1" : "0"};
            try (Cursor cursor = db.rawQuery("SELECT text,formatting FROM records WHERE dialog_id=? AND message_id=? " +
                    "AND deleted=? ORDER BY id DESC LIMIT 1", key)) {
                if (cursor.moveToFirst() && (record.deleted || record.text.equals(cursor.getString(0)) &&
                        Arrays.equals(record.formatting, cursor.getBlob(1)))) {
                    db.setTransactionSuccessful();
                    return false;
                }
            }
            ContentValues values = new ContentValues();
            values.put("dialog_id", record.dialogId);
            values.put("message_id", record.messageId);
            values.put("author_id", record.authorId);
            values.put("topic_id", record.topicId);
            values.put("message_date", record.date);
            values.put("deleted", record.deleted ? 1 : 0);
            values.put("text", record.text);
            values.put("formatting", record.formatting);
            values.put("saved_at", record.savedAt);
            values.put("byte_size", size);
            values.put("search_text", normalizedText);
            db.insertOrThrow("records", null, values);
            db.execSQL("DELETE FROM records WHERE dialog_id=? AND message_id=? AND deleted=0 AND id NOT IN " +
                    "(SELECT id FROM records WHERE dialog_id=? AND message_id=? AND deleted=0 ORDER BY id DESC LIMIT " +
                    MAX_REVISIONS + ")", new Object[]{record.dialogId, record.messageId, record.dialogId, record.messageId});
            db.execSQL("DELETE FROM records WHERE id NOT IN (SELECT id FROM records ORDER BY id DESC LIMIT " + MAX_RECORDS + ")");
            long bytes;
            try (Cursor cursor = db.rawQuery("SELECT COALESCE(SUM(byte_size),0) FROM records", null)) {
                cursor.moveToFirst();
                bytes = cursor.getLong(0);
            }
            if (bytes > MAX_ARCHIVE_BYTES) {
                try (Cursor cursor = db.rawQuery("SELECT id,byte_size FROM records ORDER BY id", null)) {
                    long last = 0;
                    while (bytes > MAX_ARCHIVE_BYTES && cursor.moveToNext()) {
                        last = cursor.getLong(0);
                        bytes -= cursor.getLong(1);
                    }
                    if (last != 0) db.delete("records", "id<=?", new String[]{Long.toString(last)});
                }
            }
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    public List<Record> recent(String search, long dialogId, int limit) {
        requireWorker();
        String query = search == null ? "" : search.substring(0, Math.min(256, search.length()));
        if (!query.isEmpty() && Character.isHighSurrogate(query.charAt(query.length() - 1))) query = query.substring(0, query.length() - 1);
        query = searchKey(query);
        query = "%" + query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        String where = "search_text LIKE ? ESCAPE '\\'" + (dialogId == 0 ? "" : " AND dialog_id=?");
        String[] args = dialogId == 0 ? new String[]{query} : new String[]{query, Long.toString(dialogId)};
        ArrayList<Record> records = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query("records", null, where, args, null, null,
                "id DESC", Integer.toString(Math.max(1, Math.min(limit, 100))))) {
            while (cursor.moveToNext()) {
                records.add(new Record(cursor.getLong(0), cursor.getLong(1), cursor.getInt(2), cursor.getLong(3),
                        cursor.getLong(4), cursor.getInt(5), cursor.getInt(6) != 0, cursor.getString(7), cursor.getBlob(8), cursor.getLong(9)));
            }
        }
        return records;
    }

    public int count() {
        requireWorker();
        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM records", null)) {
            cursor.moveToFirst();
            return cursor.getInt(0);
        }
    }

    public void clearArchive() {
        requireWorker();
        SQLiteDatabase db = getWritableDatabase();
        db.delete("records", null, null);
        db.execSQL("VACUUM");
    }

    private static void requireWorker() {
        if (Looper.myLooper() == Looper.getMainLooper()) throw new IllegalStateException("History IO must not run on the UI thread");
    }

    private static String searchKey(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
    }
}
