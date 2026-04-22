package com.vanmors.ndbx.service.impl;


import com.vanmors.ndbx.dao.EventReactionRepository;
import com.vanmors.ndbx.dto.ReactionsCountDto;
import com.vanmors.ndbx.entity.Event;
import com.vanmors.ndbx.entity.EventReaction;
import com.vanmors.ndbx.service.EventReactionService;
import com.vanmors.ndbx.service.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;


@Service
public class EventReactionServiceImpl implements EventReactionService {
    private static final Logger log = LoggerFactory.getLogger(EventReactionServiceImpl.class);

    private final EventReactionRepository cassandraRepo;

    private final EventService eventService;

    private final StringRedisTemplate redisTemplate;

    @Value("${app.like.ttl-seconds}")
    private long ttl;

    public EventReactionServiceImpl(final EventReactionRepository cassandraRepo, final StringRedisTemplate redisTemplate, final EventService eventService) {
        this.cassandraRepo = cassandraRepo;
        this.redisTemplate = redisTemplate;
        this.eventService = eventService;
    }

    @Override
    public void like(final String eventId, final String userId) {
        final Event event = eventService.findByIdForReaction(eventId);
        saveReaction(eventId, userId, (byte) 1);
        refreshCacheByEvent(event);
    }

    @Override
    public void dislike(final String eventId, final String userId) {
        final Event event = eventService.findByIdForReaction(eventId);
        saveReaction(eventId, userId, (byte) -1);
        refreshCacheByEvent(event);
    }

    @Override
    public ReactionsCountDto getReactions(final String eventId) {
        final Event event = eventService.findByIdForReaction(eventId);
        final String title = event.getTitle();
        final String cacheKey = buildKey(title);

        // Cache-Aside: проверяем Redis
        final Map<Object, Object> cached = redisTemplate.opsForHash().entries(cacheKey);
        if (!cached.isEmpty()) {
            final long likes = Long.parseLong((String) cached.getOrDefault("likes", "0"));
            final long dislikes = Long.parseLong((String) cached.getOrDefault("dislikes", "0"));
            return new ReactionsCountDto(likes, dislikes);
        }

        // Cache miss: считаем из Cassandra
        final ReactionsCountDto counts = countReactionsFromCassandra(title);

        // Кэшируем в Redis с TTL
        if (counts.likes() > 0 || counts.dislikes() > 0) {
            cacheReactions(cacheKey, counts.likes(), counts.dislikes());
        }

        return counts;
    }

    private void saveReaction(final String eventId, final String userId, final byte value) {
        final EventReaction reaction = new EventReaction();
        reaction.setEventId(eventId);
        reaction.setCreatedBy(userId);
        reaction.setLikeValue(value);
        reaction.setCreatedAt(Instant.now());

        cassandraRepo.save(reaction);
    }

    private String buildKey(final String title) {
        return "events:" + md5(title) + ":reactions";
    }

    private String md5(final String input) {
        try {
            final MessageDigest md = MessageDigest.getInstance("MD5");
            final byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));

            final StringBuilder sb = new StringBuilder();
            for (final byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void refreshCacheByEvent(final Event event) {
        final String title = event.getTitle();
        final String cacheKey = buildKey(title);

        final ReactionsCountDto counts = countReactionsFromCassandra(title);
        cacheReactions(cacheKey, counts.likes(), counts.dislikes());
    }

    private ReactionsCountDto countReactionsFromCassandra(final String title) {
        final List<Event> sameTitleEvents = eventService.findAllByTitle(title);
        final List<String> eventIds = sameTitleEvents.stream().map(Event::getId).toList();

        final List<EventReaction> reactions = cassandraRepo.findByEventIdIn(eventIds);

        long likes = 0;
        long dislikes = 0;
        for (final EventReaction r : reactions) {
            if (r.getLikeValue() == 1) {
                likes++;
            } else {
                dislikes++;
            }
        }
        return new ReactionsCountDto(likes, dislikes);
    }

    private void cacheReactions(final String key, final long likes, final long dislikes) {
        final String script = """
                redis.call('HSET', KEYS[1], 'likes', ARGV[1], 'dislikes', ARGV[2])
                redis.call('EXPIRE', KEYS[1], tonumber(ARGV[3]))
                return 1
                """;

        final DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);

        redisTemplate.execute(
                redisScript,
                Collections.singletonList(key),
                String.valueOf(likes),
                String.valueOf(dislikes),
                String.valueOf(ttl)
        );
    }
}
