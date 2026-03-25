package com.vanmors.ndbx.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "users")
public class User {
    public String getId() {
        return id;
    }

    @Id
    private String id;
    private String full_name;

    @Indexed(unique = true)
    private String username;
    private String password_hash;
    private Instant createdAt = Instant.now();

    public void setFullName(final String full_name) {
        this.full_name = full_name;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setPasswordHashed(final String password_hash) {
        this.password_hash = password_hash;
    }

    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getFullName() {
        return full_name;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHashed() {
        return password_hash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
