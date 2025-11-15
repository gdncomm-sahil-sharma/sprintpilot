package com.sprintpilot.service.impl;

import com.sprintpilot.dto.*;
import com.sprintpilot.service.TaskImportService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Mock implementation of TaskImportService
 * In production, this would integrate with real CSV parsing and Jira API
 */
@Service
public class TaskImportServiceImpl implements TaskImportService {
    
    @Override
    public TaskImportResponse importFromCSV(TaskImportRequest request) {
        try {
            if (request.tasks() == null || request.tasks().isEmpty()) {
                return TaskImportResponse.failure("No tasks provided for import");
            }
            
            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            List<TaskDto> importedTasks = new ArrayList<>();
            int skippedTasks = 0;
            
            for (TaskImportRequest.TaskImportDto taskImport : request.tasks()) {
                try {
                    // Validate required fields
                    if (!StringUtils.hasText(taskImport.taskKey())) {
                        errors.add("Task key is required for all tasks");
                        skippedTasks++;
                        continue;
                    }
                    
                    if (!StringUtils.hasText(taskImport.summary())) {
                        errors.add("Summary is required for task: " + taskImport.taskKey());
                        skippedTasks++;
                        continue;
                    }
                    
                    // Create mock TaskDto (in real implementation, this would create entities)
                    TaskDto importedTask = new TaskDto(
                        UUID.randomUUID().toString(),
                        request.sprintId(),
                        taskImport.taskKey(),
                        taskImport.summary(),
                        taskImport.description(),
                        taskImport.storyPoints() != null ? taskImport.storyPoints() : BigDecimal.ZERO,
                        parseCategory(taskImport.category()),
                        parsePriority(taskImport.priority()),
                        parseStatus(taskImport.status()),
                        LocalDate.now(),
                        LocalDate.now().plusDays(7), // Default due date
                        BigDecimal.ZERO,
                        taskImport.assignee()
                    );
                    
                    importedTasks.add(importedTask);
                    
                    // Add warning for high story points
                    if (taskImport.storyPoints() != null && taskImport.storyPoints().compareTo(new BigDecimal("13")) > 0) {
                        warnings.add("Task " + taskImport.taskKey() + " has unusually high story points (" + 
                                   taskImport.storyPoints() + ")");
                    }
                    
                } catch (Exception e) {
                    errors.add("Failed to import task " + taskImport.taskKey() + ": " + e.getMessage());
                    skippedTasks++;
                }
            }
            
            TaskImportResponse.ImportResultDto result = new TaskImportResponse.ImportResultDto(
                request.tasks().size(),
                importedTasks.size(),
                skippedTasks,
                errors.size(),
                errors,
                warnings,
                importedTasks
            );
            
            if (importedTasks.isEmpty()) {
                return TaskImportResponse.failure("No tasks could be imported. Check the errors and try again.");
            } else if (skippedTasks > 0) {
                return TaskImportResponse.partialSuccess(result, 
                    String.format("Imported %d out of %d tasks. %d tasks were skipped due to errors.", 
                                importedTasks.size(), request.tasks().size(), skippedTasks));
            } else {
                return TaskImportResponse.success(result);
            }
            
        } catch (Exception e) {
            return TaskImportResponse.failure("Import failed: " + e.getMessage());
        }
    }
    
    @Override
    public TaskImportResponse importFromJira(TaskImportRequest request) {
        try {
            if (request.jiraConfig() == null) {
                return TaskImportResponse.failure("Jira configuration is required");
            }
            
            // Mock Jira import - in real implementation, this would call Jira REST API
            List<JiraIssueDto> mockJiraIssues = getMockJiraIssues(request.jiraConfig());
            
            List<TaskDto> importedTasks = mockJiraIssues.stream()
                .map(issue -> {
                    TaskImportRequest.TaskImportDto taskImport = issue.toTaskImportDto();
                    return new TaskDto(
                        UUID.randomUUID().toString(),
                        request.sprintId(),
                        taskImport.taskKey(),
                        taskImport.summary(),
                        taskImport.description(),
                        taskImport.storyPoints(),
                        parseCategory(taskImport.category()),
                        parsePriority(taskImport.priority()),
                        parseStatus(taskImport.status()),
                        LocalDate.now(),
                        LocalDate.now().plusDays(7),
                        BigDecimal.ZERO,
                        taskImport.assignee()
                    );
                })
                .collect(Collectors.toList());
            
            TaskImportResponse.ImportResultDto result = new TaskImportResponse.ImportResultDto(
                mockJiraIssues.size(),
                importedTasks.size(),
                0,
                0,
                Collections.emptyList(),
                Collections.singletonList("Successfully connected to Jira and imported issues"),
                importedTasks
            );
            
            return TaskImportResponse.success(result);
            
        } catch (Exception e) {
            return TaskImportResponse.failure("Jira import failed: " + e.getMessage());
        }
    }
    
    @Override
    public ApiResponse<String> testJiraConnection(TaskImportRequest.JiraConfigDto config) {
        try {
            // Mock connection test
            if (!StringUtils.hasText(config.url()) || 
                !StringUtils.hasText(config.username()) || 
                !StringUtils.hasText(config.token())) {
                return ApiResponse.failure("Missing required Jira connection parameters");
            }
            
            // Simulate connection delay
            Thread.sleep(500);
            
            return ApiResponse.success("✅ Connection successful! Jira API is accessible.");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ApiResponse.failure("Connection test interrupted");
        } catch (Exception e) {
            return ApiResponse.failure("Connection failed: " + e.getMessage());
        }
    }
    
