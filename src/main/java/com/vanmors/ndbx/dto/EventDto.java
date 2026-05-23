package com.vanmors.ndbx.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vanmors.ndbx.entity.Category;
import com.vanmors.ndbx.entity.Event;

import java.time.Instant;


public record EventDto(
        String id,
        String title,
        Category category,
        Long price,
        String description,
        LocationDto location,
        Instant created_at,
        String created_by,
        Instant started_at,
        Instant finished_at,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        ReactionsCountDto reactions
) {
    public static EventDto fromEntity(final Event event) {
        return fromEntity(event, null);
    }

    public static EventDto fromEntity(final Event event, final ReactionsCountDto reactions) {
        final LocationDto locationDto = new LocationDto(event.getLocation().getCity(), event.getLocation().getAddress());
        return new EventDto(event.getId(), event.getTitle(), event.getCategory(), event.getPrice(), event.getDescription(), locationDto, event.getCreatedAt(),
                event.getCreatedBy(), event.getStartedAt(), event.getFinishedAt(), reactions);
    }

    public EventDto withReactions(final ReactionsCountDto reactions) {
        return new EventDto(id, title, category, price, description, location, created_at, created_by, started_at, finished_at, reactions);
    }

    public static Event toEntity(final EventDto eventDto, final String userId) {
        final Event.Location location = new Event.Location();
        if (eventDto.location() != null) {
            location.setAddress(eventDto.location().address());
        }

        final Event event = new Event();
        event.setTitle(eventDto.title());
        event.setCategory(eventDto.category());
        event.setPrice(eventDto.price());
        event.setDescription(eventDto.description());
        event.setLocation(location);
        event.setStartedAt(eventDto.started_at());
        event.setFinishedAt(eventDto.finished_at());
        event.setCreatedBy(userId);

        return event;
    }
}
