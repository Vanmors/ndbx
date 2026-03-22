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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;
    private final SessionService sessionService;

    @Autowired
    public EventController(final EventService eventService, final SessionService sessionService) {
        this.eventService = eventService;
        this.sessionService = sessionService;
    }

    @PostMapping
    public ResponseEntity<EventDto> create(
            @RequestBody final EventDto dto,
            @CookieValue(name = "${app.session.cookie-name}", required = false) final String sid) {

        if (sid == null || sessionService.getUserIdFromSession(sid).isEmpty()) {
            throw new UnauthorizedException("not authenticated");
        }

        final Event event = eventService.createEvent(dto, sid);
        return ResponseEntity.status(HttpStatus.CREATED).body(EventDto.fromEntity(event));
    }

    @GetMapping
    public ResponseEntity<EventsResponse> findAll(
            @NotNull @RequestParam(required = false) final String title,
            @Min(0) @NotNull @RequestParam(defaultValue = "10") final int limit,
            @Min(0) @NotNull @RequestParam(defaultValue = "0") final int offset,
            @CookieValue(name = "${app.session.cookie-name}", required = false) final String sid) {

        final Page<EventDto> page = eventService.findAll(title, limit, offset);
        return ResponseEntity.ok(new EventsResponse(page.getContent(), page.getTotalElements()));
    }
}
