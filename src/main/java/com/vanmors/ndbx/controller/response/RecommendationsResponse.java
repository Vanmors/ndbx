package com.vanmors.ndbx.controller.response;

import com.vanmors.ndbx.dto.EventDto;

import java.util.List;


public record RecommendationsResponse(List<EventDto> events) {}
