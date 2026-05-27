package com.vanmors.ndbx.controller;

import com.vanmors.ndbx.controller.response.RecommendationsResponse;
import com.vanmors.ndbx.dto.EventDto;
import com.vanmors.ndbx.service.RecommendationService;
import com.vanmors.ndbx.service.SessionService;
import com.vanmors.ndbx.service.exception.UnauthorizedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping("/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final SessionService sessionService;
    private final CookieBuilder cookieBuilder;

    public RecommendationController(final RecommendationService recommendationService,
                                    final SessionService sessionService,
                                    final CookieBuilder cookieBuilder) {
        this.recommendationService = recommendationService;
        this.sessionService = sessionService;
        this.cookieBuilder = cookieBuilder;
    }

    @GetMapping
    public ResponseEntity<RecommendationsResponse> getRecommendations(
            @CookieValue(name = "${app.session.cookie-name}", required = false) final String sid) {

        final Optional<String> userId = sessionService.getUserIdFromSession(sid);
        if (sid == null || userId.isEmpty()) {
            throw new UnauthorizedException("not authenticated");
        }

        final ResponseCookie cookie = cookieBuilder.build(sid);

        final List<EventDto> events = recommendationService.getRecommendations(userId.get());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new RecommendationsResponse(events));
    }
}
