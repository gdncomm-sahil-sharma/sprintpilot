package com.sprintpilot.dto;

public record TaskRiskDto(
        String taskId,
        RiskLevel riskLevel,
        String reason
) {
    public TaskRiskDto {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("Task ID cannot be null or blank");
        }
        if (riskLevel == null) {
            throw new IllegalArgumentException("Risk level cannot be null");
        }
        if (reason == null || reason.isBlank()) {
            reason = "Risk assessment pending";
        }
    }
    
    public enum RiskLevel {
        ON_TRACK("On Track"),
        AT_RISK("At Risk"),
        OFF_TRACK("Off Track");
        
        private final String displayName;
        
        RiskLevel(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
