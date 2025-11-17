package com.sprintpilot.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO representing a Jira issue for import
 */
public record JiraIssueDto(
    String key,
    String summary,
    String description,
    String issueType,
    String priority,
    String status,
    BigDecimal storyPoints,
    String assignee,
    String assigneeDisplayName,
    String reporter,
    LocalDate created,
    LocalDate updated,
    LocalDate dueDate,
    List<String> labels,
    List<String> components,
    String projectKey,
    String projectName,
    String start,
    String end
) {
    
    /**
     * Convert Jira issue to TaskImportDto
     */
    public TaskImportRequest.TaskImportDto toTaskImportDto() {
        return new TaskImportRequest.TaskImportDto(
            key,
            summary,
            description,
            storyPoints != null ? storyPoints : BigDecimal.ZERO,
            mapIssueTypeToCategory(issueType),
            mapJiraPriorityToTaskPriority(priority),
            "TODO", // Default status for new imports
            assigneeDisplayName,  // assignee
            assignee,             // assigneeEmail
            start,                // startDate
            end,                  // endDate
            dueDate != null ? dueDate.toString() : null  // dueDate
        );
    }
    
    private String mapIssueTypeToCategory(String jiraIssueType) {
        if (jiraIssueType == null) return "FEATURE";
        
        return switch (jiraIssueType.toLowerCase()) {
            case "bug", "defect" -> "PROD_ISSUE";
            case "technical task", "tech debt", "refactoring" -> "TECH_DEBT";
            case "story", "feature", "epic", "task" -> "FEATURE";
            default -> "OTHER";
        };
    }
    
    private String mapJiraPriorityToTaskPriority(String jiraPriority) {
        if (jiraPriority == null) return "MEDIUM";
        
        return switch (jiraPriority.toLowerCase()) {
            case "highest", "critical", "blocker" -> "CRITICAL";
            case "high" -> "HIGH";
            case "medium" -> "MEDIUM";
            case "low" -> "LOW";
            case "lowest", "trivial" -> "LOWEST";
            default -> "MEDIUM";
        };
    }
}
