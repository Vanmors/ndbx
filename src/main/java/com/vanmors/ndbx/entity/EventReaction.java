package com.vanmors.ndbx.entity;

import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.Indexed;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;

@Table("event_reactions")
public class EventReaction {

    @Indexed
    @PrimaryKeyColumn(name = "event_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private String eventId;

    @Indexed
    @PrimaryKeyColumn(name = "created_by", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    private String createdBy;

    @Indexed
    @Column("like_value")
    private byte likeValue;

    @Column("created_at")
    private Instant createdAt;

    public String getEventId() {
        return eventId;
    }

    public void setEventId(final String eventId) {
        this.eventId = eventId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
    }

    public byte getLikeValue() {
        return likeValue;
    }

    public void setLikeValue(final byte likeValue) {
        this.likeValue = likeValue;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }
}
