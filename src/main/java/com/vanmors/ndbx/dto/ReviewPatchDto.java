package com.vanmors.ndbx.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ReviewPatchDto(
        @Min(1) @Max(5) Integer rating,
        @Size(max = 300) String comment
) {
}
