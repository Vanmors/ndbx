package com.vanmors.ndbx.service.impl;

import com.vanmors.ndbx.dao.EventReviewRepository;
import com.vanmors.ndbx.dto.ReviewResponseDto;
import com.vanmors.ndbx.dto.ReviewsCountDto;
import com.vanmors.ndbx.entity.Event;
import com.vanmors.ndbx.entity.EventReview;
import com.vanmors.ndbx.service.EventReviewService;
import com.vanmors.ndbx.service.EventService;
import com.vanmors.ndbx.service.exception.AlreadyExistsException;
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
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class EventReviewServiceImpl implements EventReviewService {

    private static final Logger log = LoggerFactory.getLogger(EventReviewServiceImpl.class);

    private static final String CACHE_KEY_PATTERN = "event:%s:reviews";
    private static final String CACHE_FIELD_COUNT = "count";
    private static final String CACHE_FIELD_RATING = "rating";

    private final EventReviewRepository cassandraRepo;
    private final EventService eventService;
    private final JedisPooled reviewsCache;

    @Value("${app.review.ttl-seconds}")
    private long reviewTtlSeconds;

    public EventReviewServiceImpl(final EventReviewRepository cassandraRepo,
                                  final JedisPooled reviewsCache,
                                  final EventService eventService) {
        this.cassandraRepo = cassandraRepo;
        this.reviewsCache = reviewsCache;
        this.eventService = eventService;
    }

    @Override
    public UUID createReview(final String eventId, final String comment, final int rating, final String userId) {
        final Event event = eventService.findByIdForReaction(eventId);

        final List<EventReview> existing = cassandraRepo.findByEventId(eventId);
        final boolean alreadyReviewed = existing.stream()
                .anyMatch(r -> userId.equals(r.getCreatedBy()));
        if (alreadyReviewed) {
            throw new AlreadyExistsException("Already exists");
        }

        final Instant now = Instant.now();
        final UUID id = UUID.randomUUID();

        final EventReview review = new EventReview();
        review.setId(id);
        review.setEventId(eventId);
        review.setComment(comment);
        review.setRating((byte) rating);
        review.setCreatedBy(userId);
        review.setCreatedAt(now);
        review.setUpdatedAt(now);

        cassandraRepo.save(review);
        refreshReviewsCacheByTitle(event.getTitle());

        return id;
    }

    @Override
    public List<ReviewResponseDto> getReviews(final String eventId, final int limit, final int offset) {
        final List<EventReview> reviews = cassandraRepo.findByEventId(eventId);

        return reviews.stream()
                .skip(offset)
                .limit(limit)
                .map(ReviewResponseDto::fromEntity)
                .toList();
    }

    @Override
    public long getReviewsCount(final String eventId) {
        final List<EventReview> reviews = cassandraRepo.findByEventId(eventId);
        return reviews.size();
    }

    @Override
    public void patchReview(final String eventId, final String reviewId, final Integer rating,
                            final String comment, final String userId) {
        eventService.findByIdForReaction(eventId);

        final UUID reviewUuid;
        try {
            reviewUuid = UUID.fromString(reviewId);
        } catch (final IllegalArgumentException e) {
            throw new NoSuchElementException("Event not found");
        }

        final List<EventReview> reviews = cassandraRepo.findByEventId(eventId);
        final EventReview review = reviews.stream()
                .filter(r -> reviewUuid.equals(r.getId()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Event not found"));

        if (!userId.equals(review.getCreatedBy())) {
            throw new NoSuchElementException("Event not found");
        }

        if (rating != null) {
            review.setRating(rating.byteValue());
        }
        if (comment != null) {
            review.setComment(comment);
        }
        review.setUpdatedAt(Instant.now());

        cassandraRepo.save(review);

        final Event event = eventService.findByIdForReaction(eventId);
        refreshReviewsCacheByTitle(event.getTitle());
    }

    @Override
    public ReviewsCountDto getReviewsSummary(final String eventId) {
        final Event event = eventService.findByIdForReaction(eventId);
        final String title = event.getTitle();

        final ReviewsCountDto cached = readReviewsFromCache(title);
        if (cached != null) {
            return cached;
        }

        final ReviewsCountDto counts = computeReviewsFromCassandra(title);
        storeReviewsInCache(title, counts);

        return counts;
    }

    private ReviewsCountDto computeReviewsFromCassandra(final String title) {
        final List<Event> sameTitleEvents = eventService.findAllByTitle(title);

        long count = 0;
        double ratingSum = 0;

        for (final Event event : sameTitleEvents) {
            final List<EventReview> reviews = cassandraRepo.findByEventId(event.getId());
            for (final EventReview r : reviews) {
                count++;
                ratingSum += r.getRating();
            }
        }

        if (count == 0) {
            return new ReviewsCountDto(0, 0.0);
        }

        final double avgRating = Math.round(ratingSum / count * 10.0) / 10.0;
        return new ReviewsCountDto(count, avgRating);
    }

    private ReviewsCountDto readReviewsFromCache(final String title) {
        final String cacheKey = cacheKey(title);
        try {
            final Map<String, String> values = reviewsCache.hgetAll(cacheKey);
            if (values == null || values.isEmpty()) {
                return null;
            }

            final String countStr = values.get(CACHE_FIELD_COUNT);
            final String ratingStr = values.get(CACHE_FIELD_RATING);
            if (countStr == null || ratingStr == null) {
                return null;
            }

            return new ReviewsCountDto(Long.parseLong(countStr.trim()), Double.parseDouble(ratingStr.trim()));
        } catch (final Exception e) {
            log.warn("Failed to read reviews cache for key {}", cacheKey, e);
            return null;
        }
    }

    private void storeReviewsInCache(final String title, final ReviewsCountDto reviews) {
        final String cacheKey = cacheKey(title);
        try {
            final Map<String, String> values = Map.of(
                    CACHE_FIELD_COUNT, String.valueOf(reviews.count()),
                    CACHE_FIELD_RATING, String.valueOf(reviews.rating())
            );
            reviewsCache.hset(cacheKey, values);
            reviewsCache.expire(cacheKey, reviewTtlSeconds);
        } catch (final Exception e) {
            log.warn("Failed to write reviews cache for key {}", cacheKey, e);
        }
    }

    private void refreshReviewsCacheByTitle(final String title) {
        if (title == null || title.isBlank()) {
            return;
        }
        final ReviewsCountDto counts = computeReviewsFromCassandra(title);
        storeReviewsInCache(title, counts);
    }

    private static String cacheKey(final String title) {
        return CACHE_KEY_PATTERN.formatted(md5Hex(title));
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
