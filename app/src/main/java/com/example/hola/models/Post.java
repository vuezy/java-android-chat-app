package com.example.hola.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;

import java.io.Serializable;
import java.util.List;

public class Post implements Serializable {

    @DocumentId
    private String id;
    private String poster;
    private String content;
    private Boolean contentIsExpandable;
    private String image;
    private List<String> likedBy;
    private Integer comments;
    private String postedAt;

    @Exclude
    private User user;

    public Post() {}

    public Post(
            String poster, String content, Boolean contentIsExpandable, String image,
            List<String> likedBy, Integer comments, String postedAt
    ) {
        this.poster = poster;
        this.content = content;
        this.contentIsExpandable = contentIsExpandable;
        this.image = image;
        this.likedBy = likedBy;
        this.comments = comments;
        this.postedAt = postedAt;
    }

    public String getId() {
        return id;
    }

    public String getPoster() {
        return poster;
    }

    public String getContent() {
        return content;
    }

    public Boolean getContentIsExpandable() {
        return contentIsExpandable;
    }

    public String getImage() {
        return image;
    }

    public List<String> getLikedBy() {
        return likedBy;
    }

    public Integer getComments() {
        return comments;
    }

    public String getPostedAt() {
        return postedAt;
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
