package com.vanmors.ndbx.service.impl;

import com.vanmors.ndbx.dao.SessionDao;
import com.vanmors.ndbx.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Optional;


@Service
public class SessionServiceImpl implements SessionService {
    private static final Logger log = LoggerFactory.getLogger(SessionServiceImpl.class);

    private static final SecureRandom secureRandom = new SecureRandom();

    private final RedisTemplate<String, Object> redisTemplate;

    private final SessionDao sessionDao;

    private final String sessionPrefix;

    private final int ttlSeconds;

    @Autowired
    public SessionServiceImpl(
            final RedisTemplate<String, Object> redisTemplate,
            final SessionDao sessionDao,
            @Value("${app.session.key_prefix}") final String sessionPrefix,
            @Value("${app.session.ttl-seconds}") final int ttlSeconds) {
        this.redisTemplate = redisTemplate;
        this.sessionDao = sessionDao;
        this.sessionPrefix = sessionPrefix;
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public String createOrRefreshSession(final String existingSid) {
        String sid = existingSid;

        if (sid == null || sid.isBlank() || !exists(sid)) {
            sid = generateSessionId();
            final String key = sessionPrefix + sid;

            final boolean created = sessionDao.createSession(key, ttlSeconds);

            if (!created) {
                log.warn("Failed to create session for sid: {}", sid);
                return null;
            }

            log.debug("Created new session: {}", sid);
            return sid;
        }
        // Обновляем TTL и updated_at
        final String key = sessionPrefix + sid;
        final boolean refreshed = sessionDao.refreshSession(key, ttlSeconds);

        if (!refreshed) {
            log.warn("Failed to refresh session for sid: {}", sid);
            return null;
        }

        log.debug("Refreshed session: {}", sid);
        return sid;
    }

    /**
     * Возвращает существующую сессию без изменения TTL
     */
    @Override
    public Optional<String> getExistingSessionId(final String sid) {
        if (sid == null || sid.isBlank()) {
            return Optional.empty();
        }

        final String key = sessionPrefix + sid;
        final Boolean exists = redisTemplate.hasKey(key);

        if (exists) {
            return Optional.of(sid);
        }
        return Optional.empty();
    }

    private boolean exists(final String sid) {
        return redisTemplate.hasKey(sessionPrefix + sid);
    }

    private static String generateSessionId() {
        final byte[] randomBytes = new byte[16];
        secureRandom.nextBytes(randomBytes);
        final StringBuilder sb = new StringBuilder(32);
        for (final byte b : randomBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
