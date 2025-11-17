package com.sprintpilot.dto;

import java.util.List;

public record SprintAssignmentRequest(
    String sprintId,
    List<String> memberIds
) {
    // Validation
    public void validate() {
        if (sprintId == null || sprintId.isBlank()) {
            throw new IllegalArgumentException("Sprint ID is required");
        }
        if (memberIds == null) {
            throw new IllegalArgumentException("Member IDs list cannot be null");
        }
    }
}

