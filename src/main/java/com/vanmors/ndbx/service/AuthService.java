package com.vanmors.ndbx.service;

import com.vanmors.ndbx.dto.LoginDto;


public interface AuthService {

    void login(LoginDto dto, String existingSid);

    void logout(String sid);
}
