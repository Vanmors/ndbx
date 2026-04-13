package com.vanmors.ndbx.controller;

import com.vanmors.ndbx.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;


@RestController
public class SessionController {

    private final SessionService sessionService;

    private final CookieBuilder cookieBuilder;

    @Autowired
    public SessionController(final SessionService sessionService, final CookieBuilder cookieBuilder) {
        this.sessionService = sessionService;
        this.cookieBuilder = cookieBuilder;
    }

    @PostMapping("/session")
    public ResponseEntity<Void> handleSession(@CookieValue(name = "${app.session.cookie-name:X-Session-Id}", required = false) final String sessionId) {

        final String newOrUpdatedSid = sessionService.createOrRefreshSession(sessionId);

        final ResponseCookie cookie = cookieBuilder.build(newOrUpdatedSid);

        final HttpStatus status = sessionId != null && sessionId.equals(newOrUpdatedSid) ? HttpStatus.OK : HttpStatus.CREATED;

        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health(
            @CookieValue(name = "${app.session.cookie-name:X-Session-Id}", required = false) final String sessionId) {

        final Map<String, String> response = Map.of("status", "ok");

        if (sessionId != null) {
            final Optional<String> existing = sessionService.getExistingSessionId(sessionId);
            if (existing.isPresent()) {
                final ResponseCookie cookie = cookieBuilder.build(existing.get());

                return ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, cookie.toString())
                        .body(response);
            }
        }
        return ResponseEntity.ok(response);
    }
}
