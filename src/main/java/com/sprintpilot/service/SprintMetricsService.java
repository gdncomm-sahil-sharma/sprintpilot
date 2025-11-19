package com.sprintpilot.service;

import com.sprintpilot.dto.CurrentSprintMetricsDto;
import com.sprintpilot.dto.QuickStatsDto;
import com.sprintpilot.dto.SprintMetricsDto;
import com.sprintpilot.dto.SprintSummaryMetricsDto;
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

    /**
     * Get summary metrics for analytics dashboard (velocity, success rate, cycle time, utilization).
     *
     * @param currentSprintId Current sprint identifier.
     * @return Summary metrics for the analytics page.
     */
    SprintSummaryMetricsDto getSummaryMetrics(String currentSprintId);

    /**
     * Get current sprint metrics for tasks page (progress, work remaining, tasks completed, utilization).
     *
     * @param currentSprintId Current sprint identifier.
     * @return Current sprint metrics for the tasks page.
     */
    CurrentSprintMetricsDto getCurrentSprintMetrics(String currentSprintId);
    
    /**
     * Get quick stats for dashboard (team members, sprint dates, assigned vs capacity).
     *
     * @param sprintId Sprint identifier.
     * @return Quick stats for the dashboard.
     */
    QuickStatsDto getQuickStats(String sprintId);
}

