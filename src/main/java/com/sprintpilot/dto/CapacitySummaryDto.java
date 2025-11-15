package com.sprintpilot.dto;

import java.math.BigDecimal;

public record CapacitySummaryDto(
        String memberId,
        String memberName,
        BigDecimal totalCapacity,
        BigDecimal assignedHours,
        BigDecimal remainingHours,
        CapacityStatus status
) {
    public CapacitySummaryDto {
        if (memberId == null || memberId.isBlank()) {
            throw new IllegalArgumentException("Member ID cannot be null or blank");
        }
        if (memberName == null || memberName.isBlank()) {
            throw new IllegalArgumentException("Member name cannot be null or blank");
        }
        if (totalCapacity == null || totalCapacity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total capacity cannot be negative");
        }
        if (assignedHours == null || assignedHours.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Assigned hours cannot be negative");
        }
        if (remainingHours == null) {
            remainingHours = totalCapacity.subtract(assignedHours);
        }
        if (status == null) {
            status = calculateStatus(totalCapacity, assignedHours);
        }
    }
    
    private static CapacityStatus calculateStatus(BigDecimal totalCapacity, BigDecimal assignedHours) {
        if (totalCapacity.compareTo(BigDecimal.ZERO) == 0) {
            return CapacityStatus.OK;
        }
        
        BigDecimal utilization = assignedHours
                .divide(totalCapacity, 2, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        
        if (utilization.compareTo(new BigDecimal("100")) > 0) {
            return CapacityStatus.OVERLOADED;
        } else if (utilization.compareTo(new BigDecimal("70")) < 0) {
            return CapacityStatus.UNDERUTILIZED;
        }
        return CapacityStatus.OK;
    }
    
    public enum CapacityStatus {
        OK,
        OVERLOADED,
        UNDERUTILIZED
    }
}
