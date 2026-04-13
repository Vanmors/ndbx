package com.vanmors.ndbx.dto;

import com.vanmors.ndbx.entity.Category;


public record EventPatchDto(
        Category category,
        Long price,
        String city
) {
}