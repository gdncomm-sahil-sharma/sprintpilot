package com.sprintpilot.service.impl;

import com.sprintpilot.entity.Task;
import com.sprintpilot.entity.WorkLog;
import com.sprintpilot.repository.WorkLogRepository;
import com.sprintpilot.service.JiraClient;
import com.sprintpilot.service.WorkLogSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Implementation of WorkLogSyncService for asynchronous work log synchronization
 */
@Service
@Slf4j
public class WorkLogSyncServiceImpl implements WorkLogSyncService {
    
    @Autowired
    private JiraClient jiraClient;
    
    @Autowired
    private WorkLogRepository workLogRepository;
    
    /**
     * Fetch work logs asynchronously in background
     * This method returns immediately - work logs are fetched in parallel in background
     */
    @Override
    @Async("workLogExecutor")
    public void syncWorkLogsAsync(List<Task> tasks) {
        long startTime = System.currentTimeMillis();
        log.info("Started async work log sync for {} tasks", tasks.size());
        
        try {
            // Create CompletableFuture for each task's work log fetch
            List<CompletableFuture<Void>> futures = tasks.stream()
                    .map(task -> CompletableFuture.runAsync(() -> {
                        syncWorkLogsForTask(task);
                    }))
                    .collect(Collectors.toList());
            
            // Wait for all futures to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ Completed async work log sync for {} tasks in {}ms", tasks.size(), duration);
            
        } catch (Exception e) {
            log.error("❌ Error during async work log sync", e);
        }
    }
    
    /**
     * Fetch and save work logs for a single task
     */
    @Transactional
    private void syncWorkLogsForTask(Task task) {
        try {
            log.debug("Fetching work logs for task: {}", task.getTaskKey());
            List<WorkLog> workLogs = jiraClient.fetchWorkLogsForIssue(task.getTaskKey());
            
            if (!workLogs.isEmpty()) {
                // Delete existing work logs for this task
                workLogRepository.deleteByTaskId(task.getId());
                
                // Set task reference and save work logs
                for (WorkLog workLog : workLogs) {
                    workLog.setTask(task);
                }
                workLogRepository.saveAll(workLogs);
                log.debug("✓ Saved {} work logs for task {}", workLogs.size(), task.getTaskKey());
            } else {
                log.debug("No work logs found for task {}", task.getTaskKey());
            }
        } catch (Exception e) {
            log.warn("Failed to sync work logs for task {}: {}", task.getTaskKey(), e.getMessage());
            // Don't fail - continue with other tasks
        }
    }
}

