package com.vanmors.ndbx.service;

import com.vanmors.ndbx.dto.EventDto;

import java.util.List;


public interface RecommendationService {

    List<EventDto> getRecommendations(String userId);
}
