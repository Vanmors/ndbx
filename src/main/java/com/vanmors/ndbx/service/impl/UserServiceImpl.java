package com.vanmors.ndbx.service.impl;

import com.vanmors.ndbx.dao.UserNodeRepository;
import com.vanmors.ndbx.dao.UserRepository;
import com.vanmors.ndbx.dto.UserDto;
import com.vanmors.ndbx.dto.UserRegistrationDto;
import com.vanmors.ndbx.entity.User;
import com.vanmors.ndbx.service.SessionService;
import com.vanmors.ndbx.service.UserService;
import com.vanmors.ndbx.service.exception.RegistrationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.regex.Pattern;


@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;

    private final UserNodeRepository userNodeRepository;

    private final PasswordEncoder passwordEncoder;

    private final SessionService sessionService;

    private final MongoTemplate mongoTemplate;

    @Autowired
    public UserServiceImpl(final UserRepository userRepository,
                           final UserNodeRepository userNodeRepository,
                           final PasswordEncoder passwordEncoder,
                           final SessionService sessionService,
                           final MongoTemplate mongoTemplate) {
        this.userRepository = userRepository;
        this.userNodeRepository = userNodeRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionService = sessionService;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public String createNewUser(final UserRegistrationDto dto, final String existingSid) {
        if (userRepository.existsByUsername(dto.username())) {
            throw new RegistrationException("user already exists");
        }

        final User user = new User();
        user.setFullName(dto.full_name());
        user.setUsername(dto.username());
        user.setPasswordHashed(passwordEncoder.encode(dto.password()));

        final User saved = userRepository.save(user);

        userNodeRepository.mergeByMongoId(saved.getId());

        // Создаём/обновляем сессию и привязываем user_id
        final String sid = sessionService.createOrRefreshSession(existingSid);
        sessionService.attachUserToSession(sid, saved.getId());

        return sid;
    }

    @Override
    public User findById(final String id) {
        return userRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Not found"));
    }

    @Override
    public Optional<User> findByUsername(final String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Page<UserDto> findUsers(final String name, final String id, final int limit, final int offset) {

        log.info("params: id={} name={}", id, name);

        final Query query = new Query();

        if (StringUtils.hasText(name)) {
            query.addCriteria(Criteria.where("full_name").regex(Pattern.quote(name), "i"));
        }

        if (StringUtils.hasText(id)) {
            query.addCriteria(Criteria.where("_id").is(id));
        }

        // Пагинация
        query.skip(offset).limit(limit);

        final List<User> users = mongoTemplate.find(query, User.class);

        final List<UserDto> dtos = users.stream()
                .map(UserDto::fromEntity)
                .toList();

        return new PageImpl<>(dtos, PageRequest.of(0, limit), users.size());
    }

}
