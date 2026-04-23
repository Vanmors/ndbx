package com.vanmors.ndbx.controller.response;


import com.vanmors.ndbx.dto.UserDto;

import java.util.List;


public record UsersResponse(
        List<UserDto> users,
        long count
) {
}
