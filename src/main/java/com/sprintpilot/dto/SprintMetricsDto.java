package com.sprintpilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SprintMetricsDto {
    
    private BurndownData burndown;
    private VelocityData velocity;
    private String sprintId;
    private String sprintName;
    private String projectName;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BurndownData {
        private List<BurndownPoint> points;
        private BigDecimal totalStoryPoints;
        private BigDecimal remainingStoryPoints;
        private BigDecimal completedStoryPoints;
        private String startDate;
        private String endDate;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BurndownPoint {
        private String date;
        private BigDecimal remainingPoints;
        private BigDecimal idealRemaining;
        private BigDecimal completedPoints;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VelocityData {
        private BigDecimal currentSprintVelocity;
        private BigDecimal averageVelocity;
        private List<SprintVelocity> historicalVelocity;
        private BigDecimal committedPoints;
        private BigDecimal completedPoints;
        private Integer completedIssues;
        private Integer totalIssues;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SprintVelocity {
        private String sprintName;
        private String sprintId;
        private BigDecimal committedPoints;
        private BigDecimal completedPoints;
        private String startDate;
        private String endDate;
    }
}

