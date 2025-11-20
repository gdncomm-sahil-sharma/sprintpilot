package com.sprintpilot.controller;

import com.sprintpilot.dto.*;
import com.sprintpilot.service.TaskImportService;
import com.sprintpilot.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * REST Controller for task management operations
 */
@Slf4j
@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Tasks", description = "Task management and import operations")
public class TaskController {
    
    @Autowired
    private TaskImportService taskImportService;
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private com.sprintpilot.service.MemberService memberService;
    
    /**
     * Get all tasks for a sprint with assignee details (with pagination and filtering)
     * 
     * @param sprintId The sprint ID
     * @param riskFactor Optional risk factor filter (ON_TRACK, AT_RISK, OFF_TRACK)
     * @param page Page number (0-based)
     * @param size Number of items per page
     * @return Paginated list of tasks with assignee names
     */
    @Operation(
        summary = "Get tasks by sprint ID",
        description = "Retrieves tasks for a specific sprint with pagination and optional risk factor filtering. Results are sorted by assignee name."
    )
    @GetMapping("/sprint/{sprintId}")
    public ResponseEntity<ApiResponse<TaskPageResponse>> getTasksBySprintId(
            @Parameter(description = "Sprint ID to fetch tasks for", required = true)
            @PathVariable("sprintId") String sprintId,
            @Parameter(description = "Risk factor filter (ON_TRACK, AT_RISK, OFF_TRACK)", required = true)
            @RequestParam(required = true) String riskFactor,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("GET /api/tasks/sprint/{} - riskFactor: {}, page: {}, size: {}", sprintId, riskFactor, page, size);
            
            // Automatically analyze task risks before fetching
            try {
                int tasksAnalyzed = taskService.analyzeSprintRisks(sprintId);
                log.info("Auto-analyzed {} tasks for sprint {}", tasksAnalyzed, sprintId);
            } catch (Exception e) {
                log.warn("Auto-analysis failed for sprint {}: {} - continuing with fetch", sprintId, e.getMessage());
                // Continue even if analysis fails
            }
            
            TaskPageResponse response = taskService.getTasksBySprintIdPaginated(sprintId, riskFactor, page, size);
            log.info("Returning {} tasks (page {}/{})", response.tasks().size(), response.currentPage() + 1, response.totalPages());
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Failed to fetch tasks for sprint {}: {}", sprintId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Failed to fetch tasks: " + e.getMessage()));
        }
    }
    
    /**
     * Analyze risks for all tasks in the current sprint (reads sprintId from cookie)
     * 
     * @param request HttpServletRequest to read cookies
     * @return Number of tasks analyzed
     */
    @Operation(
        summary = "Analyze task risks",
        description = "Analyzes and updates risk factors for all tasks in the current sprint. Reads sprint ID from cookie 'currentSprintId'"
    )
    @PostMapping("/analyze-risks")
    public ResponseEntity<ApiResponse<Integer>> analyzeTaskRisks(HttpServletRequest request) {
        try {
            // Read sprint ID from cookie
            String sprintId = getSprintIdFromCookie(request);
            
            if (sprintId == null || sprintId.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.failure("Sprint ID not found in cookie"));
            }
            
            int tasksAnalyzed = taskService.analyzeSprintRisks(sprintId);
            return ResponseEntity.ok(ApiResponse.success(tasksAnalyzed));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Failed to analyze risks: " + e.getMessage()));
        }
    }
    
    /**
     * Helper method to extract sprint ID from cookies
     */
    private String getSprintIdFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("currentSprintId".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
    
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
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
            TaskImportResponse errorResponse = TaskImportResponse.failure("Jira import failed: " + errorMsg);
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
                    assignee,          // assignee name
                    null,              // assigneeEmail (not available in CSV)
                    null,              // startDate
                    null,              // endDate
                    null,              // dueDate
                    storyPoints,       // originalEstimate defaults to story points for CSV
                    BigDecimal.ZERO    // timeSpent defaults to 0 for CSV
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
     * Get member utilization metrics for a sprint
     * 
     * @param sprintId Sprint ID to calculate utilization for
     * @return List of member utilization metrics
     */
    @Operation(
        summary = "Get member utilization by sprint",
        description = "Calculates utilization metrics for all team members in a sprint, including remaining estimate, capacity, gap, and utilization status"
    )
    @GetMapping("/utilization/sprint/{sprintId}")
    public ResponseEntity<ApiResponse<List<com.sprintpilot.dto.MemberUtilizationDto>>> getMemberUtilization(
            @Parameter(description = "Sprint ID to calculate utilization for", required = true)
            @PathVariable("sprintId") String sprintId) {
        try {
            log.info("GET /api/tasks/utilization/sprint/{}", sprintId);
            List<com.sprintpilot.dto.MemberUtilizationDto> utilizations = memberService.getMemberUtilizationBySprintId(sprintId);
            log.info("Returning utilization data for {} members", utilizations.size());
            return ResponseEntity.ok(ApiResponse.success(utilizations));
        } catch (Exception e) {
            log.error("Failed to calculate member utilization for sprint {}: {}", sprintId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Failed to calculate member utilization: " + e.getMessage()));
        }
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
