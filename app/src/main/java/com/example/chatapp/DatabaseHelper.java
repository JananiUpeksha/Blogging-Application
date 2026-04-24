package com.example.chatapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "talkpal.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE = "messages";
    public static final String COL_ID = "_id";
    public static final String COL_TITLE = "title";
    public static final String COL_BODY = "content";
    public static final String COL_IMAGE = "image_path";
    public static final String COL_TS = "timestamp";
    public static final String COL_PENDING = "is_pending";

    private static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE + " (" +
                    COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_TITLE + " TEXT NOT NULL, " +
                    COL_BODY + " TEXT, " +
                    COL_IMAGE + " TEXT, " +
                    COL_TS + " INTEGER NOT NULL, " +
                    COL_PENDING + " INTEGER NOT NULL DEFAULT 0);";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context ctx) {
        if (instance == null) instance = new DatabaseHelper(ctx.getApplicationContext());
        return instance;
    }

    private DatabaseHelper(Context context) { super(context, DB_NAME, null, DB_VERSION); }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    public long insertMessage(String title, String body, String imagePath, boolean isPending) {
        ContentValues cv = new ContentValues();
        cv.put(COL_TITLE, title);
        cv.put(COL_BODY, body);
        cv.put(COL_IMAGE, imagePath);
        cv.put(COL_TS, System.currentTimeMillis());
        cv.put(COL_PENDING, isPending ? 1 : 0);
        return getWritableDatabase().insert(TABLE, null, cv);
    }

    public List<Message> getAllMessages() {
        List<Message> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query(TABLE, null, null, null, null, null, COL_TS + " DESC");
        if (c != null) {
            while (c.moveToNext()) list.add(fromCursor(c));
            c.close();
        }
        return list;
    }

    public List<Message> searchMessages(String query) {
        List<Message> list = new ArrayList<>();
        String likeQuery = "%" + query + "%";
        Cursor c = getReadableDatabase().query(TABLE, null,
                COL_TITLE + " LIKE ? OR " + COL_BODY + " LIKE ?",
                new String[]{likeQuery, likeQuery}, null, null, COL_TS + " DESC");
        if (c != null) {
            while (c.moveToNext()) list.add(fromCursor(c));
            c.close();
        }
        return list;
    }

    public Message getMessageById(long id) {
        Cursor c = getReadableDatabase().query(TABLE, null, COL_ID + "=?", new String[]{String.valueOf(id)}, null, null, null, "1");
        Message m = null;
        if (c != null && c.moveToFirst()) m = fromCursor(c);
        if (c != null) c.close();
        return m;
    }

    public int updateMessage(long id, String title, String body, String imagePath) {
        ContentValues cv = new ContentValues();
        cv.put(COL_TITLE, title);
        cv.put(COL_BODY, body);
        cv.put(COL_IMAGE, imagePath);
        cv.put(COL_TS, System.currentTimeMillis());
        return getWritableDatabase().update(TABLE, cv, COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    // --- ADDED THIS TO FIX THE ERROR ---
    public void deleteMessage(long id) {
        getWritableDatabase().delete(TABLE, COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    // --- KEPT YOUR BULK DELETE EXACTLY THE SAME ---
    public void deleteMessages(List<Long> ids) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (long id : ids) {
                db.delete(TABLE, COL_ID + "=?", new String[]{String.valueOf(id)});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private Message fromCursor(Cursor c) {
        Message m = new Message();
        m.id = c.getLong(c.getColumnIndexOrThrow(COL_ID));
        m.title = c.getString(c.getColumnIndexOrThrow(COL_TITLE));
        m.body = c.getString(c.getColumnIndexOrThrow(COL_BODY));
        m.imagePath = c.getString(c.getColumnIndexOrThrow(COL_IMAGE));
        m.timestamp = c.getLong(c.getColumnIndexOrThrow(COL_TS));
        m.isPending = c.getInt(c.getColumnIndexOrThrow(COL_PENDING)) == 1;
        return m;
    }

    public void clearAll() { getWritableDatabase().delete(TABLE, null, null); }
    public int getPendingCount() { return 0; }
}