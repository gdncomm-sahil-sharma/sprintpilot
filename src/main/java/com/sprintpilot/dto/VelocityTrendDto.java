package com.sprintpilot.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for velocity trend data across multiple sprints
 */
public record VelocityTrendDto(
        List<SprintVelocityData> sprints,
        BigDecimal averageVelocity
) {
    /**
     * Velocity data for a single sprint
     */
    public record SprintVelocityData(
            String sprintId,
            String sprintName,
            BigDecimal committedPoints,
            BigDecimal completedPoints,
            String status  // "ACTIVE" or "ARCHIVED"
    ) {}
}

