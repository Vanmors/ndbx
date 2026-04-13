package com.vanmors.ndbx.service;

import com.vanmors.ndbx.dto.EventDto;
import com.vanmors.ndbx.entity.Event;
import org.springframework.data.domain.Page;


public interface EventService {

    Event createEvent(EventDto eventDto, String sid);

    Page<EventDto> findAll(String title, int limit, int offset);
}
