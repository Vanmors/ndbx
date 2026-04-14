package com.vanmors.ndbx.controller;

import com.vanmors.ndbx.controller.response.EventsResponse;
import com.vanmors.ndbx.dto.EventDto;
import com.vanmors.ndbx.dto.EventPatchDto;
import com.vanmors.ndbx.entity.Category;
import com.vanmors.ndbx.entity.Event;
import com.vanmors.ndbx.service.EventService;
import com.vanmors.ndbx.service.SessionService;
import com.vanmors.ndbx.service.exception.UnauthorizedException;
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

import java.util.Map;


@RestController
@RequestMapping("/events")
public class EventController {

    private static final Logger log = LoggerFactory.getLogger(EventController.class);

    private final EventService eventService;

    private final SessionService sessionService;

    private final CookieBuilder cookieBuilder;

    @Autowired
    public EventController(final EventService eventService, final SessionService sessionService, final CookieBuilder cookieBuilder) {
        this.eventService = eventService;
        this.sessionService = sessionService;
        this.cookieBuilder = cookieBuilder;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> create(
            @RequestBody final EventDto dto,
            @CookieValue(name = "${app.session.cookie-name}", required = false) final String sid) {

        final ResponseCookie cookie = cookieBuilder.build(sid);

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
            @CookieValue(name = "${app.session.cookie-name}", required = false) final String sid) {

        final ResponseCookie cookie = cookieBuilder.build(sid);

        final Page<EventDto> page = eventService.findFiltered(
                title, category, price_from, price_to, city,
                date_from, date_to, user, limit, offset);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new EventsResponse(page.getContent(), page.getTotalElements()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> patchEvent(
            @PathVariable(name = "id") final String id,
            @RequestBody final EventPatchDto patchDto,
            @CookieValue(name = "${app.session.cookie-name}", required = false) final String sid) {

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
            @CookieValue(name = "${app.session.cookie-name}", required = false) final String sid) {

        final ResponseCookie cookie = cookieBuilder.build(sid);
        final Event event = eventService.findById(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(EventDto.fromEntity(event));
    }
}
