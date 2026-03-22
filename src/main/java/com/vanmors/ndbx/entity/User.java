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
    private String fullName;

    @Indexed(unique = true)
    private String username;
    private String passwordHashed;
    private Instant createdAt = Instant.now();

    public void setFullName(final String fullName) {
        this.fullName = fullName;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setPasswordHashed(final String passwordHashed) {
        this.passwordHashed = passwordHashed;
    }

    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getFullName() {
        return fullName;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHashed() {
        return passwordHashed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
