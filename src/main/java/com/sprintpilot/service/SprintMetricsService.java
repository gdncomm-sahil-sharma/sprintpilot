package com.sprintpilot.service;

import com.sprintpilot.dto.SprintMetricsDto;
import com.sprintpilot.dto.VelocityTrendDto;
import com.sprintpilot.dto.WorkDistributionDto;

/**
 * Service for generating sprint analytics metrics such as burndown and velocity.
 */
public interface SprintMetricsService {

    /**
     * Generate sprint metrics using tasks stored in SprintPilot.
     *
     * @param sprintId    Sprint identifier.
     * @param projectName Optional project name to include in the response.
     * @return Calculated sprint metrics.
     */
    SprintMetricsDto getSprintMetrics(String sprintId, String projectName);

    /**
     * Calculate work distribution by task category for a sprint.
     *
     * @param sprintId    Sprint identifier.
     * @param projectName Optional project name.
     * @return Work distribution data by category.
     */
    WorkDistributionDto getWorkDistribution(String sprintId, String projectName);

    /**
     * Get velocity trend data for current sprint and last 5 completed sprints.
     *
     * @param currentSprintId Current sprint identifier.
     * @return Velocity trend data across sprints.
     */
    VelocityTrendDto getVelocityTrend(String currentSprintId);
}

