package com.sprintpilot.controller;

import com.sprintpilot.dto.*;
import com.sprintpilot.service.TaskImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * REST Controller for task management operations
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    
    @Autowired
    private TaskImportService taskImportService;
    
    /**
     * Import tasks from CSV file
     */
    @PostMapping("/import/csv")
    public ResponseEntity<TaskImportResponse> importFromCSV(@RequestBody TaskImportRequest request) {
        try {
            TaskImportResponse response = taskImportService.importFromCSV(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            TaskImportResponse errorResponse = TaskImportResponse.failure("Import failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * Import tasks from Jira
     */
    @PostMapping("/import/jira")
    public ResponseEntity<TaskImportResponse> importFromJira(@RequestBody TaskImportRequest request) {
        try {
            TaskImportResponse response = taskImportService.importFromJira(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            TaskImportResponse errorResponse = TaskImportResponse.failure("Jira import failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * Upload and parse CSV file
     */
    @PostMapping("/import/csv/upload")
    public ResponseEntity<ApiResponse<List<TaskImportRequest.TaskImportDto>>> uploadCSV(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sprintId", defaultValue = "current-sprint") String sprintId) {
        
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("File is empty"));
            }
            
            if (!file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Only CSV files are supported"));
            }
            
            List<TaskImportRequest.TaskImportDto> tasks = parseCSVFile(file);
            ApiResponse<List<TaskImportRequest.TaskImportDto>> response = 
                taskImportService.validateAndPreviewCSV(tasks);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.failure("Failed to process CSV file: " + e.getMessage()));
        }
    }
    
    /**
     * Test Jira connection
     */
    @PostMapping("/import/jira/test")
    public ResponseEntity<ApiResponse<String>> testJiraConnection(
            @RequestBody TaskImportRequest.JiraConfigDto config) {
        
        ApiResponse<String> response = taskImportService.testJiraConnection(config);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Fetch issues from Jira
     */
    @PostMapping("/import/jira/fetch")
    public ResponseEntity<ApiResponse<List<JiraIssueDto>>> fetchJiraIssues(
            @RequestBody TaskImportRequest.JiraConfigDto config) {
        
        ApiResponse<List<JiraIssueDto>> response = taskImportService.fetchJiraIssues(config);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get import history/status
     */
    @GetMapping("/import/history")
    public ResponseEntity<ApiResponse<List<ImportHistoryDto>>> getImportHistory(
            @RequestParam(value = "sprintId", required = false) String sprintId) {
        
        // Mock import history
        List<ImportHistoryDto> history = List.of(
            new ImportHistoryDto(
                "import-1",
                "CSV Import",
                "2024-11-15T10:30:00",
                15,
                13,
                2,
                "CSV file imported with 2 warnings"
            ),
            new ImportHistoryDto(
                "import-2", 
                "Jira Import",
                "2024-11-14T14:15:00",
                8,
                8,
                0,
                "Successfully imported from PROJ project"
            )
        );
        
        return ResponseEntity.ok(ApiResponse.success(history));
    }
    
    // Helper methods
    private List<TaskImportRequest.TaskImportDto> parseCSVFile(MultipartFile file) throws Exception {
        List<TaskImportRequest.TaskImportDto> tasks = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV file is empty");
            }
            
            String[] headers = headerLine.split(",");
            
            // Find column indices
            int taskKeyIndex = findColumnIndex(headers, "task key", "key", "issue key");
            int summaryIndex = findColumnIndex(headers, "summary", "title", "description");
            int storyPointsIndex = findColumnIndex(headers, "story points", "points", "estimate");
            int assigneeIndex = findColumnIndex(headers, "assignee", "owner", "assigned to");
            int categoryIndex = findColumnIndex(headers, "category", "type", "issue type");
            int priorityIndex = findColumnIndex(headers, "priority");
            
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                
                String[] values = line.split(",");
                
                // Clean values (remove quotes)
                for (int i = 0; i < values.length; i++) {
                    values[i] = values[i].trim().replaceAll("^\"|\"$", "");
                }
                
                String taskKey = taskKeyIndex >= 0 && taskKeyIndex < values.length ? values[taskKeyIndex] : "";
                String summary = summaryIndex >= 0 && summaryIndex < values.length ? values[summaryIndex] : "";
                String storyPointsStr = storyPointsIndex >= 0 && storyPointsIndex < values.length ? values[storyPointsIndex] : "0";
                String assignee = assigneeIndex >= 0 && assigneeIndex < values.length ? values[assigneeIndex] : null;
                String category = categoryIndex >= 0 && categoryIndex < values.length ? values[categoryIndex] : "FEATURE";
                String priority = priorityIndex >= 0 && priorityIndex < values.length ? values[priorityIndex] : "MEDIUM";
                
                BigDecimal storyPoints;
                try {
                    storyPoints = new BigDecimal(storyPointsStr.isEmpty() ? "0" : storyPointsStr);
                } catch (NumberFormatException e) {
                    storyPoints = BigDecimal.ZERO;
                }
                
                TaskImportRequest.TaskImportDto task = new TaskImportRequest.TaskImportDto(
                    taskKey,
                    summary,
                    "", // description
                    storyPoints,
                    category,
                    priority,
                    "TODO",
                    assignee
                );
                
                tasks.add(task);
            }
        }
        
        return tasks;
    }
    
    private int findColumnIndex(String[] headers, String... possibleNames) {
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].toLowerCase().trim();
            for (String name : possibleNames) {
                if (header.contains(name.toLowerCase())) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    /**
     * DTO for import history
     */
    public record ImportHistoryDto(
        String id,
        String type,
        String timestamp,
        int totalTasks,
        int importedTasks,
        int failedTasks,
        String notes
    ) {}
}
