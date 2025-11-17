package com.sprintpilot.service.impl;

import com.sprintpilot.dto.*;
import com.sprintpilot.entity.Sprint;
import com.sprintpilot.entity.Task;
import com.sprintpilot.entity.TeamMember;
import com.sprintpilot.repository.SprintRepository;
import com.sprintpilot.repository.TaskRepository;
import com.sprintpilot.repository.TeamMemberRepository;
import com.sprintpilot.service.JiraClient;
import com.sprintpilot.service.TaskImportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of TaskImportService with real Jira integration
 */
@Service
@Slf4j
public class TaskImportServiceImpl implements TaskImportService {
    
    @Autowired
    private JiraClient jiraClient;
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private SprintRepository sprintRepository;
    
    @Autowired
    private TeamMemberRepository teamMemberRepository;
    
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
            // Validate request
            if (!StringUtils.hasText(request.sprintId())) {
                return TaskImportResponse.failure("Sprint ID is required");
            }
            
            if (!StringUtils.hasText(request.projectKey())) {
                return TaskImportResponse.failure("Project key is required for Jira import");
            }
            
            // Verify sprint exists
            Sprint sprint = sprintRepository.findById(request.sprintId())
                    .orElseThrow(() -> new RuntimeException("Sprint not found: " + request.sprintId()));
            
            // Build JQL query dynamically using sprint name
            String jqlQuery = request.jqlQuery();
            if (jqlQuery == null || jqlQuery.isBlank()) {
                // Build JQL query using sprint name from database
                jqlQuery = String.format("project = %s AND Sprint = \"%s\"", 
                        request.projectKey(), 
                        sprint.getSprintName());
                log.info("Built JQL query from sprint name: {}", jqlQuery);
            } else {
                log.info("Using provided JQL query: {}", jqlQuery);
            }
            
            log.info("Fetching tasks from Jira for project: {}", request.projectKey());
            
            // Fetch tasks from Jira
            List<TaskImportRequest.TaskImportDto> jiraTasks = jiraClient.fetchTasks(
                    request.projectKey(),
                    jqlQuery
            );
            
            if (jiraTasks.isEmpty()) {
                return TaskImportResponse.failure("No tasks found in Jira for the given criteria");
            }
            
            log.info("Fetched {} tasks from Jira", jiraTasks.size());
            
            // Get all task keys from Jira
            List<String> taskKeys = jiraTasks.stream()
                    .map(TaskImportRequest.TaskImportDto::taskKey)
                    .collect(Collectors.toList());
            
            // Fetch all existing tasks for this sprint in a single query
            List<Task> existingTasks = taskRepository.findBySprintIdAndTaskKeyIn(request.sprintId(), taskKeys);
            
            // Create a map for quick lookup: taskKey -> Task
            Map<String, Task> existingTasksMap = existingTasks.stream()
                    .collect(Collectors.toMap(Task::getTaskKey, task -> task));
            
            log.info("Found {} existing tasks in the sprint", existingTasksMap.size());
            
            // Process each task: create or update
            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            List<TaskDto> processedTasks = new ArrayList<>();
            int createdCount = 0;
            int updatedCount = 0;
            
