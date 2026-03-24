package com.vanmors.ndbx.controller;

import com.vanmors.ndbx.dto.LoginDto;
import com.vanmors.ndbx.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    private final CookieBuilder cookieBuilder;

    private final String cookieName;

    @Autowired
    public AuthController(final AuthService authService, final CookieBuilder cookieBuilder,
                          @Value("${app.session.cookie-name:X-Session-Id}") final String cookieName) {
        this.authService = authService;
        this.cookieBuilder = cookieBuilder;
        this.cookieName = cookieName;
    }

    @PostMapping("login")
    public ResponseEntity<Void> login(
            @Valid @RequestBody final LoginDto dto,
            @CookieValue(name = "${app.session.cookie-name}", required = false) final String sid) {

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
}
