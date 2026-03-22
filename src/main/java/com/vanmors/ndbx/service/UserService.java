package com.vanmors.ndbx.service;

import com.vanmors.ndbx.dto.RegisterDto;
import com.vanmors.ndbx.entity.User;


public interface UserService {

    User createNewUser(RegisterDto dto, String existingSid);
}
