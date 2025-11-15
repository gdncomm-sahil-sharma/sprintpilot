package com.sprintpilot.dto;

import com.sprintpilot.entity.SprintEvent;
import java.time.LocalDate;
import java.time.LocalTime;

public record SprintEventDto(
        String id,
        String sprintId,
        SprintEvent.EventType eventType,
        SprintEvent.MeetingType eventSubtype,
        String name,
        LocalDate eventDate,
        LocalTime eventTime,
        Integer durationMinutes,
        String description
) {
    public SprintEventDto {
        if (eventType == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Event name cannot be null or blank");
        }
        if (eventDate == null) {
            throw new IllegalArgumentException("Event date cannot be null");
        }
        if (eventType == SprintEvent.EventType.MEETING && eventSubtype == null) {
            throw new IllegalArgumentException("Meeting subtype required for meeting events");
        }
    }
}
