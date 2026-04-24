package com.example.chatapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";
    private static final String DB_NAME = "talkpal.db";
    private static final int DB_VERSION = 7;

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context ctx) {
        if (instance == null) {
            instance = new DatabaseHelper(ctx.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        Log.d(TAG, "DatabaseHelper created");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Creating database table");
        String createTable = "CREATE TABLE messages (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT NOT NULL, " +
                "body TEXT, " +
                "image_path TEXT, " +
                "timestamp INTEGER, " +
                "is_pending INTEGER DEFAULT 1)";
        db.execSQL(createTable);
        Log.d(TAG, "Table created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database");
        db.execSQL("DROP TABLE IF EXISTS messages");
        onCreate(db);
    }

    // Insert message
    public long insertMessage(String title, String body, String imagePath, boolean isSynced) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("body", body);
        values.put("image_path", imagePath);
        values.put("timestamp", System.currentTimeMillis());
        values.put("is_pending", isSynced ? 0 : 1);

        long id = db.insert("messages", null, values);
        Log.d(TAG, "Inserted message: ID=" + id + ", Title=" + title);
        return id;
    }

    // Update message
    public boolean updateMessage(long id, String title, String body, String imagePath) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("body", body);
        values.put("image_path", imagePath);
        values.put("timestamp", System.currentTimeMillis());

        int rowsAffected = db.update("messages", values, "_id=?", new String[]{String.valueOf(id)});
        Log.d(TAG, "Updated message ID=" + id + ", rowsAffected=" + rowsAffected);
        return rowsAffected > 0;
    }

    // Get all messages
    public List<Message> getAllMessages() {
        List<Message> messages = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query("messages", null, null, null, null, null, "timestamp DESC");

        Log.d(TAG, "getAllMessages: cursor count=" + cursor.getCount());

        while (cursor.moveToNext()) {
            Message msg = new Message();
            msg.id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
            msg.title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
            msg.body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
            msg.imagePath = cursor.getString(cursor.getColumnIndexOrThrow("image_path"));
            msg.timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"));
            msg.isPending = cursor.getInt(cursor.getColumnIndexOrThrow("is_pending")) == 1;
            messages.add(msg);
            Log.d(TAG, "Loaded: ID=" + msg.id + ", Title=" + msg.title);
        }
        cursor.close();

        return messages;
    }

    // Get message by ID
    public Message getMessageById(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query("messages", null, "_id=?", new String[]{String.valueOf(id)}, null, null, null);

        Message msg = null;
        if (cursor != null && cursor.moveToFirst()) {
            msg = new Message();
            msg.id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
            msg.title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
            msg.body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
            msg.imagePath = cursor.getString(cursor.getColumnIndexOrThrow("image_path"));
            msg.timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"));
            msg.isPending = cursor.getInt(cursor.getColumnIndexOrThrow("is_pending")) == 1;
            cursor.close();
        }
        return msg;
    }

    // Delete single message - THIS IS THE MISSING METHOD
    public void deleteMessage(long id) {
        SQLiteDatabase db = getWritableDatabase();
        int deleted = db.delete("messages", "_id=?", new String[]{String.valueOf(id)});
        Log.d(TAG, "Deleted message ID=" + id + ", rows deleted=" + deleted);
    }

    // Delete multiple messages
    public void deleteMessages(List<Long> ids) {
        SQLiteDatabase db = getWritableDatabase();
        for (long id : ids) {
            db.delete("messages", "_id=?", new String[]{String.valueOf(id)});
            Log.d(TAG, "Deleted message ID=" + id);
        }
    }

    // Get pending messages
    public List<Message> getPendingMessages() {
        List<Message> messages = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query("messages", null, "is_pending=1", null, null, null, "timestamp ASC");

        while (cursor.moveToNext()) {
            Message msg = new Message();
            msg.id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
            msg.title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
            msg.body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
            msg.imagePath = cursor.getString(cursor.getColumnIndexOrThrow("image_path"));
            msg.timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"));
            msg.isPending = true;
            messages.add(msg);
        }
        cursor.close();
        return messages;
    }

    // Get pending count
    public int getPendingCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM messages WHERE is_pending=1", null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    // Update sync status
    public void updateSyncStatus(long id, int status) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_pending", status);
        db.update("messages", values, "_id=?", new String[]{String.valueOf(id)});
        Log.d(TAG, "Updated sync status for ID=" + id + " to " + status);
    }

    // Search messages by title or body
    public List<Message> searchMessages(String query) {
        List<Message> messages = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        String likeQuery = "%" + query + "%";
        Cursor cursor = db.query(
                "messages",
                null,
                "title LIKE ? OR body LIKE ?",
                new String[]{likeQuery, likeQuery},
                null,
                null,
                "timestamp DESC"
        );

        while (cursor.moveToNext()) {
            Message msg = new Message();
            msg.id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
            msg.title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
            msg.body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
            msg.imagePath = cursor.getString(cursor.getColumnIndexOrThrow("image_path"));
            msg.timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"));
            msg.isPending = cursor.getInt(cursor.getColumnIndexOrThrow("is_pending")) == 1;
            messages.add(msg);
        }
        cursor.close();

        return messages;
    }
    // Clear all
    public void clearAll() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("messages", null, null);
        Log.d(TAG, "Cleared all messages");
    }
}