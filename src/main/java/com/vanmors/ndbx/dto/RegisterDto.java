package com.vanmors.ndbx.dto;

import jakarta.validation.constraints.NotNull;


public record RegisterDto(@NotNull String fullName, @NotNull String username, @NotNull String password) {}
