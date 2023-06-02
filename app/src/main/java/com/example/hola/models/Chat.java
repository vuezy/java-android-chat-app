package com.example.hola.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;

import java.io.Serializable;
import java.util.List;

public class Chat implements Serializable {

    @DocumentId
    private String id;
    private List<String> users;
    private String latestMessage;
    private String updatedAt;

    @Exclude
    private User receiver;
    @Exclude
    private Integer unreadMessages = 0;

    public Chat() {}

    public Chat(List<String> users, String latestMessage, String updatedAt) {
        this.users = users;
        this.latestMessage = latestMessage;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    @Exclude
    public void setId(String id) {
        this.id = id;
    }

    public List<String> getUsers() {
        return users;
    }

    public String getLatestMessage() {
        return latestMessage;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    @Exclude
    public User getReceiver() {
        return receiver;
    }

    @Exclude
    public void setReceiver(User user) {
        receiver = user;
    }

    @Exclude
    public Integer getUnreadMessages() {
        return unreadMessages;
    }

    @Exclude
    public void setUnreadMessages(Integer total) {
        unreadMessages = total;
    }
}
