package com.vanmors.ndbx.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;


@Component
public class CookieBuilder {

    private final String cookieName;

    private final long ttlSeconds;

    @Autowired
    public CookieBuilder(
            @Value("${app.session.cookie-name:X-Session-Id}") final String cookieName,
            @Value("${app.session.ttl-seconds}") final int ttlSeconds
    ) {
        this.cookieName = cookieName;
        this.ttlSeconds = ttlSeconds;
    }


    public ResponseCookie build(final String sid) {
        return ResponseCookie.from(cookieName, sid)
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofSeconds(ttlSeconds))
                .sameSite("Lax")
                .build();
    }

}
