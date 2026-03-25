package com.vanmors.ndbx.service.impl;

import com.vanmors.ndbx.dao.UserRepository;
import com.vanmors.ndbx.dto.RegisterDto;
import com.vanmors.ndbx.entity.User;
import com.vanmors.ndbx.service.SessionService;
import com.vanmors.ndbx.service.UserService;
import com.vanmors.ndbx.service.exception.RegistrationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;

    @Autowired
    public UserServiceImpl(final UserRepository userRepository, final PasswordEncoder passwordEncoder, final SessionService sessionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionService = sessionService;
    }

    @Override
    public User createNewUser(final RegisterDto dto, final String existingSid) {
        if (userRepository.existsByUsername(dto.username())) {
            throw new RegistrationException("user already exists");
        }

        final User user = new User();
        user.setFullName(dto.full_name());
        user.setUsername(dto.username());
        user.setPasswordHashed(passwordEncoder.encode(dto.password()));

        final User saved = userRepository.save(user);

        // Создаём/обновляем сессию и привязываем user_id
        final String sid = sessionService.createOrRefreshSession(existingSid);
        sessionService.attachUserToSession(sid, saved.getId());

        return saved;
    }


}
