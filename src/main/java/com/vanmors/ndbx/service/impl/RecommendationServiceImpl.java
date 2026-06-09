package com.vanmors.ndbx.service.impl;

import com.vanmors.ndbx.dao.EventNodeRepository;
import com.vanmors.ndbx.dao.EventRepository;
import com.vanmors.ndbx.dto.EventDto;
import com.vanmors.ndbx.entity.Event;
import com.vanmors.ndbx.service.RecommendationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationServiceImpl.class);

    private static final String CACHE_KEY_PATTERN = "user:%s:recomms";
    private static final String CACHE_FIELD_EVENTS = "events";

    private final EventNodeRepository eventNodeRepository;
    private final EventRepository eventRepository;
    private final JedisPooled jedis;
    private final ObjectMapper objectMapper;

    @Value("${app.recommendations.ttl-seconds}")
    private long recommendationsTtlSeconds;

    public RecommendationServiceImpl(final EventNodeRepository eventNodeRepository,
                                     final EventRepository eventRepository,
                                     final JedisPooled jedis,
                                     final ObjectMapper objectMapper) {
        this.eventNodeRepository = eventNodeRepository;
        this.eventRepository = eventRepository;
        this.jedis = jedis;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<EventDto> getRecommendations(final String userId) {
        final String cacheKey = CACHE_KEY_PATTERN.formatted(userId);

        final List<EventDto> cached = readFromCache(cacheKey);
        if (cached != null) {
            return cached;
        }

        final List<EventNodeRepository.RecommendationResult> recommendations =
                eventNodeRepository.findRecommendations(userId);

        if (recommendations.isEmpty()) {
            storeInCache(cacheKey, List.of());
            return List.of();
        }

        final Map<String, Long> scoreMap = recommendations.stream()
                .collect(Collectors.toMap(
                        EventNodeRepository.RecommendationResult::getEventId,
                        EventNodeRepository.RecommendationResult::getScore,
                        Long::max));

        final List<String> eventIds = recommendations.stream()
                .map(EventNodeRepository.RecommendationResult::getEventId)
                .toList();

        final List<Event> events = eventRepository.findAllById(eventIds);

        // Deduplicate by title — keep nearest by started_at
        final Instant now = Instant.now();
        final Map<String, Event> dedupedByTitle = new LinkedHashMap<>();
        final Map<String, Long> titleScores = new HashMap<>();

        for (final Event event : events) {
            final String title = event.getTitle();
            final long score = scoreMap.getOrDefault(event.getId(), 0L);
            titleScores.merge(title, score, Long::sum);

            if (!dedupedByTitle.containsKey(title)) {
                dedupedByTitle.put(title, event);
            } else {
                final Event existing = dedupedByTitle.get(title);
                if (isCloserToNow(event, existing, now)) {
                    dedupedByTitle.put(title, event);
                }
            }
        }

        // Sort by total likes descending
        final List<EventDto> result = dedupedByTitle.values().stream()
                .sorted((a, b) -> Long.compare(
                        titleScores.getOrDefault(b.getTitle(), 0L),
                        titleScores.getOrDefault(a.getTitle(), 0L)))
                .map(EventDto::fromEntity)
                .toList();

        storeInCache(cacheKey, result);
        return result;
    }

    private boolean isCloserToNow(final Event candidate, final Event existing, final Instant now) {
        if (candidate.getStartedAt() == null) {
            return false;
        }
        if (existing.getStartedAt() == null) {
            return true;
        }
        final long diffCandidate = Math.abs(Duration.between(now, candidate.getStartedAt()).toMillis());
        final long diffExisting = Math.abs(Duration.between(now, existing.getStartedAt()).toMillis());
        return diffCandidate < diffExisting;
    }

    private List<EventDto> readFromCache(final String cacheKey) {
        try {
            final String json = jedis.hget(cacheKey, CACHE_FIELD_EVENTS);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, new TypeReference<List<EventDto>>() {});
        } catch (final Exception e) {
            log.warn("Failed to read recommendations cache for key {}", cacheKey, e);
            return null;
        }
    }

    private void storeInCache(final String cacheKey, final List<EventDto> events) {
        try {
            final String json = objectMapper.writeValueAsString(events);
            jedis.hset(cacheKey, Map.of(CACHE_FIELD_EVENTS, json));
            jedis.expire(cacheKey, recommendationsTtlSeconds);
        } catch (final Exception e) {
            log.warn("Failed to write recommendations cache for key {}", cacheKey, e);
        }
    }
}
