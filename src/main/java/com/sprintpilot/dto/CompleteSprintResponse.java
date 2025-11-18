package com.sprintpilot.dto;

/**
 * Response DTO for complete and archive sprint operation
 * Contains both the archived sprint and the newly created next sprint
 */
public record CompleteSprintResponse(
    SprintDto archivedSprint,
    SprintDto nextSprint
) {
    public CompleteSprintResponse {
        if (archivedSprint == null) {
            throw new IllegalArgumentException("Archived sprint cannot be null");
        }
        if (nextSprint == null) {
            throw new IllegalArgumentException("Next sprint cannot be null");
        }
    }
}

