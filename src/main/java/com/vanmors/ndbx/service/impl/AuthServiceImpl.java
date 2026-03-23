package com.vanmors.ndbx.service.impl;

import com.vanmors.ndbx.dao.UserRepository;
import com.vanmors.ndbx.dto.LoginDto;
import com.vanmors.ndbx.entity.User;
import com.vanmors.ndbx.service.AuthService;
import com.vanmors.ndbx.service.SessionService;
import com.vanmors.ndbx.service.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;


    @Autowired
    public AuthServiceImpl(final UserRepository userRepository, final PasswordEncoder passwordEncoder, final SessionService sessionService) {
        this.userRepository = userRepository;
        this.passwordEncoder  = passwordEncoder;
        this.sessionService = sessionService;
    }

    @Override
    public String login(final LoginDto dto, final String existingSid) {
        final User user = userRepository.findByUsername(dto.username())
                .orElseThrow(() -> new UnauthorizedException("invalid credentials"));

        if (!passwordEncoder.matches(dto.password(), user.getPasswordHashed())) {
            throw new UnauthorizedException("invalid credentials");
        }

        // Привязываем сессию к пользователю
        final String sid = sessionService.createOrRefreshSession(existingSid);
        sessionService.attachUserToSession(sid, user.getId());
        return sid;
    }

    @Override
    public void logout(final String sid) {
        sessionService.getUserIdFromSession(sid).orElseThrow(() -> new UnauthorizedException("not authenticated"));
        sessionService.deleteSession(sid);
    }
}
