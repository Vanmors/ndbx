package com.vanmors.ndbx.controller.response;

import com.vanmors.ndbx.dto.EventDto;

import java.util.List;


public record EventsResponse(List<EventDto> events, long count) {}
