package com.sprintpilot.service.impl;

import com.sprintpilot.dto.MemberUtilizationDto;
import com.sprintpilot.dto.MemberUtilizationDto.UtilizationStatus;
import com.sprintpilot.entity.Sprint;
import com.sprintpilot.entity.Task;
import com.sprintpilot.entity.TeamMember;
import com.sprintpilot.repository.SprintRepository;
import com.sprintpilot.repository.TaskRepository;
import com.sprintpilot.repository.TeamMemberRepository;
import com.sprintpilot.service.MemberService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service implementation for team member operations
 */
@Slf4j
@Service
@Transactional
public class MemberServiceImpl implements MemberService {

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private SprintRepository sprintRepository;

    @Value("${app.utilization.ideal-gap-threshold:5}")
    private BigDecimal idealGapThreshold;

    @Override
    @Transactional(readOnly = true)
    public List<MemberUtilizationDto> getMemberUtilizationBySprintId(String sprintId) {
        log.info("Calculating member utilization for sprint: {}", sprintId);

        // 1. Get sprint details
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new RuntimeException("Sprint not found: " + sprintId));

        // 2. Calculate days remaining in sprint
        LocalDate today = LocalDate.now();
        LocalDate endDate = sprint.getEndDate();
        
        long daysRemaining = 0;
        if (endDate != null && endDate.isAfter(today)) {
            daysRemaining = ChronoUnit.DAYS.between(today, endDate);
        }
        log.info("Days remaining in sprint: {}", daysRemaining);

        // 3. Get all tasks in the sprint
        List<Task> tasks = taskRepository.findBySprintId(sprintId);
        log.info("Found {} tasks in sprint", tasks.size());

        // 4. Group tasks by assignee and calculate remaining estimates
        Map<String, BigDecimal> memberRemainingWork = new HashMap<>();
        Map<String, TeamMember> memberMap = new HashMap<>();

        for (Task task : tasks) {
            log.debug("Processing task: {} ({}), StoryPoints: {}, TimeSpent: {}, Status: {}, Assignees: {}", 
                    task.getTaskKey(), task.getSummary(), task.getStoryPoints(), task.getTimeSpent(), 
                    task.getStatus(), task.getAssignees() != null ? task.getAssignees().size() : 0);
            
            // Skip DONE tasks as they are completed
            if (task.getStatus() == Task.TaskStatus.DONE) {
                log.debug("Skipping DONE task: {}", task.getTaskKey());
                continue;
            }
            
            if (task.getAssignees() != null && !task.getAssignees().isEmpty()) {
                // Calculate remaining estimate for this task
                BigDecimal storyPoints = task.getStoryPoints() != null ? task.getStoryPoints() : BigDecimal.ZERO;
                BigDecimal timeSpent = task.getTimeSpent() != null ? task.getTimeSpent() : BigDecimal.ZERO;
                BigDecimal remainingEstimate = storyPoints.subtract(timeSpent);
                
                log.debug("Task {} - Remaining estimate: {} (StoryPoints: {} - TimeSpent: {})", 
                        task.getTaskKey(), remainingEstimate, storyPoints, timeSpent);
                
                // Only consider positive remaining estimates (tasks not overdue on time)
                if (remainingEstimate.compareTo(BigDecimal.ZERO) > 0) {
                    // Add to each assignee's workload
                    for (TeamMember member : task.getAssignees()) {
                        log.debug("Adding {} hours to member: {}", remainingEstimate, member.getName());
                        memberMap.put(member.getId(), member);
                        memberRemainingWork.merge(member.getId(), remainingEstimate, BigDecimal::add);
                    }
                } else {
                    log.debug("Task {} has non-positive remaining estimate: {}, skipping", 
                            task.getTaskKey(), remainingEstimate);
                }
            } else {
                log.debug("Task {} has no assignees, skipping", task.getTaskKey());
            }
        }
        
        log.info("Member workload summary: {}", memberRemainingWork);

        // 5. Calculate utilization for each member
        List<MemberUtilizationDto> utilizations = new ArrayList<>();
        
        for (Map.Entry<String, TeamMember> entry : memberMap.entrySet()) {
            String memberId = entry.getKey();
            TeamMember member = entry.getValue();
            
            BigDecimal remainingWork = memberRemainingWork.getOrDefault(memberId, BigDecimal.ZERO);
            BigDecimal dailyCapacity = member.getDailyCapacity() != null ? member.getDailyCapacity() : BigDecimal.ZERO;
            BigDecimal capacity = dailyCapacity.multiply(BigDecimal.valueOf(daysRemaining));
            BigDecimal gap = remainingWork.subtract(capacity);
            
            UtilizationStatus status = determineUtilizationStatus(gap);
            
            MemberUtilizationDto dto = new MemberUtilizationDto(
                    memberId,
                    member.getName(),
                    remainingWork,
                    capacity,
                    gap,
                    status
            );
            
            utilizations.add(dto);
            log.debug("Member: {}, Remaining: {}, Capacity: {}, Gap: {}, Status: {}", 
                    member.getName(), remainingWork, capacity, gap, status);
        }

        // 6. Also include members with no tasks assigned
        List<TeamMember> allActiveMembers = teamMemberRepository.findAll().stream()
                .filter(member -> member.getActive() != null && member.getActive())
                .toList();
        
        for (TeamMember member : allActiveMembers) {
            if (!memberMap.containsKey(member.getId())) {
                BigDecimal dailyCapacity = member.getDailyCapacity() != null ? member.getDailyCapacity() : BigDecimal.ZERO;
                BigDecimal capacity = dailyCapacity.multiply(BigDecimal.valueOf(daysRemaining));
                BigDecimal gap = BigDecimal.ZERO.subtract(capacity);
                
                UtilizationStatus status = determineUtilizationStatus(gap);
                
                MemberUtilizationDto dto = new MemberUtilizationDto(
                        member.getId(),
                        member.getName(),
                        BigDecimal.ZERO,
                        capacity,
                        gap,
                        status
                );
                
                utilizations.add(dto);
            }
        }

        log.info("Calculated utilization for {} members", utilizations.size());
        return utilizations;
    }

    /**
     * Determine utilization status based on gap
     * 
     * @param gap Gap between remaining work and capacity
     * @return Utilization status
     */
    private UtilizationStatus determineUtilizationStatus(BigDecimal gap) {
        // gap > idealGapThreshold: OVER_UTILIZED (too much work)
        if (gap.compareTo(idealGapThreshold) > 0) {
            return UtilizationStatus.OVER_UTILIZED;
        }
        
        // gap < -idealGapThreshold: UNDER_UTILIZED (not enough work)
        BigDecimal negativeThreshold = idealGapThreshold.negate();
        if (gap.compareTo(negativeThreshold) < 0) {
            return UtilizationStatus.UNDER_UTILIZED;
        }
        
        // Within threshold: PROPERLY_UTILIZED
        return UtilizationStatus.PROPERLY_UTILIZED;
    }
}

