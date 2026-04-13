package com.vanmors.ndbx.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


public record RegisterDto(@NotBlank @NotNull String full_name, @NotBlank @NotNull String username, @NotBlank @NotNull String password) {
}
