package com.vanmors.ndbx.service;

import com.vanmors.ndbx.dto.LoginDto;


public interface AuthService {

    String login(LoginDto dto, String existingSid);

    void logout(String sid);
}
