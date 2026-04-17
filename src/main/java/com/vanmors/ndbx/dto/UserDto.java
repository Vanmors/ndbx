package com.vanmors.ndbx.dto;

import com.vanmors.ndbx.entity.User;


public record UserDto(String id, String full_name, String username) {
    public static UserDto fromEntity(final User user) {
        return new UserDto(
                user.getId(),
                user.getFullName(),
                user.getUsername()
        );
    }

}