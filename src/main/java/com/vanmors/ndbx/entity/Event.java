package com.vanmors.ndbx.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


@Document(collection = "events")
@CompoundIndex(name = "title_created_by_unique", def = "{'created_by': 1, 'title': 1}", unique = true)
public class Event {
    @Id
    private String id;

    private String title;

    private Category category;

    private Long price;

    private String description;

    private Location location;

    private Instant created_at = Instant.now();

    @Indexed(name = "created_by_1")
    private String created_by;

    private Instant started_at;

    private Instant finished_at;

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

    public Category getCategory() {
        return category;
    }

    public void setCategory(final Category category) {
        this.category = category;
    }

    public Long getPrice() {
        return price;
    }

    public void setPrice(final Long price) {
        this.price = price;
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

    public Instant getCreatedAt() {
        return created_at;
    }

    public void setCreated_at(final Instant created_at) {
        this.created_at = created_at;
    }

    public String getCreatedBy() {
        return created_by;
    }

    public void setCreatedBy(final String created_by) {
        this.created_by = created_by;
    }

    public Instant getStartedAt() {
        return started_at;
    }

    public void setStartedAt(final Instant started_at) {
        this.started_at = started_at;
    }

    public Instant getFinishedAt() {
        return finished_at;
    }

    public void setFinishedAt(final Instant finished_at) {
        this.finished_at = finished_at;
    }

    public List<String> getParticipant_ids() {
        return participant_ids;
    }

    public void setParticipant_ids(final List<String> participant_ids) {
        this.participant_ids = participant_ids;
    }

    public static class Location {
        private String address;

        private String city;

        public Location() {
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(final String address) {
            this.address = address;
        }

        public void setCity(final String city) {
            this.city = city;
        }

        public String getCity() {
            return city;
        }
    }
}
