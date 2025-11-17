package com.sprintpilot.dto;

import java.util.List;

/**
 * Paginated response for tasks
 */
public record TaskPageResponse(
        List<TaskResponseDto> tasks,
        int currentPage,
        int totalPages,
        long totalElements,
        int pageSize,
        boolean hasNext,
        boolean hasPrevious
) {
}

