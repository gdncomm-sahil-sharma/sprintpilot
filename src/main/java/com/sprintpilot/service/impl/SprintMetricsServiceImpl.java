package com.sprintpilot.service.impl;

import com.sprintpilot.dto.SprintMetricsDto;
import com.sprintpilot.dto.VelocityTrendDto;
import com.sprintpilot.dto.WorkDistributionDto;
import com.sprintpilot.entity.Sprint;
import com.sprintpilot.entity.Task;
import com.sprintpilot.entity.TeamMember;
import com.sprintpilot.repository.SprintRepository;
import com.sprintpilot.repository.TaskRepository;
import com.sprintpilot.service.SprintMetricsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SprintMetricsServiceImpl implements SprintMetricsService {

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Override
    @Transactional(readOnly = true)
    public SprintMetricsDto getSprintMetrics(String sprintId, String projectName) {
        Sprint sprint = sprintRepository.findByIdWithTeamMembers(sprintId)
                .orElseThrow(() -> new RuntimeException("Sprint not found: " + sprintId));

        List<Task> tasks = taskRepository.findBySprintId(sprintId);
        if (tasks.isEmpty()) {
            log.info("No tasks found for sprint {}. Returning empty metrics.", sprintId);
        }

        BigDecimal totalOriginalEstimate = tasks.stream()
                .map(this::resolveOriginalEstimate)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTimeSpent = tasks.stream()
                .map(task -> task.getTimeSpent() != null ? task.getTimeSpent() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalDays = calculateTotalDays(sprint);
        if (totalDays <= 0) {
            totalDays = sprint.getDuration() != null ? sprint.getDuration() : 1;
        }

        BigDecimal idealCapacityPerDay = calculateIdealCapacityPerDay(sprint, totalDays);

        Map<LocalDate, BigDecimal> burnByDate = buildTimeSpentByDate(tasks, sprint.getStartDate(), sprint.getEndDate(), totalTimeSpent);

        List<SprintMetricsDto.BurndownPoint> burndownPoints = buildBurndownPoints(
                sprint.getStartDate(),
                sprint.getEndDate(),
                totalOriginalEstimate,
                totalTimeSpent,
                burnByDate,
                idealCapacityPerDay
        );

        BigDecimal remainingWork = clampNonNegative(totalOriginalEstimate.subtract(totalTimeSpent));

        SprintMetricsDto.BurndownData burndownData = SprintMetricsDto.BurndownData.builder()
                .points(burndownPoints)
                .totalStoryPoints(totalOriginalEstimate)
                .remainingStoryPoints(remainingWork)
                .completedStoryPoints(totalTimeSpent)
                .startDate(sprint.getStartDate() != null ? sprint.getStartDate().toString() : null)
                .endDate(sprint.getEndDate() != null ? sprint.getEndDate().toString() : null)
                .build();

        SprintMetricsDto.VelocityData velocityData = SprintMetricsDto.VelocityData.builder()
                .currentSprintVelocity(totalTimeSpent)
                .averageVelocity(totalTimeSpent)
                .historicalVelocity(List.of())
                .committedPoints(totalOriginalEstimate)
                .completedPoints(totalTimeSpent)
                .completedIssues((int) tasks.stream().filter(t -> t.getStatus() == Task.TaskStatus.DONE).count())
                .totalIssues(tasks.size())
                .build();

        return SprintMetricsDto.builder()
                .burndown(burndownData)
                .velocity(velocityData)
                .sprintId(sprint.getId())
                .sprintName(sprint.getSprintName())
                .projectName(projectName)
                .build();
    }

    private Map<LocalDate, BigDecimal> buildTimeSpentByDate(List<Task> tasks,
                                                            LocalDate sprintStart,
                                                            LocalDate sprintEnd,
                                                            BigDecimal totalTimeSpent) {
        Map<LocalDate, BigDecimal> burnByDate = new TreeMap<>();

        for (Task task : tasks) {
            BigDecimal timeSpent = task.getTimeSpent() != null ? task.getTimeSpent() : BigDecimal.ZERO;
            if (timeSpent.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            LocalDate logDate = determineLogDate(task, sprintStart, sprintEnd);
            burnByDate.merge(logDate, timeSpent, BigDecimal::add);
        }

        // Ensure total time spent is reflected even if tasks lack timestamps
        BigDecimal allocated = burnByDate.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (allocated.compareTo(totalTimeSpent) < 0 && totalTimeSpent.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal remainder = totalTimeSpent.subtract(allocated);
            LocalDate allocationDate = clampDate(LocalDate.now(), sprintStart, sprintEnd);
            burnByDate.merge(allocationDate, remainder, BigDecimal::add);
        }

        return burnByDate;
    }

    private LocalDate determineLogDate(Task task, LocalDate sprintStart, LocalDate sprintEnd) {
        LocalDate candidate = null;
        LocalDateTime updatedAt = task.getUpdatedAt();
        if (updatedAt != null) {
            candidate = updatedAt.toLocalDate();
        } else if (task.getDueDate() != null) {
            candidate = task.getDueDate();
        } else if (task.getStartDate() != null) {
            candidate = task.getStartDate();
        } else {
            candidate = sprintStart;
        }

        return clampDate(candidate, sprintStart, sprintEnd);
    }

    private LocalDate clampDate(LocalDate date, LocalDate sprintStart, LocalDate sprintEnd) {
        if (date.isBefore(sprintStart)) {
            return sprintStart;
        }
        if (date.isAfter(sprintEnd)) {
            return sprintEnd;
        }
        return date;
    }

    private List<SprintMetricsDto.BurndownPoint> buildBurndownPoints(LocalDate startDate,
                                                                     LocalDate endDate,
                                                                     BigDecimal totalOriginalEstimate,
                                                                     BigDecimal totalTimeSpent,
                                                                     Map<LocalDate, BigDecimal> burnByDate,
                                                                     BigDecimal idealCapacityPerDay) {
        List<SprintMetricsDto.BurndownPoint> points = new ArrayList<>();
        if (startDate == null || endDate == null) {
            return points;
        }

        BigDecimal cumulativeCompleted = BigDecimal.ZERO;
        LocalDate currentDate = startDate;
        int dayIndex = 0;

        while (!currentDate.isAfter(endDate)) {
            BigDecimal burnedToday = burnByDate.getOrDefault(currentDate, BigDecimal.ZERO);
            cumulativeCompleted = cumulativeCompleted.add(burnedToday);
            if (cumulativeCompleted.compareTo(totalTimeSpent) > 0) {
                cumulativeCompleted = totalTimeSpent;
            }

            BigDecimal remaining = clampNonNegative(totalOriginalEstimate.subtract(cumulativeCompleted));
            BigDecimal idealRemaining = clampNonNegative(totalOriginalEstimate.subtract(
                    idealCapacityPerDay.multiply(BigDecimal.valueOf(dayIndex))));

            points.add(SprintMetricsDto.BurndownPoint.builder()
                    .date(currentDate.toString())
                    .remainingPoints(remaining)
                    .idealRemaining(idealRemaining)
                    .completedPoints(cumulativeCompleted)
                    .build());

            currentDate = currentDate.plusDays(1);
            dayIndex++;
        }

        if (points.isEmpty()) {
            points.add(SprintMetricsDto.BurndownPoint.builder()
                    .date(LocalDate.now().toString())
                    .remainingPoints(totalOriginalEstimate)
                    .idealRemaining(totalOriginalEstimate)
                    .completedPoints(totalTimeSpent)
                    .build());
        } else {
            // Ensure the final point reflects the true totals
            SprintMetricsDto.BurndownPoint lastPoint = points.get(points.size() - 1);
            lastPoint.setCompletedPoints(totalTimeSpent);
            lastPoint.setRemainingPoints(clampNonNegative(totalOriginalEstimate.subtract(totalTimeSpent)));
        }

        return points;
    }

    private BigDecimal calculateIdealCapacityPerDay(Sprint sprint, long totalDays) {
        List<TeamMember> members = sprint.getTeamMembers();
        BigDecimal dailyCapacitySum = members.stream()
                .map(member -> member.getDailyCapacity() != null ? member.getDailyCapacity() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDays <= 0) {
            return dailyCapacitySum;
        }

        // Total sprint capacity = daily capacity * total days
        BigDecimal totalSprintCapacity = dailyCapacitySum.multiply(BigDecimal.valueOf(totalDays));
        return totalSprintCapacity.divide(BigDecimal.valueOf(totalDays), 2, RoundingMode.HALF_UP);
    }

    private long calculateTotalDays(Sprint sprint) {
        if (sprint.getStartDate() == null || sprint.getEndDate() == null) {
            return sprint.getDuration() != null ? sprint.getDuration() : 0;
        }
        return ChronoUnit.DAYS.between(sprint.getStartDate(), sprint.getEndDate()) + 1;
    }

    private BigDecimal resolveOriginalEstimate(Task task) {
        BigDecimal estimate = task.getOriginalEstimate();
        if (estimate != null && estimate.compareTo(BigDecimal.ZERO) > 0) {
            return estimate;
        }
        return task.getStoryPoints() != null ? task.getStoryPoints() : BigDecimal.ZERO;
    }

    private BigDecimal clampNonNegative(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : value;
    }

    @Override
    @Transactional(readOnly = true)
    public WorkDistributionDto getWorkDistribution(String sprintId, String projectName) {
        log.info("Calculating work distribution for sprint: {}", sprintId);
        
        // Verify sprint exists
        sprintRepository.findById(sprintId)
                .orElseThrow(() -> new RuntimeException("Sprint not found: " + sprintId));

        List<Task> tasks = taskRepository.findBySprintId(sprintId);
        
        if (tasks.isEmpty()) {
            log.info("No tasks found for sprint {}. Returning empty work distribution.", sprintId);
            return new WorkDistributionDto(List.of(), List.of(), List.of(), 0);
        }

        // Count tasks by category
        Map<String, Integer> categoryCount = new HashMap<>();
        categoryCount.put("Features", 0);
        categoryCount.put("Tech Debt", 0);
        categoryCount.put("Bug Fixes", 0);
        categoryCount.put("Support", 0);

        for (Task task : tasks) {
            Task.TaskCategory category = task.getCategory();
            if (category == null) {
                category = Task.TaskCategory.OTHER;
            }
            
            switch (category) {
                case FEATURE:
                    categoryCount.put("Features", categoryCount.get("Features") + 1);
                    break;
                case TECH_DEBT:
                    categoryCount.put("Tech Debt", categoryCount.get("Tech Debt") + 1);
                    break;
                case PROD_ISSUE:
                    categoryCount.put("Bug Fixes", categoryCount.get("Bug Fixes") + 1);
                    break;
                case OTHER:
                    categoryCount.put("Support", categoryCount.get("Support") + 1);
                    break;
            }
        }

        // Build result lists
        List<String> labels = new ArrayList<>();
        List<BigDecimal> values = new ArrayList<>();
        List<BigDecimal> percentages = new ArrayList<>();
        int totalTasks = tasks.size();

        for (Map.Entry<String, Integer> entry : categoryCount.entrySet()) {
            if (entry.getValue() > 0) {  // Only include categories that have tasks
                labels.add(entry.getKey());
                values.add(BigDecimal.valueOf(entry.getValue()));
                
                BigDecimal percentage = BigDecimal.valueOf(entry.getValue())
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalTasks), 2, RoundingMode.HALF_UP);
                percentages.add(percentage);
            }
        }

        log.info("Work distribution calculated: Total tasks={}, Categories={}", totalTasks, labels.size());
        return new WorkDistributionDto(labels, values, percentages, totalTasks);
    }

    @Override
    @Transactional(readOnly = true)
    public VelocityTrendDto getVelocityTrend(String currentSprintId) {
        log.info("Calculating velocity trend for current sprint: {}", currentSprintId);
        
        // Get current sprint
        Sprint currentSprint = sprintRepository.findById(currentSprintId)
                .orElseThrow(() -> new RuntimeException("Sprint not found: " + currentSprintId));
        
        // Get last 5 archived (completed) sprints
        List<Sprint> archivedSprints = sprintRepository.findArchivedSprintsOrderByEndDateDesc();
        List<Sprint> last5ArchivedSprints = archivedSprints.stream()
                .limit(5)
                .collect(Collectors.toList());
        
        log.info("Found {} archived sprints, using last 5", archivedSprints.size());
        
        // Build list with current sprint + last 5 archived sprints (in chronological order)
        List<Sprint> allSprints = new ArrayList<>();
        // Add archived sprints in reverse order (oldest to newest)
        for (int i = last5ArchivedSprints.size() - 1; i >= 0; i--) {
            allSprints.add(last5ArchivedSprints.get(i));
        }
        // Add current sprint at the end
        allSprints.add(currentSprint);
        
        // Calculate velocity for each sprint
        List<VelocityTrendDto.SprintVelocityData> sprintVelocities = new ArrayList<>();
        BigDecimal totalCompletedPoints = BigDecimal.ZERO;
        int completedSprintCount = 0;
        
        for (Sprint sprint : allSprints) {
            List<Task> tasks = taskRepository.findBySprintId(sprint.getId());
            
            // Calculate committed points (original estimates)
            BigDecimal committedPoints = tasks.stream()
                    .map(this::resolveOriginalEstimate)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Calculate completed points (time spent or completed work)
            BigDecimal completedPoints = tasks.stream()
                    .map(task -> task.getTimeSpent() != null ? task.getTimeSpent() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            VelocityTrendDto.SprintVelocityData velocityData = new VelocityTrendDto.SprintVelocityData(
                    sprint.getId(),
                    sprint.getSprintName(),
                    committedPoints,
                    completedPoints,
                    sprint.getStatus().toString()
            );
            
            sprintVelocities.add(velocityData);
            log.debug("Sprint: {}, Committed: {}, Completed: {}", 
                    sprint.getSprintName(), committedPoints, completedPoints);
            
            // Track completed points for average calculation (only for archived sprints)
            if (sprint.getStatus() == Sprint.SprintStatus.ARCHIVED) {
                totalCompletedPoints = totalCompletedPoints.add(completedPoints);
                completedSprintCount++;
            }
        }
        
        // Calculate average velocity (only from completed sprints)
        BigDecimal averageVelocity = BigDecimal.ZERO;
        if (completedSprintCount > 0) {
            averageVelocity = totalCompletedPoints.divide(
                    BigDecimal.valueOf(completedSprintCount), 
                    2, 
                    RoundingMode.HALF_UP
            );
        }
        
        log.info("Velocity trend calculated: {} sprints, Average velocity: {}", 
                sprintVelocities.size(), averageVelocity);
        
        return new VelocityTrendDto(sprintVelocities, averageVelocity);
    }
}

