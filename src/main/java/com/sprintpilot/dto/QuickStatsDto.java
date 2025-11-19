package com.sprintpilot.dto;

import java.math.BigDecimal;

/**
 * DTO for Quick Stats dashboard metrics
 */
public record QuickStatsDto(
    String sprintName,
    Integer teamMembersCount,
    String sprintStartDate,
    String sprintEndDate,
    BigDecimal assignedHours,
    BigDecimal capacityHours
) {}
