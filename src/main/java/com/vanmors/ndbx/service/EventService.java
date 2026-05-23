package com.vanmors.ndbx.service;

import com.vanmors.ndbx.dto.EventDto;
import com.vanmors.ndbx.dto.EventPatchDto;
import com.vanmors.ndbx.entity.Category;
import com.vanmors.ndbx.entity.Event;
import org.springframework.data.domain.Page;

import java.util.List;


public interface EventService {

    Event createEvent(EventDto eventDto, String sid);

    Page<EventDto> findAll(String title, int limit, int offset);

    Page<EventDto> findFiltered(String id, String title, Category category, Long priceFrom,
                                Long priceTo, String city, String dateFrom, String dateTo,
                                String user, int limit, int offset);

    void patchEvent(String eventId, EventPatchDto patchDto, String sid);

    Event findById(String id);

    Event findByIdForReaction(String id);

    Page<EventDto> findByUser(String createdBy, int limit, int offset);

    List<Event> findAll();

    List<Event> findAllByTitle(String title);
}
