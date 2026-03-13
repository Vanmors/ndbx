package com.vanmors.ndbx.dao.impl;

import com.vanmors.ndbx.dao.SessionDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisHashCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;


@Repository
public class SessionDaoImpl implements SessionDao {

    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public SessionDaoImpl(final RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Boolean refreshSession(final String key, final int ttlSeconds) {
        return redisTemplate.execute((final RedisConnection conn) -> {
            final byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);

            final Map<byte[], byte[]> byteMap = new HashMap<>();
            final byte[] now = Instant.now().toString().getBytes(StandardCharsets.UTF_8);
            byteMap.put("updated_at".getBytes(StandardCharsets.UTF_8), now);

            return conn.hashCommands().hSetEx(
                    keyBytes,
                    byteMap,
                    RedisHashCommands.HashFieldSetOption.IF_ALL_EXIST,
                    Expiration.seconds(ttlSeconds)
            );
        });
    }

    public Boolean createSession(final String key, final int ttlSeconds) {
        return redisTemplate.execute((final RedisConnection conn) -> {
            final byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);

            final Map<byte[], byte[]> byteMap = new HashMap<>();
            final byte[] now = Instant.now().toString().getBytes(StandardCharsets.UTF_8);
            byteMap.put("created_at".getBytes(StandardCharsets.UTF_8), now);
            byteMap.put("updated_at".getBytes(StandardCharsets.UTF_8), now);

            return conn.hashCommands().hSetEx(
                    keyBytes,
                    byteMap,
                    RedisHashCommands.HashFieldSetOption.IF_NONE_EXIST,
                    Expiration.seconds(ttlSeconds)
            );
        });
    }

}
