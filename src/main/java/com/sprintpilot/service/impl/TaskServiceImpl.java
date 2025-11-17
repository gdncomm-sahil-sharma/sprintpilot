package com.sprintpilot.service.impl;

import com.sprintpilot.dto.TaskPageResponse;
import com.sprintpilot.dto.TaskResponseDto;
import com.sprintpilot.entity.Task;
import com.sprintpilot.repository.TaskRepository;
import com.sprintpilot.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of TaskService
 */
@Service
@Slf4j
public class TaskServiceImpl implements TaskService {
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Override
    @Transactional(readOnly = true)
    public List<TaskResponseDto> getTasksBySprintId(String sprintId) {
        log.info("Fetching tasks for sprint: {}", sprintId);
        
        // Fetch tasks with assignees eagerly loaded
        List<Task> tasks = taskRepository.findBySprintId(sprintId);
        
        // Convert to DTOs with assignee details
        return tasks.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public TaskPageResponse getTasksBySprintIdPaginated(String sprintId, String riskFactor, int page, int size) {
        log.info("Fetching tasks for sprint: {} with riskFactor: {}, page: {}, size: {}", 
                 sprintId, riskFactor, page, size);
        
        // Create pageable with sort already handled by the query
        Pageable pageable = PageRequest.of(page, size);
        
        // Fetch paginated tasks
        Page<Task> taskPage = taskRepository.findBySprintIdAndRiskFactorPaginated(
                sprintId, 
                riskFactor,
                pageable
        );
        
        // Convert to DTOs and sort by assignee name
        List<TaskResponseDto> taskDtos = taskPage.getContent().stream()
                .map(this::convertToResponseDto)
                .sorted((t1, t2) -> {
                    // Tasks without assignees come last
                    String name1 = t1.assigneeName() != null ? t1.assigneeName() : "\uFFFF"; // Use max Unicode char for unassigned
                    String name2 = t2.assigneeName() != null ? t2.assigneeName() : "\uFFFF";
                    int nameCompare = name1.compareTo(name2);
                    if (nameCompare != 0) return nameCompare;
                    // If same assignee, sort by priority desc
                    return t2.priority().compareTo(t1.priority());
                })
                .collect(Collectors.toList());
        
        return new TaskPageResponse(
                taskDtos,
                taskPage.getNumber(),
                taskPage.getTotalPages(),
                taskPage.getTotalElements(),
                taskPage.getSize(),
                taskPage.hasNext(),
                taskPage.hasPrevious()
        );
    }
    
    /**
     * Convert Task entity to TaskResponseDto
     */
    private TaskResponseDto convertToResponseDto(Task task) {
        // Convert assignees to AssigneeDto list
        List<TaskResponseDto.AssigneeDto> assigneeDtos = task.getAssignees().stream()
                .map(member -> new TaskResponseDto.AssigneeDto(
                        member.getId(),
                        member.getName(),
                        member.getEmail(),
                        member.getRole() != null ? member.getRole().name() : null
                ))
                .collect(Collectors.toList());
        
        // Get primary assignee name (first assignee if exists)
        String primaryAssigneeName = task.getAssignees().isEmpty() 
                ? null 
                : task.getAssignees().get(0).getName();
        
        return new TaskResponseDto(
                task.getId(),
                task.getSprint().getId(),
                task.getTaskKey(),
                task.getSummary(),
                task.getDescription(),
                task.getStoryPoints(),
                task.getCategory(),
                task.getPriority(),
                task.getStatus(),
                task.getStartDate(),
                task.getDueDate(),
                task.getTimeSpent(),
                task.getRiskFactor(),
                assigneeDtos,
                primaryAssigneeName
        );
    }
    
    @Override
    @Transactional
    public int analyzeSprintRisks(String sprintId) {
        log.info("Analyzing risks for sprint: {}", sprintId);
        
        List<Task> tasks = taskRepository.findBySprintId(sprintId);
        
        for (Task task : tasks) {
            Task.RiskFactor riskFactor = calculateRiskFactor(task);
            task.setRiskFactor(riskFactor);
            taskRepository.save(task);
        }
        
        log.info("Analyzed {} tasks for sprint: {}", tasks.size(), sprintId);
        return tasks.size();
    }
    
    /**
     * Calculate risk factor based on task attributes
     * Logic: Uses startDate, dueDate, timeSpent, and status
     */
    private Task.RiskFactor calculateRiskFactor(Task task) {
        LocalDate today = LocalDate.now();
        
        // Rule 1: Completed tasks are always ON_TRACK
        if (task.getStatus() == Task.TaskStatus.DONE) {
            return Task.RiskFactor.ON_TRACK;
        }
        
        // Rule 2: Check if task is past due date
        if (task.getDueDate() != null && task.getDueDate().isBefore(today)) {
            return Task.RiskFactor.OFF_TRACK;
        }
        
        // Rule 3: Check progress vs. time remaining
        if (task.getStartDate() != null && task.getDueDate() != null) {
            long totalDays = ChronoUnit.DAYS.between(task.getStartDate(), task.getDueDate());
            long daysPassed = ChronoUnit.DAYS.between(task.getStartDate(), today);
            long daysRemaining = ChronoUnit.DAYS.between(today, task.getDueDate());
            
            if (totalDays > 0) {
                double percentTimeElapsed = (double) daysPassed / totalDays * 100;
                
                // Calculate percent work completed based on time spent vs story points
                double percentWorkCompleted = 0;
                if (task.getStoryPoints() != null && task.getStoryPoints().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal timeSpent = task.getTimeSpent() != null ? task.getTimeSpent() : BigDecimal.ZERO;
                    percentWorkCompleted = timeSpent.divide(task.getStoryPoints(), 2, RoundingMode.HALF_UP)
                                                    .multiply(BigDecimal.valueOf(100))
                                                    .doubleValue();
                }
                
                // If time elapsed is significantly more than work completed, it's at risk
                if (percentTimeElapsed > percentWorkCompleted + 20) {
                    return Task.RiskFactor.AT_RISK;
                }
                
                // If due in less than 2 days and not much progress, it's at risk
                if (daysRemaining <= 2 && percentWorkCompleted < 50) {
                    return Task.RiskFactor.AT_RISK;
                }
            }
        }
        
        // Rule 4: Check if approaching due date with little progress
        if (task.getDueDate() != null) {
            long daysUntilDue = ChronoUnit.DAYS.between(today, task.getDueDate());
            
            if (daysUntilDue <= 2 && daysUntilDue >= 0) {
                // Due in 2 days or less
                BigDecimal timeSpent = task.getTimeSpent() != null ? task.getTimeSpent() : BigDecimal.ZERO;
                BigDecimal storyPoints = task.getStoryPoints() != null ? task.getStoryPoints() : BigDecimal.ZERO;
                
                // If less than 50% complete and due soon, mark as AT_RISK
                if (storyPoints.compareTo(BigDecimal.ZERO) > 0) {
                    double percentComplete = timeSpent.divide(storyPoints, 2, RoundingMode.HALF_UP)
                                                     .multiply(BigDecimal.valueOf(100))
                                                     .doubleValue();
                    if (percentComplete < 50) {
                        return Task.RiskFactor.AT_RISK;
                    }
                }
            }
        }
        
        // Rule 5: Check task status and priority
        if (task.getStatus() == Task.TaskStatus.TODO && 
            (task.getPriority() == Task.TaskPriority.CRITICAL || 
             task.getPriority() == Task.TaskPriority.HIGH)) {
            // High priority tasks that haven't started are at risk
            if (task.getDueDate() != null) {
                long daysUntilDue = ChronoUnit.DAYS.between(today, task.getDueDate());
                if (daysUntilDue <= 5) {
                    return Task.RiskFactor.AT_RISK;
                }
            }
        }
        
        // Default: ON_TRACK
        return Task.RiskFactor.ON_TRACK;
    }
}

