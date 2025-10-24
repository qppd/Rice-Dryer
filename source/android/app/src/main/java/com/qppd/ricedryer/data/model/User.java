package com.qppd.ricedryer.data.model;

public class User {
    private String userId;
    private String email;
    private String name;
    private long createdAt;

    public User() {
    }

    public User(String userId, String email, String name, long createdAt) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.createdAt = createdAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
