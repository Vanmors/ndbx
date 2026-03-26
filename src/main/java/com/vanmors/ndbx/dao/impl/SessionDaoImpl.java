package com.vanmors.ndbx.dao.impl;

import com.vanmors.ndbx.dao.SessionDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collections;


@Repository
public class SessionDaoImpl implements SessionDao {

    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public SessionDaoImpl(final RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void refreshSession(final String key, final int ttlSeconds) {

        final String refreshSession = """
                if redis.call('EXISTS', KEYS[1]) == 0 then
                    return 0
                end
                redis.call('HSET', KEYS[1], 'updated_at', ARGV[1])
                redis.call('EXPIRE', KEYS[1], tonumber(ARGV[2]))
                return 1
                """;

        final DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(refreshSession);
        script.setResultType(Long.class);

        redisTemplate.execute(
                script,
                Collections.singletonList(key),
                Instant.now().toString(),
                String.valueOf(ttlSeconds)
        );
    }

    public void attachToUser(String key, String userId, int ttlSeconds) {
        final String attachToUser = """
                if redis.call('EXISTS', KEYS[1]) == 0 then
                    return 0
                end
                redis.call('HSET', KEYS[1], 'user_id', ARGV[1], 'updated_at', ARGV[2])
                redis.call('EXPIRE', KEYS[1], tonumber(ARGV[3]))
                return 1
                """;


        final DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(attachToUser);
        script.setResultType(Long.class);

        redisTemplate.execute(
                script,
                Collections.singletonList(key),
                userId,
                Instant.now().toString(),
                String.valueOf(ttlSeconds)
        );
    }

    public void createSession(final String key, final int ttlSeconds) {
        final String createSession = """ 
                if redis.call('EXISTS', KEYS[1]) == 1 then
                return 0
                end
                redis.call('HSET', KEYS[1], 'created_at', ARGV[1], 'updated_at', ARGV[1])
                redis.call('EXPIRE', KEYS[1], tonumber(ARGV[2]))
                return 1
                """;

        final DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(createSession);
        script.setResultType(Long.class);

        redisTemplate.execute(
                script,
                Collections.singletonList(key),
                Instant.now().toString(),
                String.valueOf(ttlSeconds)
        );
    }

}
