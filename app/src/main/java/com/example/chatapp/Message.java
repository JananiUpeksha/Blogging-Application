package com.example.chatapp;

/**
 * Simple data model for a Talk Pal message/post.
 */
public class Message {
    public long    id;
    public String  title;
    public String  body;
    public String  imagePath;   // nullable – local file path or content URI string
    public long    timestamp;   // epoch millis
    public boolean isPending;   // true = not yet synced (offline queue)

    public Message() {}

    public Message(long id, String title, String body,
                   String imagePath, long timestamp, boolean isPending) {
        this.id        = id;
        this.title     = title;
        this.body      = body;
        this.imagePath = imagePath;
        this.timestamp = timestamp;
        this.isPending = isPending;
    }
}
