package com.vanmors.ndbx.controller;

import com.vanmors.ndbx.controller.response.EventsResponse;
import com.vanmors.ndbx.controller.response.UsersResponse;
import com.vanmors.ndbx.dto.EventDto;
import com.vanmors.ndbx.dto.UserDto;
import com.vanmors.ndbx.dto.UserRegistrationDto;
import com.vanmors.ndbx.entity.Category;
import com.vanmors.ndbx.entity.User;
import com.vanmors.ndbx.service.EventReactionService;
import com.vanmors.ndbx.service.EventService;
import com.vanmors.ndbx.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {


    private final UserService userService;

    private final CookieBuilder cookieBuilder;

    private final EventService eventService;

    private final EventReactionService eventReactionService;

    @Autowired
    public UserController(final UserService userService, final CookieBuilder cookieBuilder, final EventService eventService, final EventReactionService eventReactionService) {
        this.userService = userService;
        this.cookieBuilder = cookieBuilder;
        this.eventService = eventService;
        this.eventReactionService = eventReactionService;
    }

    @PostMapping
    public ResponseEntity<Void> register(@Valid @RequestBody final UserRegistrationDto dto,
                                         @CookieValue(name = "${app.session.cookie-name}", required = false) final String sid,
                                         final HttpServletRequest request) {

        final String updatedSid = userService.createNewUser(dto, sid);
        final ResponseCookie cookie = cookieBuilder.build(updatedSid);

        return ResponseEntity.status(HttpStatus.CREATED).header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }

    @GetMapping
    public ResponseEntity<UsersResponse> findUsers(
            @RequestParam(name = "name", required = false) final String name,
            @RequestParam(name = "id", required = false) final String id,
            @Min(0) @RequestParam(name = "limit", defaultValue = "10") final int limit,
            @Min(0) @RequestParam(name = "offset", defaultValue = "0") final int offset,
            @CookieValue(name = "${app.session.cookie-name}", required = false) final String sid) {

        final ResponseCookie cookie = cookieBuilder.build(sid);

        final Page<UserDto> page = userService.findUsers(name, id, limit, offset);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new UsersResponse(page.getContent(), page.getTotalElements()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> findUser(@PathVariable("id") final String id, @CookieValue(name = "${app.session.cookie-name}", required = false) final String sid) {

        final ResponseCookie cookie = cookieBuilder.build(sid);
        final User user = userService.findById(id);

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(UserDto.fromEntity(user));
    }

    @GetMapping("/{id}/events")
    public ResponseEntity<EventsResponse> getUserEvents(
            @PathVariable("id") final String id,
            @RequestParam(name = "title", required = false) final String title,
            @RequestParam(name = "category", required = false) final Category category,
            @RequestParam(name = "price_from", required = false) final Long priceFrom,
            @RequestParam(name = "price_to", required = false) final Long priceTo,
            @RequestParam(name = "city", required = false) final String city,
            @RequestParam(name = "date_from", required = false) final String dateFrom,
            @RequestParam(name = "date_to", required = false) final String dateTo,
            @RequestParam(name = "include", required = false) final String include,
            @Min(0) @RequestParam(name = "limit", defaultValue = "10") final int limit,
            @Min(0) @RequestParam(name = "offset", defaultValue = "0") final int offset,
            @CookieValue(name = "${app.session.cookie-name}", required = false) final String sid
    ) {
        final ResponseCookie cookie = cookieBuilder.build(sid);

        final User user = userService.findById(id);

        final Page<EventDto> page = eventService.findFiltered(
                null, title, category, priceFrom, priceTo, city,
                dateFrom, dateTo, user.getUsername(),
                limit, offset
        );

        List<EventDto> events = page.getContent();
        if ("reactions".equals(include)) {
            events = events.stream()
                    .map(dto -> dto.withReactions(eventReactionService.getReactions(dto.id())))
                    .toList();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new EventsResponse(events, page.getTotalElements()));
    }
}
