package com.vanmors.ndbx.dao;

import com.vanmors.ndbx.entity.EventReaction;
import org.springframework.data.cassandra.core.mapping.MapId;
import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.Collection;
import java.util.List;

public interface EventReactionRepository extends CassandraRepository<EventReaction, MapId> {
    List<EventReaction> findByEventId(String eventId);
    List<EventReaction> findByEventIdIn(Collection<String> eventIds);
}
