package com.sprintpilot.service;

import com.sprintpilot.dto.SprintMetricsDto;

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
}

