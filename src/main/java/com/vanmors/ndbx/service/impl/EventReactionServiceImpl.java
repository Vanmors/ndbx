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
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;


@Service
public class EventReactionServiceImpl implements EventReactionService {
    private static final Logger log = LoggerFactory.getLogger(EventReactionServiceImpl.class);

    private static final String CACHE_KEY_PATTERN = "events:%s:reactions";
    private static final String CACHE_COMPAT_KEY_PATTERN = "event:%s:reactions";
    private static final String CACHE_FIELD_LIKES = "likes";
    private static final String CACHE_FIELD_DISLIKES = "dislikes";

    private final EventReactionRepository cassandraRepo;
    private final EventService eventService;
    private final JedisPooled reactionsCache;

    @Value("${app.like.ttl-seconds}")
    private long likeTtlSeconds;

    public EventReactionServiceImpl(final EventReactionRepository cassandraRepo,
                                    final JedisPooled reactionsCache,
                                    final EventService eventService) {
        this.cassandraRepo = cassandraRepo;
        this.reactionsCache = reactionsCache;
        this.eventService = eventService;
    }

    @Override
    public void like(final String eventId, final String userId) {
        final Event event = eventService.findByIdForReaction(eventId);
        saveReaction(eventId, userId, (byte) 1);
        refreshReactionsCacheByTitle(event.getTitle());
    }

    @Override
    public void dislike(final String eventId, final String userId) {
        final Event event = eventService.findByIdForReaction(eventId);
        saveReaction(eventId, userId, (byte) -1);
        refreshReactionsCacheByTitle(event.getTitle());
    }

    @Override
    public ReactionsCountDto getReactions(final String eventId) {
        final Event event = eventService.findByIdForReaction(eventId);
        final String title = event.getTitle();

        final ReactionsCountDto cached = readReactionsFromCache(title);
        if (cached != null) {
            return cached;
        }

        final ReactionsCountDto counts = countReactionsFromCassandra(title);

        if (counts.likes() > 0 || counts.dislikes() > 0) {
            storeReactionsInCache(title, counts);
        }

        return counts;
    }

    // --- Cassandra ---

    private void saveReaction(final String eventId, final String userId, final byte value) {
        final EventReaction reaction = new EventReaction();
        reaction.setEventId(eventId);
        reaction.setCreatedBy(userId);
        reaction.setLikeValue(value);
        reaction.setCreatedAt(Instant.now());
        cassandraRepo.save(reaction);
    }

    private ReactionsCountDto countReactionsFromCassandra(final String title) {
        final List<Event> sameTitleEvents = eventService.findAllByTitle(title);

        long likes = 0;
        long dislikes = 0;
        for (final Event event : sameTitleEvents) {
            final List<EventReaction> reactions = cassandraRepo.findByEventId(event.getId());
            for (final EventReaction r : reactions) {
                if (r.getLikeValue() == 1) {
                    likes++;
                } else if (r.getLikeValue() == -1) {
                    dislikes++;
                }
            }
        }
        return new ReactionsCountDto(likes, dislikes);
    }

    // --- Redis cache ---

    private ReactionsCountDto readReactionsFromCache(final String title) {
        final String primaryKey = cacheKey(title);
        ReactionsCountDto reactions = readFromCacheKey(primaryKey);
        if (reactions != null) {
            return reactions;
        }

        final String compatKey = compatCacheKey(title);
        reactions = readFromCacheKey(compatKey);
        if (reactions != null) {
            storeInCacheByKey(primaryKey, reactions);
        }

        return reactions;
    }

    private ReactionsCountDto readFromCacheKey(final String cacheKey) {
        final Map<String, String> values = reactionsCache.hgetAll(cacheKey);
        if (values == null || values.isEmpty()) {
            return null;
        }

        final String likesStr = values.get(CACHE_FIELD_LIKES);
        final String dislikesStr = values.get(CACHE_FIELD_DISLIKES);
        if (likesStr == null || dislikesStr == null) {
            return null;
        }

        try {
            return new ReactionsCountDto(Long.parseLong(likesStr.trim()), Long.parseLong(dislikesStr.trim()));
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    private void storeReactionsInCache(final String title, final ReactionsCountDto reactions) {
        storeInCacheByKey(cacheKey(title), reactions);
        storeInCacheByKey(compatCacheKey(title), reactions);
    }

    private void storeInCacheByKey(final String cacheKey, final ReactionsCountDto reactions) {
        try {
            final Map<String, String> values = Map.of(
                    CACHE_FIELD_LIKES, String.valueOf(reactions.likes()),
                    CACHE_FIELD_DISLIKES, String.valueOf(reactions.dislikes())
            );
            reactionsCache.hset(cacheKey, values);
            reactionsCache.expire(cacheKey, likeTtlSeconds);
        } catch (final Exception e) {
            log.warn("Failed to write reactions cache for key {}", cacheKey, e);
        }
    }

    private void refreshReactionsCacheByTitle(final String title) {
        if (title == null || title.isBlank()) {
            return;
        }

        final ReactionsCountDto counts = countReactionsFromCassandra(title);
        if (counts.likes() > 0 || counts.dislikes() > 0) {
            storeReactionsInCache(title, counts);
        } else {
            invalidateCache(title);
        }
    }

    private void invalidateCache(final String title) {
        try {
            reactionsCache.del(cacheKey(title), compatCacheKey(title));
        } catch (final Exception e) {
            log.warn("Failed to invalidate reactions cache for title {}", title, e);
        }
    }

    // --- Keys ---

    private static String cacheKey(final String title) {
        return CACHE_KEY_PATTERN.formatted(md5Hex(title));
    }

    private static String compatCacheKey(final String title) {
        return CACHE_COMPAT_KEY_PATTERN.formatted(md5Hex(title));
    }

    private static String md5Hex(final String value) {
        try {
            final MessageDigest md5 = MessageDigest.getInstance("MD5");
            final byte[] digest = md5.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 is not available", e);
        }
    }
}
