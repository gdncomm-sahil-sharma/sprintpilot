package com.sprintpilot.service;

import com.sprintpilot.dto.*;
import java.util.List;

/**
 * Service interface for task import functionality
 */
public interface TaskImportService {
    
    /**
     * Import tasks from CSV data
     */
    TaskImportResponse importFromCSV(TaskImportRequest request);
    
    /**
     * Import tasks from Jira
     */
    TaskImportResponse importFromJira(TaskImportRequest request);
    
    /**
     * Test Jira connection
     */
    ApiResponse<String> testJiraConnection(TaskImportRequest.JiraConfigDto config);
    
    /**
     * Fetch issues from Jira without importing
     */
    ApiResponse<List<JiraIssueDto>> fetchJiraIssues(TaskImportRequest.JiraConfigDto config);
    
    /**
     * Validate CSV structure and return preview
     */
    ApiResponse<List<TaskImportRequest.TaskImportDto>> validateAndPreviewCSV(
        List<TaskImportRequest.TaskImportDto> tasks
    );
}
