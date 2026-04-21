package com.vanmors.ndbx.controller;

import com.vanmors.ndbx.controller.response.EventsResponse;
import com.vanmors.ndbx.controller.response.UsersResponse;
import com.vanmors.ndbx.dto.EventDto;
import com.vanmors.ndbx.dto.UserDto;
import com.vanmors.ndbx.dto.UserRegistrationDto;
import com.vanmors.ndbx.entity.User;
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


@RestController
@RequestMapping("/users")
public class UserController {


    private final UserService userService;

    private final CookieBuilder cookieBuilder;

    private final EventService eventService;

    @Autowired
    public UserController(final UserService userService, final CookieBuilder cookieBuilder, final EventService eventService) {
        this.userService = userService;
        this.cookieBuilder = cookieBuilder;
        this.eventService = eventService;
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
            @PathVariable(name = "id") final String id,
            @Min(0) @RequestParam(name = "limit", defaultValue = "10") final int limit,
            @Min(0) @RequestParam(name = "offset", defaultValue = "0") final int offset,
            @CookieValue(name = "${app.session.cookie-name}", required = false) final String sid) {

        final ResponseCookie cookie = cookieBuilder.build(sid);

        final Page<EventDto> page = eventService.findByUser(id, limit, offset);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new EventsResponse(page.getContent(), page.getTotalElements()));
    }
}
