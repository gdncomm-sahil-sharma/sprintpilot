package com.sprintpilot.dto;

import com.sprintpilot.entity.Task;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO for Task with assignee details
 */
public record TaskResponseDto(
        String id,
        String sprintId,
        String taskKey,
        String summary,
        String description,
        BigDecimal storyPoints,
        Task.TaskCategory category,
        Task.TaskPriority priority,
        Task.TaskStatus status,
        LocalDate startDate,
        LocalDate dueDate,
        BigDecimal timeSpent,
        Task.RiskFactor riskFactor,  // Risk factor calculated by backend
        List<AssigneeDto> assignees,
        String assigneeName  // Primary assignee name for display
) {
    
    /**
     * DTO for assignee information
     */
    public record AssigneeDto(
            String id,
            String name,
            String email,
            String role
    ) {}
}

