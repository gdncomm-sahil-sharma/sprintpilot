package com.sprintpilot.service.impl;

import com.sprintpilot.dto.TaskDto;
import com.sprintpilot.dto.TaskResponseDto;
import com.sprintpilot.dto.TaskRiskDto;
import com.sprintpilot.entity.Task;
import com.sprintpilot.service.TaskService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Helper class for risk summary generation
 * Contains shared logic for converting tasks and calculating risk factors
 */
@Component
public class RiskSummaryHelper {
    
    private final TaskService taskService;
    
    public RiskSummaryHelper(TaskService taskService) {
        this.taskService = taskService;
    }
    
    /**
     * Fetch tasks from database and convert to DTOs for risk summary
     * 
     * @param sprintId The sprint ID
     * @return RiskSummaryData containing tasks and risks, or null if no tasks found
     */
    public RiskSummaryData prepareRiskSummaryData(String sprintId) {
        // Fetch all tasks for the sprint from database
        List<TaskResponseDto> allTasksResponse = taskService.getTasksBySprintId(sprintId);
        
        if (allTasksResponse.isEmpty()) {
            return null;
        }
        
        // Convert TaskResponseDto to TaskDto for AI service
        List<TaskDto> allTasks = allTasksResponse.stream()
            .map(taskResponse -> new TaskDto(
                taskResponse.id(),
                taskResponse.sprintId(),
                taskResponse.taskKey(),
                taskResponse.summary(),
                taskResponse.description(),
                taskResponse.storyPoints(),
                taskResponse.category(),
                taskResponse.priority(),
                taskResponse.status(),
                taskResponse.startDate(),
                taskResponse.dueDate(),
                taskResponse.timeSpent(),
                taskResponse.assignees() != null && !taskResponse.assignees().isEmpty() 
                    ? taskResponse.assignees().get(0).id() 
                    : null
            ))
            .collect(Collectors.toList());
        
        // Convert tasks to TaskRiskDto format
        List<TaskRiskDto> risks = allTasksResponse.stream()
            .map(taskResponse -> new TaskRiskDto(
                taskResponse.id(),
                convertRiskFactor(taskResponse.riskFactor()),
                getRiskReason(taskResponse.riskFactor())
            ))
            .collect(Collectors.toList());
        
        // Extract assignee names for each task
        Map<String, List<String>> taskAssignees = allTasksResponse.stream()
            .collect(Collectors.toMap(
                TaskResponseDto::id,
                task -> task.assignees() != null && !task.assignees().isEmpty()
                    ? task.assignees().stream()
                        .map(assignee -> assignee.name())
                        .collect(Collectors.toList())
                    : List.of("Unassigned")
            ));
        
        return new RiskSummaryData(allTasks, risks, taskAssignees);
    }
    
    /**
     * Convert Task.RiskFactor to TaskRiskDto.RiskLevel
     */
    private TaskRiskDto.RiskLevel convertRiskFactor(Task.RiskFactor riskFactor) {
        if (riskFactor == null) {
            return TaskRiskDto.RiskLevel.ON_TRACK;
        }
        
        return switch (riskFactor) {
            case OFF_TRACK -> TaskRiskDto.RiskLevel.OFF_TRACK;
            case AT_RISK -> TaskRiskDto.RiskLevel.AT_RISK;
            case ON_TRACK -> TaskRiskDto.RiskLevel.ON_TRACK;
        };
    }
    
    /**
     * Generate risk reason based on task properties
     */
    private String getRiskReason(Task.RiskFactor riskFactor) {
        if (riskFactor == null) {
            return "Not analyzed yet";
        }
        
        return switch (riskFactor) {
            case OFF_TRACK -> "Task is significantly behind schedule or blocked";
            case AT_RISK -> "Task may need attention - potential delays or resource constraints";
            case ON_TRACK -> "Task is progressing as expected";
        };
    }
    
    /**
     * Data class to hold tasks and risks for risk summary generation
     */
    public record RiskSummaryData(
        List<TaskDto> tasks, 
        List<TaskRiskDto> risks,
        Map<String, List<String>> taskAssignees  // taskId -> list of assignee names
    ) {}
}