    @Override
    public ApiResponse<List<JiraIssueDto>> fetchJiraIssues(TaskImportRequest.JiraConfigDto config) {
        try {
            if (!StringUtils.hasText(config.projectKey())) {
                return ApiResponse.failure("Project key is required");
            }
            
            // Mock fetch with delay
            Thread.sleep(1000);
            
            List<JiraIssueDto> issues = getMockJiraIssues(config);
            return ApiResponse.success(issues);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ApiResponse.failure("Fetch operation interrupted");
        } catch (Exception e) {
            return ApiResponse.failure("Failed to fetch issues: " + e.getMessage());
        }
    }
    
    @Override
    public ApiResponse<List<TaskImportRequest.TaskImportDto>> validateAndPreviewCSV(
            List<TaskImportRequest.TaskImportDto> tasks) {
        
        try {
            List<String> errors = new ArrayList<>();
            List<TaskImportRequest.TaskImportDto> validTasks = new ArrayList<>();
            
            for (TaskImportRequest.TaskImportDto task : tasks) {
                if (!StringUtils.hasText(task.taskKey())) {
                    errors.add("Task key is missing");
                    continue;
                }
                
                if (!StringUtils.hasText(task.summary())) {
                    errors.add("Summary is missing for task: " + task.taskKey());
                    continue;
                }
                
                validTasks.add(task);
            }
            
            if (!errors.isEmpty()) {
                return ApiResponse.failure("Validation errors: " + String.join(", ", errors));
            }
            
            return ApiResponse.success(validTasks);
            
        } catch (Exception e) {
            return ApiResponse.failure("Validation failed: " + e.getMessage());
        }
    }
    
    // Helper methods
    private List<JiraIssueDto> getMockJiraIssues(TaskImportRequest.JiraConfigDto config) {
        String projectKey = config.projectKey().toUpperCase();
        
        return Arrays.asList(
            new JiraIssueDto(
                projectKey + "-201",
                "Implement user authentication system",
                "Design and implement a secure user authentication system with JWT tokens",
                "Story",
                "High",
                "To Do",
                new BigDecimal("8"),
                "john.doe@company.com",
                "John Doe",
                "product.owner@company.com",
                LocalDate.now().minusDays(5),
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(10),
                Arrays.asList("security", "authentication"),
                Arrays.asList("backend", "api"),
                config.projectKey(),
                "Sample Project"
            ),
            new JiraIssueDto(
                projectKey + "-202",
                "Fix pagination bug in user list",
                "Users list pagination is not working correctly on page 3 and beyond",
                "Bug",
                "Critical",
                "In Progress",
                new BigDecimal("3"),
                "jane.smith@company.com",
                "Jane Smith",
                "qa.lead@company.com",
                LocalDate.now().minusDays(3),
                LocalDate.now(),
                LocalDate.now().plusDays(2),
                Arrays.asList("bug", "pagination"),
                Arrays.asList("frontend", "ui"),
                config.projectKey(),
                "Sample Project"
            ),
            new JiraIssueDto(
                projectKey + "-203",
                "Add advanced search functionality",
                "Implement advanced search with filters for date range, status, and category",
                "Feature",
                "Medium",
                "To Do",
                new BigDecimal("5"),
                null,
                null,
                "product.owner@company.com",
                LocalDate.now().minusDays(2),
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(8),
                Arrays.asList("search", "filters"),
                Arrays.asList("frontend", "backend"),
                config.projectKey(),
                "Sample Project"
            ),
            new JiraIssueDto(
                projectKey + "-204",
                "Update API documentation",
                "Update the REST API documentation to reflect recent changes",
                "Technical Task",
                "Low",
                "To Do",
                new BigDecimal("2"),
                "bob.wilson@company.com",
                "Bob Wilson",
                "tech.lead@company.com",
                LocalDate.now().minusDays(1),
                LocalDate.now(),
                LocalDate.now().plusDays(5),
                Arrays.asList("documentation", "api"),
                Arrays.asList("docs"),
                config.projectKey(),
                "Sample Project"
            ),
            new JiraIssueDto(
                projectKey + "-205",
                "Optimize database query performance",
                "Several database queries are running slowly, need optimization",
                "Technical Task",
                "High",
                "To Do",
                new BigDecimal("13"),
                null,
                null,
                "tech.lead@company.com",
                LocalDate.now(),
                LocalDate.now(),
                LocalDate.now().plusDays(12),
                Arrays.asList("performance", "database"),
                Arrays.asList("backend", "database"),
                config.projectKey(),
                "Sample Project"
            )
        );
    }
    
    private com.sprintpilot.entity.Task.TaskCategory parseCategory(String category) {
        if (category == null) return com.sprintpilot.entity.Task.TaskCategory.FEATURE;
        
        try {
            return com.sprintpilot.entity.Task.TaskCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            return com.sprintpilot.entity.Task.TaskCategory.OTHER;
        }
    }
    
    private com.sprintpilot.entity.Task.TaskPriority parsePriority(String priority) {
        if (priority == null) return com.sprintpilot.entity.Task.TaskPriority.MEDIUM;
        
        try {
            return com.sprintpilot.entity.Task.TaskPriority.valueOf(priority.toUpperCase());
        } catch (IllegalArgumentException e) {
            return com.sprintpilot.entity.Task.TaskPriority.MEDIUM;
        }
    }
    
    private com.sprintpilot.entity.Task.TaskStatus parseStatus(String status) {
        if (status == null) return com.sprintpilot.entity.Task.TaskStatus.TODO;
        
        try {
            return com.sprintpilot.entity.Task.TaskStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return com.sprintpilot.entity.Task.TaskStatus.TODO;
        }
    }
}
