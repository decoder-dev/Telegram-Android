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
import java.util.HashSet;
import java.util.Set;
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
        public final Attachment attachment;

        public Record(long dialogId, int messageId, long authorId, long topicId, int date,
                      boolean deleted, String text, byte[] formatting) {
            this(dialogId, messageId, authorId, topicId, date, deleted, text, formatting, null);
        }

        public Record(long dialogId, int messageId, long authorId, long topicId, int date,
                      boolean deleted, String text, byte[] formatting, Attachment attachment) {
            this(0, dialogId, messageId, authorId, topicId, date, deleted, text, formatting, System.currentTimeMillis(), attachment);
        }

        private Record(long id, long dialogId, int messageId, long authorId, long topicId, int date,
                       boolean deleted, String text, byte[] formatting, long savedAt, Attachment attachment) {
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
            this.attachment = attachment;
        }
    }

    public static final class Attachment {
        public final String mediaKey, mimeType, displayName, storedName;
        public final int kind;
        public final long bytes;

        public Attachment(String mediaKey, int kind, String mimeType, String displayName) {
            this(mediaKey, kind, mimeType, displayName, null, 0);
        }

        private Attachment(String mediaKey, int kind, String mimeType, String displayName, String storedName, long bytes) {
            this.mediaKey = mediaKey;
            this.kind = kind;
            this.mimeType = mimeType == null ? "application/octet-stream" : mimeType.substring(0, Math.min(127, mimeType.length()));
            this.displayName = displayName == null ? "" : displayName.substring(0, Math.min(255, displayName.length()));
            this.storedName = storedName;
            this.bytes = bytes;
        }
    }

    /** The caller supplies a private app directory; account slot numbers are never file identities. */
    public HistoryDatabase(Context context, File directory, long ownerId, boolean testBackend) {
        super(context, archivePath(directory, ownerId, testBackend), null, 2);
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
        addAttachmentColumns(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1 && newVersion == 2) addAttachmentColumns(db);
        else throw new IllegalStateException("Unsupported history schema migration");
    }

    private static void addAttachmentColumns(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE records ADD COLUMN media_key TEXT");
        db.execSQL("ALTER TABLE records ADD COLUMN media_kind INTEGER NOT NULL DEFAULT 0");
        db.execSQL("ALTER TABLE records ADD COLUMN mime_type TEXT");
        db.execSQL("ALTER TABLE records ADD COLUMN display_name TEXT");
        db.execSQL("ALTER TABLE records ADD COLUMN attachment_name TEXT");
        db.execSQL("ALTER TABLE records ADD COLUMN attachment_size INTEGER NOT NULL DEFAULT 0");
    }

    public boolean append(Record record) {
        return appendWithId(record) > 0;
    }

    /** Negative IDs identify a duplicate that can still need a missing attachment linked. */
    long appendWithId(Record record) {
        requireWorker();
        Attachment attachment = record.attachment;
        String normalizedText = searchKey(record.text + (attachment == null ? "" : " " + attachment.displayName));
        long size = record.text.getBytes(StandardCharsets.UTF_8).length +
                (long) normalizedText.getBytes(StandardCharsets.UTF_8).length + record.formatting.length;
        if (record.dialogId == 0 || record.messageId <= 0 || size > MAX_CONTENT_BYTES ||
                record.text.isEmpty() && attachment == null || attachment != null &&
                (attachment.mediaKey == null || attachment.mediaKey.length() > 64 || attachment.kind < 1 || attachment.kind > 4)) return 0;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            String[] key = {Long.toString(record.dialogId), Integer.toString(record.messageId), record.deleted ? "1" : "0"};
            try (Cursor cursor = db.rawQuery("SELECT id,text,formatting,media_key FROM records WHERE dialog_id=? AND message_id=? " +
                    "AND deleted=? ORDER BY id DESC LIMIT 1", key)) {
                if (cursor.moveToFirst() && (record.deleted || record.text.equals(cursor.getString(1)) &&
                        Arrays.equals(record.formatting, cursor.getBlob(2)) &&
                        java.util.Objects.equals(attachment == null ? null : attachment.mediaKey, cursor.getString(3)))) {
                    db.setTransactionSuccessful();
                    return -cursor.getLong(0);
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
            if (attachment != null) {
                values.put("media_key", attachment.mediaKey);
                values.put("media_kind", attachment.kind);
                values.put("mime_type", attachment.mimeType);
                values.put("display_name", attachment.displayName);
            }
            long inserted = db.insertOrThrow("records", null, values);
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
            return inserted;
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
                String mediaKey = cursor.getString(cursor.getColumnIndexOrThrow("media_key"));
                Attachment media = mediaKey == null ? null : new Attachment(mediaKey,
                        cursor.getInt(cursor.getColumnIndexOrThrow("media_kind")), cursor.getString(cursor.getColumnIndexOrThrow("mime_type")),
                        cursor.getString(cursor.getColumnIndexOrThrow("display_name")), cursor.getString(cursor.getColumnIndexOrThrow("attachment_name")),
                        cursor.getLong(cursor.getColumnIndexOrThrow("attachment_size")));
                records.add(new Record(cursor.getLong(0), cursor.getLong(1), cursor.getInt(2), cursor.getLong(3),
                        cursor.getLong(4), cursor.getInt(5), cursor.getInt(6) != 0, cursor.getString(7), cursor.getBlob(8), cursor.getLong(9), media));
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

    boolean linkAttachment(long recordId, String storedName, long bytes) {
        requireWorker();
        if (recordId <= 0 || storedName == null || !storedName.matches("[a-f0-9]{32}\\.[a-z0-9]{1,10}") || bytes < 0) return false;
        ContentValues values = new ContentValues();
        values.put("attachment_name", storedName);
        values.put("attachment_size", bytes);
        return getWritableDatabase().update("records", values, "id=? AND media_key IS NOT NULL AND attachment_name IS NULL",
                new String[]{Long.toString(recordId)}) == 1;
    }

    boolean canLinkAttachment(long recordId) {
        requireWorker();
        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT 1 FROM records WHERE id=? AND media_key IS NOT NULL AND attachment_name IS NULL",
                new String[]{Long.toString(recordId)})) {
            return cursor.moveToFirst();
        }
    }

    Attachment storedMedia(String mediaKey) {
        requireWorker();
        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT media_kind,mime_type,display_name,attachment_name,attachment_size " +
                "FROM records WHERE media_key=? AND attachment_name IS NOT NULL ORDER BY id DESC LIMIT 1", new String[]{mediaKey})) {
            return cursor.moveToFirst() ? new Attachment(mediaKey, cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getLong(4)) : null;
        }
    }

    Set<String> referencedAttachments() {
        requireWorker();
        HashSet<String> names = new HashSet<>();
        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT attachment_name FROM records WHERE attachment_name IS NOT NULL", null)) {
            while (cursor.moveToNext()) names.add(cursor.getString(0));
        }
        return names;
    }

    private static void requireWorker() {
        if (Looper.myLooper() == Looper.getMainLooper()) throw new IllegalStateException("History IO must not run on the UI thread");
    }

    private static String searchKey(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
    }
}
