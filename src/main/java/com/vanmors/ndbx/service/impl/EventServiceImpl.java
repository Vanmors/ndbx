package com.vanmors.ndbx.service.impl;

import com.vanmors.ndbx.dao.EventRepository;
import com.vanmors.ndbx.dto.EventDto;
import com.vanmors.ndbx.dto.EventPatchDto;
import com.vanmors.ndbx.entity.Category;
import com.vanmors.ndbx.entity.Event;
import com.vanmors.ndbx.entity.User;
import com.vanmors.ndbx.service.EventService;
import com.vanmors.ndbx.service.SessionService;
import com.vanmors.ndbx.service.UserService;
import com.vanmors.ndbx.service.exception.UnauthorizedException;
import com.vanmors.ndbx.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;


@Service
public class EventServiceImpl implements EventService {

    private static final Logger log = LoggerFactory.getLogger(EventServiceImpl.class);

    final private EventRepository eventRepository;

    final private UserService userService;

    final private SessionService sessionService;

    final private MongoTemplate mongoTemplate;

    @Autowired
    public EventServiceImpl(final EventRepository eventRepository,
                            final SessionService sessionService,
                            final MongoTemplate mongoTemplate,
                            final UserService userService) {
        this.eventRepository = eventRepository;
        this.sessionService = sessionService;
        this.mongoTemplate = mongoTemplate;
        this.userService = userService;
    }

    @Override
    public Event createEvent(final EventDto eventDto, final String sid) {

        final String userId = sessionService.getUserIdFromSession(sid).orElseThrow(
                () -> new UnauthorizedException("not authenticated")
        );

        if (eventRepository.countByTitleContainingIgnoreCase(eventDto.title()) > 0) {
            throw new DataIntegrityViolationException("event already exists");
        }

        final Event event = getEvent(eventDto, userId);

        sessionService.createOrRefreshSession(sid);

        return eventRepository.save(event);
    }

    private static Event getEvent(final EventDto eventDto, final String userId) {
        final Event.Location location = new Event.Location();
        if (eventDto.location() != null) {
            location.setCity(eventDto.location().city());
            location.setAddress(eventDto.location().address());
        }

        final Event event = new Event();
        event.setTitle(eventDto.title());
        event.setDescription(eventDto.description());
        event.setCategory(eventDto.category());
        event.setPrice(eventDto.price());
        event.setLocation(location);
        event.setStartedAt(eventDto.started_at());
        event.setFinishedAt(eventDto.finished_at());
        event.setCreatedBy(userId);
        return event;
    }

    public Page<EventDto> findAll(final String title, final int limit, final int offset) {
        final Pageable pageable = PageRequest.of(offset / limit, limit);
        final Page<Event> page = (title == null || title.isBlank())
                ? eventRepository.findAll(pageable)
                : eventRepository.findByTitleContainingIgnoreCase(title, pageable);

        return page.map(EventDto::fromEntity);
    }

    @Override
    public Page<EventDto> findFiltered(
            final String title,
            final Category category,
            final Long priceFrom,
            final Long priceTo,
            final String city,
            final String dateFrom,
            final String dateTo,
            final String user,
            final int limit,
            final int offset) {

        final Pageable pageable = PageRequest.of(offset / limit, limit);

        final Query query = new Query();

        if (StringUtils.hasText(title)) {
            query.addCriteria(Criteria.where("title").regex(title, "i"));
        }

        if (category != null) {
            query.addCriteria(Criteria.where("category").is(category));
        }

        Criteria criteriaPrice = Criteria.where("price");

        if (priceFrom != null) {
            criteriaPrice = criteriaPrice.gte(priceFrom);
        }
        if (priceTo != null) {
            criteriaPrice = criteriaPrice.lte(priceTo);
        }

        query.addCriteria(criteriaPrice);

        if (StringUtils.hasText(city)) {
            query.addCriteria(Criteria.where("location.city").is(city));
        }

        Criteria criteriaDate = Criteria.where("started_at");

        if (StringUtils.hasText(dateFrom)) {
            final Instant from = DateUtils.parseDateFromYYYYMMDD(dateFrom);
            criteriaDate = criteriaDate.gte(from);
        }

        if (StringUtils.hasText(dateTo)) {
            final Instant to = DateUtils.parseDateFromYYYYMMDD(dateTo);
            criteriaDate = criteriaDate.lte(to);
        }

        query.addCriteria(criteriaDate);

        final Optional<User> foundedUser = userService.findByUsername(user);
        if (StringUtils.hasText(user) && foundedUser.isPresent()) {
            query.addCriteria(Criteria.where("created_by").is(foundedUser.get().getId()));
        }

        query.with(pageable);

        final List<Event> events = mongoTemplate.find(query, Event.class);
        final long total = mongoTemplate.count(query, Event.class);

        final List<EventDto> dtos = events.stream()
                .map(EventDto::fromEntity)
                .toList();

        return new PageImpl<>(dtos, pageable, total);
    }

    @Override
    public void patchEvent(final String eventId, final EventPatchDto patchDto, final String sid) {
        final String userId = sessionService.getUserIdFromSession(sid)
                .orElseThrow(() -> new UnauthorizedException("not authenticated"));

        final Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Not found. Be sure that event exists and you are the organizer"));

        if (!userId.equals(event.getCreatedBy())) {
            throw new NoSuchElementException("Not found. Be sure that event exists and you are the organizer");
        }

        if (patchDto.category() != null) {
            event.setCategory(patchDto.category());
        }

        if (patchDto.price() != null) {
            event.setPrice(patchDto.price());
        }

        if (patchDto.city() != null) {
            if (patchDto.city().isBlank()) {
                event.getLocation().setCity(null);
            } else {
                event.getLocation().setCity(patchDto.city());
            }
        }

        eventRepository.save(event);
    }

    public Event findById(final String id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Not found"));
    }

    @Override
    public Page<EventDto> findByUser(final String createdBy, final int limit, final int offset) {
        final Pageable pageable = PageRequest.of(offset / limit, limit);

        final Query query = new Query(Criteria.where("created_by").is(createdBy));
        query.with(pageable);

        final List<Event> events = mongoTemplate.find(query, Event.class);
        final long total = mongoTemplate.count(query, Event.class);

        final List<EventDto> dtos = events.stream()
                .map(EventDto::fromEntity)
                .toList();

        return new PageImpl<>(dtos, pageable, total);
    }
}
