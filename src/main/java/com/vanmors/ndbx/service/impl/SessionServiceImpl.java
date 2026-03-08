package com.vanmors.ndbx.service.impl;

import com.vanmors.ndbx.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class SessionServiceImpl implements SessionService {
    private static final Logger log = LoggerFactory.getLogger(SessionServiceImpl.class);

    private static final SecureRandom secureRandom = new SecureRandom();

    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();

    private final RedisTemplate<String, Object> redisTemplate;
    private final String sessionPrefix;
    private final long ttlSeconds;

    @Autowired
    public SessionServiceImpl(
            final RedisTemplate<String, Object> redisTemplate,
            @Value("${app.session.prefix}") final String sessionPrefix,
            @Value("${app.session.ttl-seconds}") final long ttlSeconds) {
        this.redisTemplate = redisTemplate;
        this.sessionPrefix = sessionPrefix;
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public String createOrRefreshSession(final String existingSid) {
        String sid = existingSid;

        if (sid == null || sid.isBlank() || !exists(sid)) {
            sid = generateSessionId();
            final String key = sessionPrefix + sid;

            final Map<String, String> data = new HashMap<>();
            final String now = Instant.now().toString();
            data.put("created_at", now);
            data.put("updated_at", now);

            redisTemplate.opsForHash().putAll(key, data);
            redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);

            log.debug("Created new session: {}", sid);
            return sid;
        } else {
            // Обновляем TTL и updated_at
            final String key = sessionPrefix + sid;
            redisTemplate.opsForHash().put(key, "updated_at", Instant.now().toString());
            redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);

            log.debug("Refreshed session: {}", sid);
            return sid;
        }
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
        new SecureRandom().nextBytes(randomBytes);
        final StringBuilder sb = new StringBuilder(32);
        for (final byte b : randomBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
