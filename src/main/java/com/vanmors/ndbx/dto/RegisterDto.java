package com.vanmors.ndbx.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


public record RegisterDto(@NotBlank @NotNull String fullName, @NotBlank @NotNull String username,
                          @Size(min = 8) @NotBlank @NotNull String password) {
}
