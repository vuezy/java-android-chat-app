package com.example.hola.models;

import com.google.firebase.firestore.DocumentId;

import java.io.Serializable;

public class UnreadMessage implements Serializable {

    @DocumentId
    private String id;
    private String chatId;
    private String userId;
    private Integer total;

    public UnreadMessage() {}

    public UnreadMessage(String chatId, String userId, Integer total) {
        this.chatId = chatId;
        this.userId = userId;
        this.total = total;
    }

    public String getId() {
        return id;
    }

    public String getChatId() {
        return chatId;
    }

    public String getUserId() {
        return userId;
    }

    public Integer getTotal() {
        return total;
    }
}
