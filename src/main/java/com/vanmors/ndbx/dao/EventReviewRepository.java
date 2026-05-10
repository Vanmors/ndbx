package com.vanmors.ndbx.dao;

import com.vanmors.ndbx.entity.EventReview;
import org.springframework.data.cassandra.core.mapping.MapId;
import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.Collection;
import java.util.List;

public interface EventReviewRepository extends CassandraRepository<EventReview, MapId> {
    List<EventReview> findByEventId(String eventId);
    List<EventReview> findByEventIdIn(Collection<String> eventIds);
}
