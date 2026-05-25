package com.vanmors.ndbx.controller.response;

import com.vanmors.ndbx.dto.ReviewResponseDto;

import java.util.List;

public record ReviewsResponse(List<ReviewResponseDto> reviews, long count) {
}
