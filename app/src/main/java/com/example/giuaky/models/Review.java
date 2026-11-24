package com.example.giuaky.models;

import com.google.firebase.firestore.DocumentId;
import java.io.Serializable;

public class Review implements Serializable {
    @DocumentId
    private String id;
    private String userId;
    private String userName;
    private double rating;
    private String comment;
    private long createdAt;

    // Required empty constructor for Firestore
    public Review() {
    }

    public Review(String userId, String userName, double rating, String comment, long createdAt) {
        this.userId = userId;
        this.userName = userName;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public Review(String id, String userId, String userName, double rating, String comment, long createdAt) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
