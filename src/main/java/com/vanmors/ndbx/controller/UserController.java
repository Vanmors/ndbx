package com.vanmors.ndbx.controller;

import com.vanmors.ndbx.dto.RegisterDto;
import com.vanmors.ndbx.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;


@RestController
@RequestMapping("/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    private final CookieBuilder cookieBuilder;
    private final ObjectMapper objectMapper;

    @Autowired
    public UserController(final UserService userService, final CookieBuilder cookieBuilder, ObjectMapper objectMapper) {
        this.userService = userService;
        this.cookieBuilder = cookieBuilder;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<Void> register(@Valid @RequestBody final RegisterDto dto,
                                         @CookieValue(name = "${app.session.cookie-name}", required = false) final String sid,
                                         HttpServletRequest request) {

        logRequestBody("POST /users", dto, request);

        final String updatedSid = userService.createNewUser(dto, sid);
        final ResponseCookie cookie = cookieBuilder.build(updatedSid);

        return ResponseEntity.status(HttpStatus.CREATED).header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }

    private void logRequestBody(String endpoint, Object body, HttpServletRequest request) {
        try {
            String json = objectMapper.writeValueAsString(body);
            log.info("[REQUEST] {} | Body: {}", endpoint, json);
        } catch (Exception e) {
            log.warn("[REQUEST] {} | Failed to log body", endpoint);
        }
    }

}
