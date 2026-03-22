package com.vanmors.ndbx.dto;

import com.vanmors.ndbx.entity.Event;

import javax.swing.*;
import java.time.Instant;


public record EventDto(String title,
                       String description,
                       LocationDto location,
                       Instant startedAt,
                       Instant finishedAt
){
    public static EventDto fromEntity(final Event event) {
        final LocationDto locationDto = new LocationDto(event.getLocation().getAddress());
        return new EventDto(event.getTitle(), event.getDescription(), locationDto, event.getStartedAt(), event.getFinishedAt());
    }
}
