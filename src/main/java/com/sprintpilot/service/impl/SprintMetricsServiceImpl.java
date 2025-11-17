package com.sprintpilot.service.impl;

import com.sprintpilot.dto.SprintMetricsDto;
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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
}

