package com.example.hola.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;

import java.io.Serializable;
import java.util.List;

public class Comment implements Serializable {

    @DocumentId
    private String id;
    private String postId;
    private String sender;
    private String content;
    private List<String> likedBy;
    private String dateTime;

    @Exclude
    private User user;

    public Comment() {}

    public Comment(String postId, String sender, String content, List<String> likedBy, String dateTime) {
        this.postId = postId;
        this.sender = sender;
        this.content = content;
        this.likedBy = likedBy;
        this.dateTime = dateTime;
    }

    public String getId() {
        return id;
    }

    public String getPostId() {
        return postId;
    }

    public String getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public List<String> getLikedBy() {
        return likedBy;
    }

    public String getDateTime() {
        return dateTime;
    }

    @Exclude
    public Integer getLikes() {
        return likedBy.size();
    }

    @Exclude
    public User getUser() {
        return user;
    }

    @Exclude
    public void setUser(User user) {
        this.user = user;
    }
}
