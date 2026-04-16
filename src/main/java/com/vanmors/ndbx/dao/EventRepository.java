package com.vanmors.ndbx.dao;

import com.vanmors.ndbx.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends MongoRepository<Event, String> {
    Page<Event> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    long countByTitleContainingIgnoreCase(String title);
    long countByTitleIgnoreCase(String title);
}