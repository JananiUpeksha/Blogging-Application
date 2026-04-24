package com.example.chatapp;

public class Message {
    public long id;
    public String title;
    public String body;
    public String imagePath;
    public long timestamp;
    public boolean isPending;

    public Message() {}

    public Message(String title, String body) {
        this.title = title;
        this.body = body;
        this.timestamp = System.currentTimeMillis();
        this.isPending = true;
    }
}