package com.vanmors.ndbx.controller;

import com.vanmors.ndbx.dto.LoginDto;
import com.vanmors.ndbx.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(final AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("login")
    public ResponseEntity<Void> login(
            @RequestBody final LoginDto dto,
            @CookieValue(name = "${app.session.cookie-name}", required = false) final String sid) {

        authService.login(dto, sid);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "${app.session.cookie-name}", required = false) final String sid) {

        if (sid != null) {
            authService.logout(sid);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, "X-Session-Id=; Max-Age=0; Path=/; HttpOnly")
                .build();
    }
}
