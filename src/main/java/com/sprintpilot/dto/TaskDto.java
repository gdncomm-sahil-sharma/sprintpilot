package com.sprintpilot.dto;

import com.sprintpilot.entity.Task;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TaskDto(
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
        String assigneeId
) {
    public TaskDto {
        if (taskKey == null || taskKey.isBlank()) {
            throw new IllegalArgumentException("Task key cannot be null or blank");
        }
        if (summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("Summary cannot be null or blank");
        }
        if (storyPoints == null || storyPoints.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Story points cannot be negative");
        }
        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }
        if (priority == null) {
            priority = Task.TaskPriority.MEDIUM;
        }
        if (status == null) {
            status = Task.TaskStatus.TODO;
        }
        if (timeSpent == null) {
            timeSpent = BigDecimal.ZERO;
        }
    }
}
