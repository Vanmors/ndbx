package com.vanmors.ndbx.service;

import com.vanmors.ndbx.dto.UserDto;
import com.vanmors.ndbx.dto.UserRegistrationDto;
import com.vanmors.ndbx.entity.User;
import org.springframework.data.domain.Page;

import java.util.Optional;


public interface UserService {

    String createNewUser(UserRegistrationDto dto, String existingSid);

    User findById(String id);

    Optional<User> findByUsername(String username);

    Page<UserDto> findUsers(String name, String id, int limit, int offset);
}
