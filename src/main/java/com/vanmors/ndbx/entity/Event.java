package com.vanmors.ndbx.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


@Document(collection = "events")
@CompoundIndex(def = "{'title': 1, 'created_by': 1}")
public class Event {
    @Id
    private String id;

    @Indexed(unique = true)
    private String title;

    private String description;

    private Location location;

    private Instant createdAt = Instant.now();

    @Indexed
    private String createdBy;

    private Instant startedAt;

    private Instant finishedAt;

    @Indexed
    private List<String> participant_ids = new ArrayList<>();

    public Event() {
    }

    // getters & setters
    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(final Location location) {
        this.location = location;
    }

    public Instant getCreated_at() {
        return createdAt;
    }

    public void setCreated_at(final Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(final Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(final Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public List<String> getParticipant_ids() {
        return participant_ids;
    }

    public void setParticipant_ids(final List<String> participant_ids) {
        this.participant_ids = participant_ids;
    }

    public static class Location {
        private String address;

        public Location() {
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(final String address) {
            this.address = address;
        }
    }
}
