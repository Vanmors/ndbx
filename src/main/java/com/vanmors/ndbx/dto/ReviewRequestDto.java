package com.vanmors.ndbx.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewRequestDto(
        @NotNull @Size(max = 300) String comment,
        @NotNull @Min(1) @Max(5) Integer rating
) {
}
