package com.example.hola.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;

import java.io.Serializable;

public class User implements Serializable {

    @DocumentId
    private String id;
    private String image;
    private String name;
    private String username;
    private String email;
    private String password;

    public User() {}

    public User(String image, String name, String username, String email, String password) {
        this.image = image;
        this.name = name;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public String getId() {
        return id;
    }

    @Exclude
    public void setId(String id) {
        this.id = id;
    }

    public String getImage() {
        return image;
    }

    public String getName() {
        return name;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
