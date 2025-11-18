package com.sprintpilot.dto;

import com.sprintpilot.entity.Sprint;
import java.time.LocalDate;
import java.util.List;

public record SprintDto(
        String id,
        LocalDate startDate,
        LocalDate endDate,
        Integer duration,
        LocalDate freezeDate,
        Sprint.SprintStatus status,
        List<SprintEventDto> events,
        List<TeamMemberDto> teamMembers,
        List<TaskDto> tasks
) {
    public SprintDto {
        if (startDate == null) {
            throw new IllegalArgumentException("Start date cannot be null");
        }
        // Allow null endDate for new sprints - will be calculated by service
        if (duration == null || duration <= 0) {
            throw new IllegalArgumentException("Duration must be positive");
        }
        if (status == null) {
            status = Sprint.SprintStatus.ACTIVE;
        }
        if (events == null) {
            events = List.of();
        }
        if (teamMembers == null) {
            teamMembers = List.of();
        }
        if (tasks == null) {
            tasks = List.of();
        }
    }
}
