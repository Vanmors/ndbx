package com.vanmors.ndbx.controller;

import com.vanmors.ndbx.controller.response.EventsResponse;
import com.vanmors.ndbx.dto.EventDto;
import com.vanmors.ndbx.entity.Event;
import com.vanmors.ndbx.service.EventService;
import com.vanmors.ndbx.service.SessionService;
import com.vanmors.ndbx.service.exception.UnauthorizedException;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/events")
public class EventController {

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
    public ResponseEntity<EventDto> create(
            @RequestBody final EventDto dto,
            @CookieValue(name = "${app.session.cookie-name}", required = false) final String sid) {

        if (sid == null || sessionService.getUserIdFromSession(sid).isEmpty()) {
            throw new UnauthorizedException("not authenticated");
        }

        final ResponseCookie cookie = cookieBuilder.build(sid);

        final Event event = eventService.createEvent(dto, sid);
        return ResponseEntity.status(HttpStatus.CREATED).header(HttpHeaders.SET_COOKIE, cookie.toString()).body(EventDto.fromEntity(event));
    }

    @GetMapping
    public ResponseEntity<EventsResponse> findAll(
            @NotNull @RequestParam(required = false) final String title,
            @Min(0) @NotNull @RequestParam(defaultValue = "10") final int limit,
            @Min(0) @NotNull @RequestParam(defaultValue = "0") final int offset,
            @CookieValue(name = "${app.session.cookie-name}", required = false) final String sid) {

        sessionService.getUserIdFromSession(sid).orElseThrow(() -> new UnauthorizedException("not authenticated"));

        final ResponseCookie cookie = cookieBuilder.build(sid);

        final Page<EventDto> page = eventService.findAll(title, limit, offset);
        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new EventsResponse(page.getContent(), page.getTotalElements()));
    }
}
