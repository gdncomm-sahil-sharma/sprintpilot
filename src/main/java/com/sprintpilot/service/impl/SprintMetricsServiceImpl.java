package com.sprintpilot.service.impl;

import com.sprintpilot.dto.CurrentSprintMetricsDto;
import com.sprintpilot.dto.MemberUtilizationDto;
import com.sprintpilot.dto.QuickStatsDto;
import com.sprintpilot.dto.SprintMetricsDto;
import com.sprintpilot.dto.SprintSummaryMetricsDto;
import com.sprintpilot.dto.VelocityTrendDto;
import com.sprintpilot.dto.WorkDistributionDto;
import com.sprintpilot.entity.Holiday;
import com.sprintpilot.entity.LeaveDay;
import com.sprintpilot.entity.Sprint;
import com.sprintpilot.entity.SprintEvent;
import com.sprintpilot.entity.Task;
import com.sprintpilot.entity.TeamMember;
import com.sprintpilot.repository.HolidayRepository;
import com.sprintpilot.repository.LeaveDayRepository;
import com.sprintpilot.repository.SprintEventRepository;
import com.sprintpilot.repository.SprintRepository;
import com.sprintpilot.repository.TaskRepository;
import com.sprintpilot.service.MemberService;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SprintMetricsServiceImpl implements SprintMetricsService {

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private MemberService memberService;

    @Autowired
    private HolidayRepository holidayRepository;

    @Autowired
    private SprintEventRepository sprintEventRepository;
    
    @Autowired
    private LeaveDayRepository leaveDayRepository;

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
            
            // Calculate committed points (story points)
            BigDecimal committedPoints = tasks.stream()
                    .map(task -> task.getStoryPoints() != null ? task.getStoryPoints() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Calculate completed points (story points of completed tasks only)
            BigDecimal completedPoints = tasks.stream()
                    .filter(task -> task.getStatus() == Task.TaskStatus.DONE)
                    .map(task -> task.getStoryPoints() != null ? task.getStoryPoints() : BigDecimal.ZERO)
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

    @Override
    @Transactional(readOnly = true)
    public SprintSummaryMetricsDto getSummaryMetrics(String currentSprintId) {
        log.info("Calculating summary metrics for sprint: {}", currentSprintId);
        
        // Verify sprint exists
        sprintRepository.findById(currentSprintId)
                .orElseThrow(() -> new RuntimeException("Sprint not found: " + currentSprintId));
        
        // Get velocity trend data (includes current + last 5 sprints)
        VelocityTrendDto velocityTrend = getVelocityTrend(currentSprintId);
        
        // Calculate velocity metric
        SprintSummaryMetricsDto.VelocityMetric velocityMetric = calculateVelocityMetric(
                currentSprintId, velocityTrend);
        
        // Calculate success rate metric
        SprintSummaryMetricsDto.SuccessRateMetric successRateMetric = calculateSuccessRateMetric(
                currentSprintId);
        
        // Calculate cycle time metric
        SprintSummaryMetricsDto.CycleTimeMetric cycleTimeMetric = calculateCycleTimeMetric(
                currentSprintId);
        
        // Calculate utilization metric
        SprintSummaryMetricsDto.UtilizationMetric utilizationMetric = calculateUtilizationMetric(
                currentSprintId);
        
        log.info("Summary metrics calculated successfully for sprint: {}", currentSprintId);
        
        return new SprintSummaryMetricsDto(
                velocityMetric,
                successRateMetric,
                cycleTimeMetric,
                utilizationMetric
        );
    }

    private SprintSummaryMetricsDto.VelocityMetric calculateVelocityMetric(
            String currentSprintId, VelocityTrendDto velocityTrend) {
        
        List<VelocityTrendDto.SprintVelocityData> sprints = velocityTrend.sprints();
        if (sprints.isEmpty()) {
            return new SprintSummaryMetricsDto.VelocityMetric(
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "neutral");
        }
        
        // Current sprint is the last one in the list
        VelocityTrendDto.SprintVelocityData currentSprintData = sprints.get(sprints.size() - 1);
        BigDecimal currentVelocity = currentSprintData.completedPoints();
        BigDecimal averageVelocity = velocityTrend.averageVelocity();
        
        // Calculate percentage change from previous sprint
        BigDecimal percentageChange = BigDecimal.ZERO;
        String trend = "neutral";
        
        if (sprints.size() > 1) {
            VelocityTrendDto.SprintVelocityData previousSprintData = sprints.get(sprints.size() - 2);
            BigDecimal previousVelocity = previousSprintData.completedPoints();
            
            if (previousVelocity.compareTo(BigDecimal.ZERO) > 0) {
                percentageChange = currentVelocity
                        .subtract(previousVelocity)
                        .divide(previousVelocity, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(1, RoundingMode.HALF_UP);
                
                if (percentageChange.compareTo(BigDecimal.ZERO) > 0) {
                    trend = "up";
                } else if (percentageChange.compareTo(BigDecimal.ZERO) < 0) {
                    trend = "down";
                }
            }
        }
        
        log.debug("Velocity metric: current={}, average={}, change={}%, trend={}", 
                currentVelocity, averageVelocity, percentageChange, trend);
        
        return new SprintSummaryMetricsDto.VelocityMetric(
                currentVelocity, averageVelocity, percentageChange, trend);
    }

    private SprintSummaryMetricsDto.SuccessRateMetric calculateSuccessRateMetric(
            String currentSprintId) {
        
        List<Task> currentTasks = taskRepository.findBySprintId(currentSprintId);
        
        if (currentTasks.isEmpty()) {
            return new SprintSummaryMetricsDto.SuccessRateMetric(
                    BigDecimal.ZERO, BigDecimal.ZERO, "neutral");
        }
        
        // Calculate current success rate
        long completedTasks = currentTasks.stream()
                .filter(task -> task.getStatus() == Task.TaskStatus.DONE)
                .count();
        
        BigDecimal currentSuccessRate = BigDecimal.valueOf(completedTasks)
                .divide(BigDecimal.valueOf(currentTasks.size()), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);
        
        // Get previous sprint for comparison
        List<Sprint> archivedSprints = sprintRepository.findArchivedSprintsOrderByEndDateDesc();
        BigDecimal percentageChange = BigDecimal.ZERO;
        String trend = "neutral";
        
        if (!archivedSprints.isEmpty()) {
            Sprint previousSprint = archivedSprints.get(0); // Most recent archived sprint
            List<Task> previousTasks = taskRepository.findBySprintId(previousSprint.getId());
            
            if (!previousTasks.isEmpty()) {
                long previousCompletedTasks = previousTasks.stream()
                        .filter(task -> task.getStatus() == Task.TaskStatus.DONE)
                        .count();
                
                BigDecimal previousSuccessRate = BigDecimal.valueOf(previousCompletedTasks)
                        .divide(BigDecimal.valueOf(previousTasks.size()), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                
                percentageChange = currentSuccessRate.subtract(previousSuccessRate)
                        .setScale(1, RoundingMode.HALF_UP);
                
                if (percentageChange.compareTo(BigDecimal.ZERO) > 0) {
                    trend = "up";
                } else if (percentageChange.compareTo(BigDecimal.ZERO) < 0) {
                    trend = "down";
                }
            }
        }
        
        log.debug("Success rate metric: current={}%, change={}%, trend={}", 
                currentSuccessRate, percentageChange, trend);
        
        return new SprintSummaryMetricsDto.SuccessRateMetric(
                currentSuccessRate, percentageChange, trend);
    }

    private SprintSummaryMetricsDto.CycleTimeMetric calculateCycleTimeMetric(
            String currentSprintId) {
        
        List<Task> currentTasks = taskRepository.findBySprintId(currentSprintId);
        
        // Calculate current average cycle time (for completed tasks)
        List<Task> completedTasks = currentTasks.stream()
                .filter(task -> task.getStatus() == Task.TaskStatus.DONE)
                .filter(task -> task.getCreatedAt() != null && task.getUpdatedAt() != null)
                .collect(Collectors.toList());
        
        BigDecimal currentCycleTime = BigDecimal.ZERO;
        if (!completedTasks.isEmpty()) {
            long totalDays = completedTasks.stream()
                    .mapToLong(task -> ChronoUnit.DAYS.between(
                            task.getCreatedAt().toLocalDate(), 
                            task.getUpdatedAt().toLocalDate()))
                    .sum();
            
            currentCycleTime = BigDecimal.valueOf(totalDays)
                    .divide(BigDecimal.valueOf(completedTasks.size()), 1, RoundingMode.HALF_UP);
        }
        
        // Calculate baseline (average from all archived sprints)
        List<Sprint> archivedSprints = sprintRepository.findArchivedSprintsOrderByEndDateDesc();
        BigDecimal baseline = BigDecimal.ZERO;
        int totalCompletedTasksInArchive = 0;
        long totalCycleTimeDays = 0;
        
        for (Sprint sprint : archivedSprints) {
            List<Task> sprintTasks = taskRepository.findBySprintId(sprint.getId());
            List<Task> sprintCompletedTasks = sprintTasks.stream()
                    .filter(task -> task.getStatus() == Task.TaskStatus.DONE)
                    .filter(task -> task.getCreatedAt() != null && task.getUpdatedAt() != null)
                    .collect(Collectors.toList());
            
            for (Task task : sprintCompletedTasks) {
                totalCycleTimeDays += ChronoUnit.DAYS.between(
                        task.getCreatedAt().toLocalDate(), 
                        task.getUpdatedAt().toLocalDate());
            }
            
            totalCompletedTasksInArchive += sprintCompletedTasks.size();
        }
        
        if (totalCompletedTasksInArchive > 0) {
            baseline = BigDecimal.valueOf(totalCycleTimeDays)
                    .divide(BigDecimal.valueOf(totalCompletedTasksInArchive), 1, RoundingMode.HALF_UP);
        }
        
        BigDecimal difference = currentCycleTime.subtract(baseline);
        String trend = "neutral";
        if (difference.compareTo(BigDecimal.ZERO) > 0) {
            trend = "up"; // Higher cycle time is worse
        } else if (difference.compareTo(BigDecimal.ZERO) < 0) {
            trend = "down"; // Lower cycle time is better
        }
        
        log.debug("Cycle time metric: current={} days, baseline={} days, difference={}, trend={}", 
                currentCycleTime, baseline, difference, trend);
        
        return new SprintSummaryMetricsDto.CycleTimeMetric(
                currentCycleTime, baseline, difference, trend);
    }

    private SprintSummaryMetricsDto.UtilizationMetric calculateUtilizationMetric(
            String currentSprintId) {
        
        List<MemberUtilizationDto> utilizations = memberService.getMemberUtilizationBySprintId(currentSprintId);
        
        if (utilizations.isEmpty()) {
            return new SprintSummaryMetricsDto.UtilizationMetric(BigDecimal.ZERO, "under");
        }
        
        // Calculate average utilization across all team members
        BigDecimal totalUtilization = BigDecimal.ZERO;
        
        for (MemberUtilizationDto util : utilizations) {
            // Calculate utilization % = (assigned work / capacity) * 100
            if (util.capacity().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal memberUtilization = util.remainingEstimate()
                        .divide(util.capacity(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                totalUtilization = totalUtilization.add(memberUtilization);
            }
        }
        
        BigDecimal averageUtilization = totalUtilization
                .divide(BigDecimal.valueOf(utilizations.size()), 1, RoundingMode.HALF_UP);
        
        // Determine status based on utilization percentage
        String status;
        if (averageUtilization.compareTo(BigDecimal.valueOf(90)) > 0) {
            status = "over"; // Over 90% - overutilized
        } else if (averageUtilization.compareTo(BigDecimal.valueOf(70)) < 0) {
            status = "under"; // Under 70% - underutilized
        } else {
            status = "optimal"; // 70-90% - optimal range
        }
        
        log.debug("Utilization metric: average={}%, status={}", averageUtilization, status);
        
        return new SprintSummaryMetricsDto.UtilizationMetric(averageUtilization, status);
    }

    @Override
    @Transactional(readOnly = true)
    public CurrentSprintMetricsDto getCurrentSprintMetrics(String currentSprintId) {
        log.info("Calculating current sprint metrics for sprint: {}", currentSprintId);
        
        // Get sprint details
        Sprint sprint = sprintRepository.findById(currentSprintId)
                .orElseThrow(() -> new RuntimeException("Sprint not found: " + currentSprintId));
        
        // Get all tasks in sprint
        List<Task> tasks = taskRepository.findBySprintId(currentSprintId);
        
        // Calculate Sprint Progress
        CurrentSprintMetricsDto.SprintProgressMetric sprintProgress = calculateSprintProgress(sprint);
        
        // Calculate Work Remaining
        CurrentSprintMetricsDto.WorkRemainingMetric workRemaining = calculateWorkRemaining(tasks, sprint);
        
        // Calculate Tasks Completed
        CurrentSprintMetricsDto.TasksCompletedMetric tasksCompleted = calculateTasksCompleted(tasks);
        
        // Calculate Team Utilization (reuse existing logic)
        CurrentSprintMetricsDto.UtilizationMetric utilization = calculateCurrentUtilizationMetric(currentSprintId);
        
        log.info("Current sprint metrics calculated successfully for sprint: {}", currentSprintId);
        
        return new CurrentSprintMetricsDto(sprintProgress, workRemaining, tasksCompleted, utilization);
    }

    private CurrentSprintMetricsDto.SprintProgressMetric calculateSprintProgress(Sprint sprint) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = sprint.getStartDate();
        LocalDate endDate = sprint.getEndDate();
        
        // Collect all unique holiday dates (from both holidays table and sprint events)
        java.util.Set<LocalDate> uniqueHolidayDates = new java.util.HashSet<>();
        holidayRepository.findByDateRange(startDate, endDate)
                .forEach(holiday -> uniqueHolidayDates.add(holiday.getHolidayDate()));
        sprintEventRepository.findBySprintIdAndEventTypeOrderByEventDate(
                sprint.getId(), 
                SprintEvent.EventType.HOLIDAY
        ).forEach(event -> uniqueHolidayDates.add(event.getEventDate()));
        
        // Calculate total working days in sprint (business days - holidays)
        long totalBusinessDays = countBusinessDays(startDate, endDate);
        long businessDayHolidays = uniqueHolidayDates.stream()
                .filter(this::isBusinessDay)
                .count();
        long totalWorkingDays = totalBusinessDays - businessDayHolidays;
        totalWorkingDays = Math.max(0, totalWorkingDays); // Ensure not negative
        
        // Calculate working days elapsed (from start to today, capped at sprint end)
        long workingDaysElapsed;
        if (today.isBefore(startDate)) {
            workingDaysElapsed = 0;
        } else {
            LocalDate cappedToday = today.isAfter(endDate) ? endDate : today;
            long businessDaysElapsed = countBusinessDays(startDate, cappedToday);
            
            // Count holidays that fall on business days within elapsed period
            long businessDayHolidaysElapsed = uniqueHolidayDates.stream()
                    .filter(date -> !date.isBefore(startDate) && !date.isAfter(cappedToday))
                    .filter(this::isBusinessDay)
                    .count();
            
            workingDaysElapsed = businessDaysElapsed - businessDayHolidaysElapsed;
            workingDaysElapsed = Math.max(0, workingDaysElapsed);
        }
        
        // Calculate percentage complete
        BigDecimal percentComplete = BigDecimal.ZERO;
        if (totalWorkingDays > 0) {
            percentComplete = BigDecimal.valueOf(workingDaysElapsed)
                    .divide(BigDecimal.valueOf(totalWorkingDays), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(1, RoundingMode.HALF_UP);
        }
        
        log.debug("Sprint progress: {}% complete, working day {} of {} (excluding weekends and holidays)", 
                percentComplete, workingDaysElapsed, totalWorkingDays);
        
        return new CurrentSprintMetricsDto.SprintProgressMetric(
                percentComplete,
                (int) workingDaysElapsed,
                (int) totalWorkingDays
        );
    }

    private CurrentSprintMetricsDto.WorkRemainingMetric calculateWorkRemaining(List<Task> tasks, Sprint sprint) {
        // Calculate total remaining work (original estimate - time spent)
        BigDecimal totalRemaining = BigDecimal.ZERO;
        
        for (Task task : tasks) {
            BigDecimal originalEstimate = resolveOriginalEstimate(task);
            BigDecimal timeSpent = task.getTimeSpent() != null ? task.getTimeSpent() : BigDecimal.ZERO;
            BigDecimal remaining = originalEstimate.subtract(timeSpent);
            
            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                totalRemaining = totalRemaining.add(remaining);
            }
        }
        
        // Calculate working days left in sprint (excluding weekends and holidays)
        // Include current day since people typically log work at end of day
        LocalDate today = LocalDate.now();
        LocalDate endDate = sprint.getEndDate();
        
        int workingDaysLeft;
        if (today.isAfter(endDate)) {
            workingDaysLeft = 0;
        } else {
            // Collect all unique holiday dates
            java.util.Set<LocalDate> uniqueHolidayDates = new java.util.HashSet<>();
            holidayRepository.findByDateRange(today, endDate)
                    .forEach(holiday -> uniqueHolidayDates.add(holiday.getHolidayDate()));
            sprintEventRepository.findBySprintIdAndEventTypeOrderByEventDate(
                    sprint.getId(), 
                    SprintEvent.EventType.HOLIDAY
            ).forEach(event -> uniqueHolidayDates.add(event.getEventDate()));
            
            // Count business days from today (inclusive) to end date
            long businessDaysRemaining = countBusinessDays(today, endDate);
            
            // Count holidays that fall on business days from today onwards
            long businessDayHolidays = uniqueHolidayDates.stream()
                    .filter(date -> !date.isBefore(today) && !date.isAfter(endDate))
                    .filter(this::isBusinessDay)
                    .count();
            
            workingDaysLeft = (int) Math.max(0, businessDaysRemaining - businessDayHolidays);
        }
        
        log.debug("Work remaining: {} hours, {} working days left", totalRemaining, workingDaysLeft);
        
        return new CurrentSprintMetricsDto.WorkRemainingMetric(
                totalRemaining.setScale(1, RoundingMode.HALF_UP),
                workingDaysLeft
        );
    }

    private CurrentSprintMetricsDto.TasksCompletedMetric calculateTasksCompleted(List<Task> tasks) {
        int totalTasks = tasks.size();
        
        long completedTasks = tasks.stream()
                .filter(task -> task.getStatus() == Task.TaskStatus.DONE)
                .count();
        
        BigDecimal percentComplete = BigDecimal.ZERO;
        if (totalTasks > 0) {
            percentComplete = BigDecimal.valueOf(completedTasks)
                    .divide(BigDecimal.valueOf(totalTasks), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(1, RoundingMode.HALF_UP);
        }
        
        log.debug("Tasks completed: {} of {} ({}%)", completedTasks, totalTasks, percentComplete);
        
        return new CurrentSprintMetricsDto.TasksCompletedMetric(
                (int) completedTasks,
                totalTasks,
                percentComplete
        );
    }

    private CurrentSprintMetricsDto.UtilizationMetric calculateCurrentUtilizationMetric(String currentSprintId) {
        List<MemberUtilizationDto> utilizations = memberService.getMemberUtilizationBySprintId(currentSprintId);
        
        if (utilizations.isEmpty()) {
            return new CurrentSprintMetricsDto.UtilizationMetric(BigDecimal.ZERO, "under");
        }
        
        // Calculate average utilization across all team members
        BigDecimal totalUtilization = BigDecimal.ZERO;
        
        for (MemberUtilizationDto util : utilizations) {
            // Calculate utilization % = (assigned work / capacity) * 100
            if (util.capacity().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal memberUtilization = util.remainingEstimate()
                        .divide(util.capacity(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                totalUtilization = totalUtilization.add(memberUtilization);
            }
        }
        
        BigDecimal averageUtilization = totalUtilization
                .divide(BigDecimal.valueOf(utilizations.size()), 1, RoundingMode.HALF_UP);
        
        // Determine status based on utilization percentage
        String status;
        if (averageUtilization.compareTo(BigDecimal.valueOf(90)) > 0) {
            status = "over"; // Over 90% - overutilized
        } else if (averageUtilization.compareTo(BigDecimal.valueOf(70)) < 0) {
            status = "under"; // Under 70% - underutilized
        } else {
            status = "optimal"; // 70-90% - optimal range
        }
        
        log.debug("Current utilization metric: average={}%, status={}", averageUtilization, status);
        
        return new CurrentSprintMetricsDto.UtilizationMetric(averageUtilization, status);
    }

    /**
     * Count business days (Monday-Friday) between two dates (inclusive)
     */
    private long countBusinessDays(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            return 0;
        }
        
        long businessDays = 0;
        LocalDate current = startDate;
        
        while (!current.isAfter(endDate)) {
            if (isBusinessDay(current)) {
                businessDays++;
            }
            current = current.plusDays(1);
        }
        
        return businessDays;
    }

    /**
     * Check if a date is a business day (Monday-Friday)
     */
    private boolean isBusinessDay(LocalDate date) {
        java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != java.time.DayOfWeek.SATURDAY && 
               dayOfWeek != java.time.DayOfWeek.SUNDAY;
    }
    
    @Override
    @Transactional(readOnly = true)
    public QuickStatsDto getQuickStats(String sprintId) {
        log.info("Calculating quick stats for sprint: {}", sprintId);
        
        // Get sprint with team members
        Sprint sprint = sprintRepository.findByIdWithTeamMembers(sprintId)
                .orElseThrow(() -> new RuntimeException("Sprint not found: " + sprintId));
        
        // Get team members assigned to this sprint
        List<TeamMember> sprintMembers = sprint.getTeamMembers();
        int teamMembersCount = sprintMembers != null ? sprintMembers.size() : 0;
        
        // Format dates
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");
        String startDate = sprint.getStartDate() != null ? sprint.getStartDate().format(formatter) : "";
        String endDate = sprint.getEndDate() != null ? sprint.getEndDate().format(formatter) : "";
        
        // Calculate assigned hours: sum of original estimates of all tasks
        List<Task> sprintTasks = taskRepository.findBySprintId(sprintId);
        BigDecimal assignedHours = sprintTasks.stream()
                .map(this::resolveOriginalEstimate)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        
        // Calculate capacity hours: sum of total capacity of all sprint members
        BigDecimal capacityHours = BigDecimal.ZERO;
        
        if (sprintMembers != null && !sprintMembers.isEmpty()) {
            for (TeamMember member : sprintMembers) {
                BigDecimal memberCapacity = calculateMemberCapacityForSprint(member, sprint);
                capacityHours = capacityHours.add(memberCapacity);
            }
        }
        
        capacityHours = capacityHours.setScale(2, RoundingMode.HALF_UP);
        
        log.info("Quick Stats - Sprint: {}, Members: {}, Dates: {} → {}, Assigned: {}h, Capacity: {}h", 
                sprint.getSprintName(), teamMembersCount, startDate, endDate, assignedHours, capacityHours);
        
        return new QuickStatsDto(
                sprint.getSprintName(),
                teamMembersCount,
                startDate,
                endDate,
                assignedHours,
                capacityHours
        );
    }
    
    /**
     * Calculate total capacity for a team member in a sprint (accounting for holidays and leave days)
     * Uses the same working day logic as utilization calculation
     */
    private BigDecimal calculateMemberCapacityForSprint(TeamMember member, Sprint sprint) {
        // Calculate total BUSINESS days in sprint (excluding weekends)
        long businessDays = countBusinessDays(sprint.getStartDate(), sprint.getEndDate());
        
        // Get unique holiday dates to avoid double-counting
        Set<LocalDate> uniqueHolidayDates = new HashSet<>();
        
        // Add holidays from holidays table
        List<Holiday> holidays = holidayRepository.findByDateRange(
                sprint.getStartDate(), 
                sprint.getEndDate()
        );
        holidays.forEach(holiday -> uniqueHolidayDates.add(holiday.getHolidayDate()));
        
        // Add holidays from sprint events
        List<SprintEvent> sprintHolidays = sprintEventRepository.findBySprintIdAndEventTypeOrderByEventDate(
                sprint.getId(), 
                SprintEvent.EventType.HOLIDAY
        );
        sprintHolidays.forEach(event -> uniqueHolidayDates.add(event.getEventDate()));
        
        // Count only holidays that fall on business days (not weekends)
        long businessDayHolidays = uniqueHolidayDates.stream()
                .filter(this::isBusinessDay)
                .count();
        
        // Get leave days during sprint for this member
        List<LeaveDay> leaveDays = leaveDayRepository.findByMemberIdAndSprintId(
                member.getId(),
                sprint.getId()
        );
        
        // Count only leave days that fall on business days (not weekends)
        long businessDayLeaves = leaveDays.stream()
                .map(LeaveDay::getLeaveDate)
                .filter(this::isBusinessDay)
                .count();
        
        // Calculate available days (business days - holidays - leaves)
        long availableDays = businessDays - businessDayHolidays - businessDayLeaves;
        
        // Ensure availableDays is not negative
        availableDays = Math.max(0, availableDays);
        
        // Calculate total capacity
        BigDecimal dailyCapacity = member.getDailyCapacity() != null ? 
                member.getDailyCapacity() : BigDecimal.ZERO;
        BigDecimal totalCapacity = dailyCapacity
                .multiply(BigDecimal.valueOf(availableDays))
                .setScale(2, RoundingMode.HALF_UP);
        
        log.debug("Capacity for {}: businessDays={}, holidays={}, leaves={}, availableDays={}, capacity={}h",
                member.getName(), businessDays, businessDayHolidays, businessDayLeaves, 
                availableDays, totalCapacity);
        
        return totalCapacity;
    }
}

