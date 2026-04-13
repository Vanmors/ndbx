package com.vanmors.ndbx.dto;

import com.vanmors.ndbx.entity.Event;

import javax.swing.*;
import java.time.Instant;


public record EventDto(
        String id,
        String title,
        String description,
        LocationDto location,
        Instant created_at,
        String created_by,
        Instant started_at,
        Instant finished_at
) {
    public static EventDto fromEntity(final Event event) {
        final LocationDto locationDto = new LocationDto(event.getLocation().getAddress());
        return new EventDto(event.getId(), event.getTitle(), event.getDescription(), locationDto, event.getCreatedAt(),
                event.getCreatedBy(), event.getStartedAt(), event.getFinishedAt());
    }
}
