package com.vanmors.ndbx.controller;

import com.vanmors.ndbx.dto.RegisterDto;
import com.vanmors.ndbx.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public UserController(final UserService userService, final CookieBuilder cookieBuilder) {
        this.userService = userService;
        this.cookieBuilder = cookieBuilder;
    }

    @PostMapping
    public ResponseEntity<Void> register(@Valid @RequestBody final RegisterDto dto,
                                         @CookieValue(name = "${app.session.cookie-name}", required = false) final String sid,
                                         HttpServletRequest request) {

        final String updatedSid = userService.createNewUser(dto, sid);
        final ResponseCookie cookie = cookieBuilder.build(updatedSid);

        return ResponseEntity.status(HttpStatus.CREATED).header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }

}
