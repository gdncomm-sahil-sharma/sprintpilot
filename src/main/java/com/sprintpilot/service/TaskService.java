package com.sprintpilot.service;

import com.sprintpilot.dto.TaskPageResponse;
import com.sprintpilot.dto.TaskResponseDto;
import java.util.List;

/**
 * Service interface for task operations
 */
public interface TaskService {
    
    /**
     * Get all tasks for a specific sprint with assignee details
     * 
     * @param sprintId The sprint ID
     * @return List of tasks with assignee information
     */
    List<TaskResponseDto> getTasksBySprintId(String sprintId);
    
    /**
     * Get tasks for a specific sprint with pagination and optional risk factor filtering
     * 
     * @param sprintId The sprint ID
     * @param riskFactor Risk factor filter (ON_TRACK, AT_RISK, OFF_TRACK) - null for all
     * @param page Page number (0-based)
     * @param size Number of items per page
     * @return Paginated task response
     */
    TaskPageResponse getTasksBySprintIdPaginated(String sprintId, String riskFactor, int page, int size);
    
    /**
     * Analyze and update risk factors for all tasks in a sprint
     * 
     * @param sprintId The sprint ID
     * @return Number of tasks analyzed
     */
    int analyzeSprintRisks(String sprintId);
}

