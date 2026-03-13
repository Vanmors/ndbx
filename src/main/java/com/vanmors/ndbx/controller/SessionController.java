package com.vanmors.ndbx.controller;

import com.vanmors.ndbx.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;


@RestController
public class SessionController {

    private final SessionService sessionService;

    private final String cookieName;

    private final long ttlSeconds;

    @Autowired
    public SessionController(
            final SessionService sessionService,
            @Value("${app.session.cookie-name:X-Session-Id}") final String cookieName,
            @Value("${app.session.ttl-seconds}") final int ttlSeconds) {
        this.sessionService = sessionService;
        this.cookieName = cookieName;
        this.ttlSeconds = ttlSeconds;
    }

    @PostMapping("/session")
    public ResponseEntity<Void> handleSession(@CookieValue(name = "${app.session.cookie-name:X-Session-Id}", required = false) final String sessionId) {

        final String newOrUpdatedSid = sessionService.createOrRefreshSession(sessionId);

        final ResponseCookie cookie = ResponseCookie.from(cookieName, newOrUpdatedSid)
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofSeconds(ttlSeconds))
                .sameSite("Lax")
                .build();

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
                final ResponseCookie cookie = ResponseCookie.from(cookieName, existing.get())
                        .httpOnly(true)
                        .path("/")
                        .maxAge(Duration.ofSeconds(ttlSeconds))
                        .sameSite("Lax")
                        .build();

                return ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, cookie.toString())
                        .body(response);
            }
        }
        return ResponseEntity.ok(response);
    }
}
