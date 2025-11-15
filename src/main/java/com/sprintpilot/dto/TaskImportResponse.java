package com.sprintpilot.dto;

import java.util.List;

/**
 * DTO for task import responses
 */
public record TaskImportResponse(
    boolean success,
    String message,
    ImportResultDto result
) {
    
    /**
     * Detailed import results
     */
    public record ImportResultDto(
        int totalTasks,
        int importedTasks,
        int skippedTasks,
        int failedTasks,
        List<String> errors,
        List<String> warnings,
        List<TaskDto> importedTasksDetails
    ) {}
    
    /**
     * Factory methods for common responses
     */
    public static TaskImportResponse success(ImportResultDto result) {
        return new TaskImportResponse(true, "Import completed successfully", result);
    }
    
    public static TaskImportResponse failure(String message) {
        return new TaskImportResponse(false, message, null);
    }
    
    public static TaskImportResponse partialSuccess(ImportResultDto result, String message) {
        return new TaskImportResponse(true, message, result);
    }
}
