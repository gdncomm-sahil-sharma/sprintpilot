package com.sprintpilot.dto;

import java.math.BigDecimal;

/**
 * DTO for sprint summary metrics displayed at the top of Analytics page
 */
public record SprintSummaryMetricsDto(
        VelocityMetric velocity,
        SuccessRateMetric successRate,
        CycleTimeMetric cycleTime,
        UtilizationMetric utilization
) {
    /**
     * Average velocity metric
     */
    public record VelocityMetric(
            BigDecimal current,           // Current sprint velocity
            BigDecimal average,            // Average from historical sprints
            BigDecimal percentageChange,   // % change from last sprint
            String trend                   // "up" or "down" or "neutral"
    ) {}

    /**
     * Success rate metric (task completion rate)
     */
    public record SuccessRateMetric(
            BigDecimal current,           // Current success rate %
            BigDecimal percentageChange,   // % change from last sprint
            String trend                   // "up" or "down" or "neutral"
    ) {}

    /**
     * Cycle time metric (average time to complete tasks)
     */
    public record CycleTimeMetric(
            BigDecimal current,           // Current average cycle time in days
            BigDecimal baseline,           // Baseline (historical average)
            BigDecimal difference,         // Difference from baseline
            String trend                   // "up" or "down" or "neutral"
    ) {}

    /**
     * Team utilization metric
     */
    public record UtilizationMetric(
            BigDecimal average,            // Average utilization %
            String status                  // "optimal", "over", "under"
    ) {}
}

