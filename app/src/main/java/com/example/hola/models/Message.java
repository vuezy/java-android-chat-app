package com.example.hola.models;

import com.google.firebase.firestore.DocumentId;

import java.io.Serializable;

public class Message implements Serializable {

    @DocumentId
    private String id;
    private String chatId;
    private String sender;
    private String content;
    private String dateTime;

    public Message() {}

    public Message(String chatId, String sender, String content, String dateTime) {
        this.chatId = chatId;
        this.sender = sender;
        this.content = content;
        this.dateTime = dateTime;
    }

    public String getId() {
        return id;
    }

    public String getChatId() {
        return chatId;
    }

    public String getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public String getDateTime() {
        return dateTime;
    }
}
