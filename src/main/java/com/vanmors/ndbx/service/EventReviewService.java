package com.vanmors.ndbx.service;

import com.vanmors.ndbx.dto.ReviewResponseDto;
import com.vanmors.ndbx.dto.ReviewsCountDto;

import java.util.List;
import java.util.UUID;

public interface EventReviewService {
    UUID createReview(String eventId, String comment, int rating, String userId);
    List<ReviewResponseDto> getReviews(String eventId, int limit, int offset);
    long getReviewsCount(String eventId);
    void patchReview(String eventId, String reviewId, Integer rating, String comment, String userId);
    ReviewsCountDto getReviewsSummary(String eventId);
}