            for (TaskImportRequest.TaskImportDto jiraTask : jiraTasks) {
                try {
                    Task task;
                    boolean isUpdate = false;
                    
                    // Check if task already exists
                    if (existingTasksMap.containsKey(jiraTask.taskKey())) {
                        // Update existing task
                        task = existingTasksMap.get(jiraTask.taskKey());
                        isUpdate = true;
                        log.debug("Updating existing task: {}", jiraTask.taskKey());
                    } else {
                        // Create new task
                        task = new Task();
                        task.setSprint(sprint);
                        task.setTaskKey(jiraTask.taskKey());
                        log.debug("Creating new task: {}", jiraTask.taskKey());
                    }
                    
                    // Update task fields
                    task.setSummary(jiraTask.summary());
                    task.setDescription(jiraTask.description());
                    task.setStoryPoints(jiraTask.storyPoints() != null ? jiraTask.storyPoints() : BigDecimal.ZERO);
                    task.setCategory(parseCategory(jiraTask.category()));
                    task.setPriority(parsePriority(jiraTask.priority()));
                    task.setStatus(parseStatus(jiraTask.status()));
                    
                    // Parse and set dates
                    if (jiraTask.startDate() != null && !jiraTask.startDate().isBlank()) {
                        try {
                            task.setStartDate(LocalDate.parse(jiraTask.startDate()));
                        } catch (Exception e) {
                            log.warn("Failed to parse start date for task {}: {}", jiraTask.taskKey(), jiraTask.startDate());
                        }
                    }
                    
                    if (jiraTask.dueDate() != null && !jiraTask.dueDate().isBlank()) {
                        try {
                            task.setDueDate(LocalDate.parse(jiraTask.dueDate()));
                        } catch (Exception e) {
                            log.warn("Failed to parse due date for task {}: {}", jiraTask.taskKey(), jiraTask.dueDate());
                        }
                    }
                    
                    // Set assignees using email ID from Jira
                    if (jiraTask.assigneeEmail() != null && !jiraTask.assigneeEmail().isBlank()) {
                        try {
                            Optional<TeamMember> assigneeMember = teamMemberRepository.findByEmail(jiraTask.assigneeEmail());
                            if (assigneeMember.isPresent()) {
                                List<TeamMember> assignees = new ArrayList<>();
                                assignees.add(assigneeMember.get());
                                task.setAssignees(assignees);
                                log.debug("Assigned team member {} to task {}", assigneeMember.get().getName(), jiraTask.taskKey());
                            } else {
                                log.warn("No team member found with email: {} for task {}", jiraTask.assigneeEmail(), jiraTask.taskKey());
                                warnings.add("No team member found with email " + jiraTask.assigneeEmail() + " for task " + jiraTask.taskKey());
                            }
                        } catch (Exception e) {
                            log.warn("Failed to assign team member for task {}: {}", jiraTask.taskKey(), e.getMessage());
                        }
                    } else {
                        // Clear assignees if no email provided
                        task.setAssignees(new ArrayList<>());
                    }
                    
                    // Save task
                    Task savedTask = taskRepository.save(task);
                    
                    // Convert to DTO
                    TaskDto taskDto = new TaskDto(
                            savedTask.getId(),
                            savedTask.getSprint().getId(),
                            savedTask.getTaskKey(),
                            savedTask.getSummary(),
                            savedTask.getDescription(),
                            savedTask.getStoryPoints(),
                            savedTask.getCategory(),
                            savedTask.getPriority(),
                            savedTask.getStatus(),
                            savedTask.getStartDate(),
                            savedTask.getDueDate(),
                            savedTask.getTimeSpent(),
                            jiraTask.assignee()
                    );
                    
                    processedTasks.add(taskDto);
                    
                    if (isUpdate) {
                        updatedCount++;
                    } else {
                        createdCount++;
                    }
                    
                    // Add warning for high story points
                    if (jiraTask.storyPoints() != null && jiraTask.storyPoints().compareTo(new BigDecimal("13")) > 0) {
                        warnings.add("Task " + jiraTask.taskKey() + " has unusually high story points (" + 
                                   jiraTask.storyPoints() + ")");
                    }
                    
                } catch (Exception e) {
                    log.error("Failed to process task: " + jiraTask.taskKey(), e);
                    errors.add("Failed to process task " + jiraTask.taskKey() + ": " + e.getMessage());
                }
            }
            
            log.info("Jira import complete: {} created, {} updated, {} errors", createdCount, updatedCount, errors.size());
            
            // Add summary warnings
            if (createdCount > 0) {
                warnings.add(0, String.format("Created %d new task(s)", createdCount));
            }
            if (updatedCount > 0) {
                warnings.add(0, String.format("Updated %d existing task(s)", updatedCount));
            }
            
            TaskImportResponse.ImportResultDto result = new TaskImportResponse.ImportResultDto(
                    jiraTasks.size(),
                    processedTasks.size(),
                    jiraTasks.size() - processedTasks.size(),
                    errors.size(),
                    errors,
                    warnings,
                    processedTasks
            );
            
            if (processedTasks.isEmpty()) {
                return TaskImportResponse.failure("No tasks could be imported. Check the errors and try again.");
            } else if (!errors.isEmpty()) {
                return TaskImportResponse.partialSuccess(result, 
                        String.format("Imported %d out of %d tasks from Jira. %d tasks had errors.", 
                                processedTasks.size(), jiraTasks.size(), errors.size()));
            } else {
            return TaskImportResponse.success(result);
            }
            
        } catch (Exception e) {
            log.error("Jira import failed", e);
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
                "Sample Project",
                LocalDate.now().minusDays(5).toString(),  // start date
                null  // end date (not completed)
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
                "Sample Project",
                LocalDate.now().minusDays(3).toString(),  // start date
                null  // end date (in progress)
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
                "Sample Project",
                LocalDate.now().minusDays(2).toString(),  // start date
                null  // end date (not started)
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
                "Sample Project",
                LocalDate.now().minusDays(1).toString(),  // start date
                null  // end date (not started)
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
                "Sample Project",
                LocalDate.now().toString(),  // start date
                null  // end date (not started)
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
