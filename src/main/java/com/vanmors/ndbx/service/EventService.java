package com.vanmors.ndbx.service;

import com.vanmors.ndbx.dto.EventDto;
import com.vanmors.ndbx.dto.EventPatchDto;
import com.vanmors.ndbx.entity.Category;
import com.vanmors.ndbx.entity.Event;
import org.springframework.data.domain.Page;


public interface EventService {

    Event createEvent(EventDto eventDto, String sid);

    Page<EventDto> findAll(String title, int limit, int offset);

    Page<EventDto> findFiltered(String id, String title, Category category, Long priceFrom,
                                Long priceTo, String city, String dateFrom, String dateTo,
                                String user, int limit, int offset);

    void patchEvent(String eventId, EventPatchDto patchDto, String sid);

    Event findById(String id);

    Page<EventDto> findByUser(final String createdBy, final int limit, final int offset);
}
