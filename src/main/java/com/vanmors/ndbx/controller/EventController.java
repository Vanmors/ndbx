package com.vanmors.ndbx.controller;

import com.vanmors.ndbx.controller.response.EventsResponse;
import com.vanmors.ndbx.dto.EventDto;
import com.vanmors.ndbx.dto.EventPatchDto;
import com.vanmors.ndbx.entity.Category;
import com.vanmors.ndbx.entity.Event;
import com.vanmors.ndbx.service.EventReactionService;
import com.vanmors.ndbx.service.EventService;
import com.vanmors.ndbx.service.SessionService;
import com.vanmors.ndbx.service.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/events")
public class EventController {

    private static final Logger log = LoggerFactory.getLogger(EventController.class);

    private final ObjectMapper objectMapper;

    private final EventService eventService;

    private final EventReactionService eventReactionService;

    private final SessionService sessionService;

    private final CookieBuilder cookieBuilder;

    private final MongoTemplate mongoTemplate;

    @Autowired
    public EventController(final EventService eventService, final EventReactionService eventReactionService, final SessionService sessionService, final CookieBuilder cookieBuilder, final ObjectMapper objectMapper, final MongoTemplate mongoTemplate) {
        this.eventService = eventService;
        this.eventReactionService = eventReactionService;
        this.sessionService = sessionService;
        this.cookieBuilder = cookieBuilder;
        this.objectMapper = objectMapper;
        this.mongoTemplate = mongoTemplate;

    }

    @PostMapping
    public ResponseEntity<Map<String, String>> create(
            @RequestBody final EventDto dto,
            @CookieValue(name = "${app.session.cookie-name}", required = false) final String sid,
            final HttpServletRequest request) {

        final ResponseCookie cookie = cookieBuilder.build(sid);

        logRequest("Post /events/", dto, sid, request);

        try {
            final Event event = eventService.createEvent(dto, sid);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(Map.of("id", event.getId()));

        } catch (final DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(Map.of("message", "event already exists"));
        }
    }

    @GetMapping
    public ResponseEntity<EventsResponse> findFiltered(
            @RequestParam(name = "id", required = false) final String id,
            @RequestParam(name = "title", required = false) final String title,
            @RequestParam(name = "category", required = false) final Category category,
            @RequestParam(name = "price_from", required = false) final Long price_from,
            @RequestParam(name = "price_to", required = false) final Long price_to,
            @RequestParam(name = "city", required = false) final String city,
            @RequestParam(name = "date_from", required = false) final String date_from,
            @RequestParam(name = "date_to", required = false) final String date_to,
            @RequestParam(name = "user", required = false) final String user,
            @RequestParam(name = "include", required = false) final String include,
            @Min(0) @RequestParam(name = "limit", defaultValue = "10") final int limit,
            @Min(0) @RequestParam(name = "offset", defaultValue = "0") final int offset,
            @CookieValue(name = "${app.session.cookie-name}", required = false) final String sid,
            final HttpServletRequest request) {

        logRequest("GET /events/", null, sid, request);

        final ResponseCookie cookie = cookieBuilder.build(sid);

        final Page<EventDto> page = eventService.findFiltered(
                id, title, category, price_from, price_to, city,
                date_from, date_to, user, limit, offset);

        List<EventDto> events = page.getContent();
        if ("reactions".equals(include)) {
            events = enrichWithReactions(events);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new EventsResponse(events, page.getTotalElements()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> patchEvent(
            @PathVariable(name = "id") final String id,
            @RequestBody final EventPatchDto patchDto,
            @CookieValue(name = "${app.session.cookie-name}", required = false) final String sid,
            final HttpServletRequest request) {

        logRequest("Patch /events/" + patchDto, null, sid, request);

        if (sid == null || sessionService.getUserIdFromSession(sid).isEmpty()) {
            throw new UnauthorizedException("not authenticated");
        }

        final ResponseCookie cookie = cookieBuilder.build(sid);

        eventService.patchEvent(id, patchDto, sid);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventDto> getEvent(
            @PathVariable(name = "id") final String id,
            @RequestParam(name = "include", required = false) final String include,
            @CookieValue(name = "${app.session.cookie-name}", required = false) final String sid,
            final HttpServletRequest request) {

        logRequest("GET /events/" + id, null, sid, request);

        final ResponseCookie cookie = cookieBuilder.build(sid);
        final Event event = eventService.findById(id);

        EventDto dto = EventDto.fromEntity(event);
        if ("reactions".equals(include)) {
            dto = dto.withReactions(eventReactionService.getReactions(event.getId()));
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(dto);
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Void> likeEvent(@PathVariable(name = "id") final String eventId,
                                          @CookieValue(name = "${app.session.cookie-name}", required = false) final String sid) {

        final Optional<String> userId = sessionService.getUserIdFromSession(sid);
        if (sid == null || userId.isEmpty()) {
            throw new UnauthorizedException("not authenticated");
        }

        final ResponseCookie cookie = cookieBuilder.build(sid);

        eventReactionService.like(eventId, userId.get());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @PostMapping("/{id}/dislike")
    public ResponseEntity<Void> dislikeEvent(@PathVariable(name = "id") final String eventId,
                                             @CookieValue(name = "${app.session.cookie-name}", required = false) final String sid) {

        final Optional<String> userId = sessionService.getUserIdFromSession(sid);
        if (sid == null || userId.isEmpty()) {
            throw new UnauthorizedException("not authenticated");
        }

        final ResponseCookie cookie = cookieBuilder.build(sid);

        eventReactionService.dislike(eventId, userId.get());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    private List<EventDto> enrichWithReactions(final List<EventDto> events) {
        return events.stream()
                .map(dto -> dto.withReactions(eventReactionService.getReactions(dto.id())))
                .toList();
    }

    private void logRequest(final String endpoint, final Object body, final String sid, final HttpServletRequest request) {
        final String sidShort = (sid != null && !sid.isBlank())
                ? sid.substring(0, Math.min(12, sid.length())) + "..."
                : "null";

        try {
            final String bodyStr = (body != null) ? objectMapper.writeValueAsString(body) : "{}";
            log.info("[REQUEST] {} | sid={} | body={}", endpoint, sidShort, bodyStr);
        } catch (final Exception e) {
            log.info("[REQUEST] {} | sid={}", endpoint, sidShort);
        }
    }
}
