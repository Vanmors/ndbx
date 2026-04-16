package com.vanmors.ndbx.controller;

import com.vanmors.ndbx.controller.response.EventsResponse;
import com.vanmors.ndbx.dto.EventDto;
import com.vanmors.ndbx.dto.EventPatchDto;
import com.vanmors.ndbx.entity.Category;
import com.vanmors.ndbx.entity.Event;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/events")
public class EventController {

    private static final Logger log = LoggerFactory.getLogger(EventController.class);

    private final ObjectMapper objectMapper;

    private final EventService eventService;

    private final SessionService sessionService;

    private final CookieBuilder cookieBuilder;

    @Autowired
    public EventController(final EventService eventService, final SessionService sessionService, final CookieBuilder cookieBuilder, final ObjectMapper objectMapper) {
        this.eventService = eventService;
        this.sessionService = sessionService;
        this.cookieBuilder = cookieBuilder;
        this.objectMapper = objectMapper;

    }

    @PostMapping
    public ResponseEntity<Map<String, String>> create(
            @RequestBody final EventDto dto,
            @CookieValue(name = "${app.session.cookie-name}", required = false) final String sid,
            final HttpServletRequest request) {

        final ResponseCookie cookie = cookieBuilder.build(sid);

        logRequest("Post /events/", dto, sid, request);
        log.info("id={} ", dto.id());
        log.info("title={} ", dto.title());
        log.info("category={} ", dto.category());
        log.info("price={} ", dto.price());
        log.info("description={} ", dto.description());
        log.info("location={} ", dto.location());
        log.info("created_at={} ", dto.created_at());
        log.info("created_by={} ", dto.created_by());
        log.info("started_at={} ", dto.started_at());

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
            @Min(0) @RequestParam(name = "limit", defaultValue = "10") final int limit,
            @Min(0) @RequestParam(name = "offset", defaultValue = "0") final int offset,
            @CookieValue(name = "${app.session.cookie-name}", required = false) final String sid,
            final HttpServletRequest request) {

        final List<Event> events = eventService.findAll();

        log.info("count all={}", events.size());
        for (final var event: events) {
            log.info("event={}", event.toString());
        }

        logRequest("GET /events/", null, sid, request);
        log.info("id={} ", id);
        log.info("title={} ", title);
        log.info("category={} ", category);
        log.info("price_from={} ", price_from);
        log.info("price_to={} ", price_to);
        log.info("city={} ", city);
        log.info("date_from={} ", date_from);
        log.info("date_to={} ", date_to);
        log.info("user={} ", user);

        final ResponseCookie cookie = cookieBuilder.build(sid);

        final Page<EventDto> page = eventService.findFiltered(
                id, title, category, price_from, price_to, city,
                date_from, date_to, user, limit, offset);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new EventsResponse(page.getContent(), page.getTotalElements()));
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
            @CookieValue(name = "${app.session.cookie-name}", required = false) final String sid,
            final HttpServletRequest request) {

        logRequest("GET /events/" + id, null, sid, request);

        final ResponseCookie cookie = cookieBuilder.build(sid);
        final Event event = eventService.findById(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(EventDto.fromEntity(event));
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
