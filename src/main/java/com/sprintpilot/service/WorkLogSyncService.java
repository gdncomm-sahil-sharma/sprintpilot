package com.sprintpilot.service;

import com.sprintpilot.entity.Task;
import java.util.List;

/**
 * Service for asynchronous work log synchronization from Jira
 */
public interface WorkLogSyncService {
    
    /**
     * Fetch and save work logs for multiple tasks asynchronously
     * This method returns immediately and processes work logs in background
     * 
     * @param tasks List of tasks to fetch work logs for
     */
    void syncWorkLogsAsync(List<Task> tasks);
}

