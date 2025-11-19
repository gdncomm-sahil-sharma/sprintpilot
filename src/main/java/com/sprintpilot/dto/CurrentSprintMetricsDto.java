package com.sprintpilot.dto;

import java.math.BigDecimal;

/**
 * DTO for current sprint metrics displayed in Tasks page
 */
public record CurrentSprintMetricsDto(
        SprintProgressMetric sprintProgress,
        WorkRemainingMetric workRemaining,
        TasksCompletedMetric tasksCompleted,
        UtilizationMetric utilization
) {
    /**
     * Sprint progress metric
     */
    public record SprintProgressMetric(
            BigDecimal percentComplete,     // % of sprint elapsed
            Integer currentDay,             // Current day number
            Integer totalDays               // Total days in sprint
    ) {}

    /**
     * Work remaining metric
     */
    public record WorkRemainingMetric(
            BigDecimal hoursRemaining,      // Total hours remaining
            Integer daysLeft                // Days remaining in sprint
    ) {}

    /**
     * Tasks completed metric
     */
    public record TasksCompletedMetric(
            Integer completedTasks,         // Number of completed tasks
            Integer totalTasks,             // Total number of tasks
            BigDecimal percentComplete      // % of tasks completed
    ) {}

    /**
     * Team utilization metric
     */
    public record UtilizationMetric(
            BigDecimal average,             // Average utilization %
            String status                   // "optimal", "over", "under"
    ) {}
}

