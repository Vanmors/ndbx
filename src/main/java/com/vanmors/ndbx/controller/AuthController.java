package com.vanmors.ndbx.controller;

import com.vanmors.ndbx.dto.LoginDto;
import com.vanmors.ndbx.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;


@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    private final CookieBuilder cookieBuilder;

    private final String cookieName;
    private final ObjectMapper objectMapper;

    @Autowired
    public AuthController(final AuthService authService, final CookieBuilder cookieBuilder,
                          @Value("${app.session.cookie-name:X-Session-Id}") final String cookieName,
                          ObjectMapper objectMapper) {
        this.authService = authService;
        this.cookieBuilder = cookieBuilder;
        this.cookieName = cookieName;
        this.objectMapper = objectMapper;
    }

    @PostMapping("login")
    public ResponseEntity<Void> login(
            @Valid @RequestBody final LoginDto dto,
            @CookieValue(name = "${app.session.cookie-name}", required = false) final String sid,
            HttpServletRequest request) {


        logRequestBody("POST /auth/login", dto, request);

        final String newSid = authService.login(dto, sid);
        final ResponseCookie cookie = cookieBuilder.build(newSid);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "${app.session.cookie-name}", required = false) final String sid) {

        authService.logout(sid);

        final ResponseCookie cookie = ResponseCookie.from(cookieName, sid)
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    private void logRequestBody(String endpoint, Object body, HttpServletRequest request) {
        try {
            String json = objectMapper.writeValueAsString(body);
            log.info("[REQUEST] {} | Body: {}", endpoint, json);
        } catch (Exception e) {
            log.warn("[REQUEST] {} | Failed to serialize body", endpoint);
        }
    }
}
