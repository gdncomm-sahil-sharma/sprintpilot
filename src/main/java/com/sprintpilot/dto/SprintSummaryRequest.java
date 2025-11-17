package com.sprintpilot.dto;

import java.util.List;

/**
 * Request DTO for AI Sprint Summary generation
 * Contains all data needed to generate comprehensive sprint insights
 */
public record SprintSummaryRequest(
        List<TeamMemberDto> team,
        SprintDto sprint,
        List<TaskDto> tasks,
        List<CapacitySummaryDto> workload
) {
    // Validation in compact constructor
    public SprintSummaryRequest {
        if (team == null) {
            team = List.of();
        }
        if (tasks == null) {
            tasks = List.of();
        }
        if (workload == null) {
            workload = List.of();
        }
        // sprint can be null, will be handled by service
    }
}

