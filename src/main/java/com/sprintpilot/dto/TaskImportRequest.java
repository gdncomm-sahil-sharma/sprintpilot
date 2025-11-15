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
    JiraConfigDto jiraConfig,
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
        String assignee
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
