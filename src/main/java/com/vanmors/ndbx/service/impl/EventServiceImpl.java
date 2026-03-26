package com.vanmors.ndbx.service.impl;

import com.vanmors.ndbx.dao.EventRepository;
import com.vanmors.ndbx.dto.EventDto;
import com.vanmors.ndbx.entity.Event;
import com.vanmors.ndbx.service.EventService;
import com.vanmors.ndbx.service.SessionService;
import com.vanmors.ndbx.service.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


@Service
public class EventServiceImpl implements EventService {

    final private EventRepository eventRepository;

    final private SessionService sessionService;

    @Autowired
    public EventServiceImpl(final EventRepository eventRepository, final SessionService sessionService) {
        this.eventRepository = eventRepository;
        this.sessionService = sessionService;
    }

    @Override
    public Event createEvent(final EventDto eventDto, final String sid) {
        final String userId = sessionService.getUserIdFromSession(sid).orElseThrow(
                () -> new UnauthorizedException("not authenticated")
        );

        final Event.Location location = new Event.Location();
        if (eventDto.location() != null) {
            location.setAddress(eventDto.location().address());
        }

        final Event event = new Event();
        event.setTitle(eventDto.title());
        event.setDescription(eventDto.description());
        event.setLocation(location);
        event.setStartedAt(eventDto.started_at());
        event.setFinishedAt(eventDto.finished_at());
        event.setCreatedBy(userId);

        return eventRepository.save(event);
    }

    public Page<EventDto> findAll(final String title, final int limit, final int offset) {
        final Pageable pageable = PageRequest.of(offset / limit, limit);
        final Page<Event> page = (title == null || title.isBlank())
                ? eventRepository.findAll(pageable)
                : eventRepository.findByTitleContainingIgnoreCase(title, pageable);

        return page.map(EventDto::fromEntity);
    }
}
