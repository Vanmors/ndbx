package com.vanmors.ndbx.service;

import com.vanmors.ndbx.dto.ReactionsCountDto;


public interface EventReactionService {
    void like(String eventId, String userId);
    void dislike(String eventId, String userId);
    ReactionsCountDto getReactions(String eventId);
}
