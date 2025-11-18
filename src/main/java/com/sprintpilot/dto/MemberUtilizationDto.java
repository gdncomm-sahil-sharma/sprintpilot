package com.sprintpilot.dto;

import java.math.BigDecimal;

/**
 * DTO for member utilization response
 */
public record MemberUtilizationDto(
        String memberId,
        String name,
        BigDecimal remainingEstimate,  // Total assigned work (original estimates) for all assigned tasks in the sprint
        BigDecimal capacity,            // Total capacity available in sprint (dailyCapacity * available days after holidays/leaves)
        BigDecimal gap,                 // assignedWork - capacity
        UtilizationStatus status        // OVER_UTILIZED, PROPERLY_UTILIZED, UNDER_UTILIZED
) {
    
    /**
     * Enum for utilization status
     */
    public enum UtilizationStatus {
        OVER_UTILIZED,      // Gap is significantly positive (too much work)
        PROPERLY_UTILIZED,  // Gap is within acceptable range
        UNDER_UTILIZED      // Gap is significantly negative (not enough work)
    }
}

