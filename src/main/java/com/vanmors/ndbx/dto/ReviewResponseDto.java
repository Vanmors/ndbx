package com.vanmors.ndbx.dto;

import com.vanmors.ndbx.entity.EventReview;

import java.time.Instant;

public record ReviewResponseDto(
        String id,
        String event_id,
        String comment,
        Instant created_at,
        String created_by,
        int rating,
        Instant updated_at
) {
    public static ReviewResponseDto fromEntity(final EventReview review) {
        return new ReviewResponseDto(
                review.getId().toString(),
                review.getEventId(),
                review.getComment(),
                review.getCreatedAt(),
                review.getCreatedBy(),
                review.getRating(),
                review.getUpdatedAt()
        );
    }
}
