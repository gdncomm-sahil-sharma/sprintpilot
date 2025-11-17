package com.sprintpilot.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for task import requests
 */
public record TaskImportRequest(
    String sprintId,
    ImportSource source,
    List<TaskImportDto> tasks,
    JiraConfigDto jiraConfig, // @Deprecated - Jira config is now in properties file
    String projectKey, // For Jira import: project key
    String jqlQuery, // For Jira import: optional JQL query
    ColumnMappingDto columnMapping
) {
    
    public enum ImportSource {
        CSV, JIRA
    }
    
    /**
     * Individual task data for import
     */
    public record TaskImportDto(
        String taskKey,
        String summary,
        String description,
        BigDecimal storyPoints,
        String category,
        String priority,
        String status,
        String assignee,       // Assignee display name
        String assigneeEmail,  // Assignee email address
        String startDate,      // ISO date string (yyyy-MM-dd)
        String endDate,        // ISO date string (yyyy-MM-dd)
        String dueDate,        // ISO date string (yyyy-MM-dd)
        BigDecimal originalEstimate, // In hours
        BigDecimal timeSpent         // In hours
    ) {}
    
    /**
     * Jira configuration for API connection
     */
    public record JiraConfigDto(
        String url,
        String username,
        String token,
        String projectKey,
        String jqlQuery
    ) {}
    
    /**
     * Column mapping for CSV import
     */
    public record ColumnMappingDto(
        int taskKeyColumn,
        int summaryColumn,
        int descriptionColumn,
        int storyPointsColumn,
        int categoryColumn,
        int priorityColumn,
        int assigneeColumn
    ) {}
}
