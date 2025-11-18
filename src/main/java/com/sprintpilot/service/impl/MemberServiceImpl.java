package com.sprintpilot.service.impl;

import com.sprintpilot.dto.MemberUtilizationDto;
import com.sprintpilot.dto.MemberUtilizationDto.UtilizationStatus;
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
import com.sprintpilot.repository.TeamMemberRepository;
import com.sprintpilot.service.MemberService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @Autowired
    private HolidayRepository holidayRepository;

    @Autowired
    private LeaveDayRepository leaveDayRepository;

    @Autowired
    private SprintEventRepository sprintEventRepository;

    @Value("${app.utilization.ideal-gap-threshold:5}")
    private BigDecimal idealGapThreshold;

    @Override
    @Transactional(readOnly = true)
    public List<MemberUtilizationDto> getMemberUtilizationBySprintId(String sprintId) {
        log.info("Calculating member utilization for sprint: {}", sprintId);

        // 1. Get sprint details
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new RuntimeException("Sprint not found: " + sprintId));

        // 2. Get only members who are assigned to THIS sprint
        List<TeamMember> sprintMembers = teamMemberRepository.findBySprintId(sprintId);
        log.info("Found {} members assigned to sprint {}", sprintMembers.size(), sprintId);
        
        // Create a map of sprint member IDs for quick lookup
        Map<String, TeamMember> sprintMemberMap = new HashMap<>();
        for (TeamMember member : sprintMembers) {
            sprintMemberMap.put(member.getId(), member);
        }

        // 3. Get all tasks in the sprint
        List<Task> tasks = taskRepository.findBySprintId(sprintId);
        log.info("Found {} tasks in sprint", tasks.size());

        // 4. Group tasks by assignee and calculate total assigned work (original estimates)
        // Only for members who are part of the sprint team
        Map<String, BigDecimal> memberAssignedWork = new HashMap<>();

        for (Task task : tasks) {
            log.debug("Processing task: {} ({}), OriginalEstimate: {}, StoryPoints: {}, Assignees: {}", 
                    task.getTaskKey(), task.getSummary(), task.getOriginalEstimate(), task.getStoryPoints(), 
                    task.getAssignees() != null ? task.getAssignees().size() : 0);
            
            if (task.getAssignees() != null && !task.getAssignees().isEmpty()) {
                // Use original estimate (or story points as fallback) - this is the TOTAL assigned work
                BigDecimal storyPoints = task.getStoryPoints() != null ? task.getStoryPoints() : BigDecimal.ZERO;
                BigDecimal originalEstimate = task.getOriginalEstimate() != null && task.getOriginalEstimate().compareTo(BigDecimal.ZERO) > 0
                        ? task.getOriginalEstimate()
                        : storyPoints;
                
                log.debug("Task {} - Assigned work: {}", task.getTaskKey(), originalEstimate);
                
                // Add original estimate to each assignee's total assigned workload
                // BUT only if the assignee is part of the sprint team
                if (originalEstimate.compareTo(BigDecimal.ZERO) > 0) {
                    for (TeamMember member : task.getAssignees()) {
                        // Check if this member is part of the sprint team
                        if (sprintMemberMap.containsKey(member.getId())) {
                            log.debug("Adding {} hours to sprint member: {}", originalEstimate, member.getName());
                            memberAssignedWork.merge(member.getId(), originalEstimate, BigDecimal::add);
                        } else {
                            log.debug("Skipping member {} - not part of sprint team", member.getName());
                        }
                    }
                } else {
                    log.debug("Task {} has no estimate, skipping", task.getTaskKey());
                }
            } else {
                log.debug("Task {} has no assignees, skipping", task.getTaskKey());
            }
        }
        
        log.info("Member assigned work summary: {}", memberAssignedWork);

        // 5. Calculate utilization for each sprint member
        List<MemberUtilizationDto> utilizations = new ArrayList<>();
        
        for (TeamMember member : sprintMembers) {
            BigDecimal assignedWork = memberAssignedWork.getOrDefault(member.getId(), BigDecimal.ZERO);
            BigDecimal capacity = calculateMemberCapacity(member, sprint);
            BigDecimal gap = assignedWork.subtract(capacity);
            
            UtilizationStatus status = determineUtilizationStatus(gap);
            
            MemberUtilizationDto dto = new MemberUtilizationDto(
                    member.getId(),
                    member.getName(),
                    assignedWork,
                    capacity,
                    gap,
                    status
            );
            
            utilizations.add(dto);
            log.debug("Member: {}, Assigned Work: {}, Capacity: {}, Gap: {}, Status: {}", 
                    member.getName(), assignedWork, capacity, gap, status);
        }

        log.info("Calculated utilization for {} members in sprint", utilizations.size());
        return utilizations;
    }

    /**
     * Calculate total capacity for a team member in a sprint (accounting for holidays and leave days)
     * 
     * @param member Team member
     * @param sprint Sprint
     * @return Total capacity in hours
     */
    private BigDecimal calculateMemberCapacity(TeamMember member, Sprint sprint) {
        // Calculate total BUSINESS days in sprint (excluding weekends)
        long businessDays = countBusinessDays(sprint.getStartDate(), sprint.getEndDate());
        
        // Get unique holiday dates to avoid double-counting
        // (if same date is in both holidays table and sprint events)
        java.util.Set<java.time.LocalDate> uniqueHolidayDates = new java.util.HashSet<>();
        
        // Add holidays from holidays table
        holidayRepository.findByDateRange(sprint.getStartDate(), sprint.getEndDate())
                .forEach(holiday -> uniqueHolidayDates.add(holiday.getHolidayDate()));
        
        // Add holidays from sprint events
        sprintEventRepository.findBySprintIdAndEventTypeOrderByEventDate(
                sprint.getId(), 
                SprintEvent.EventType.HOLIDAY
        ).forEach(event -> uniqueHolidayDates.add(event.getEventDate()));
        
        // Count only holidays that fall on business days (not weekends)
        long businessDayHolidays = uniqueHolidayDates.stream()
                .filter(this::isBusinessDay)
                .count();
        
        // Get leave days during sprint for this member
        List<LeaveDay> leaveDaysInSprint = leaveDayRepository.findByMemberIdAndSprintId(
                member.getId(),
                sprint.getId()
        );
        
        // Count only leave days that fall on business days (not weekends)
        long businessDayLeaves = leaveDaysInSprint.stream()
                .map(LeaveDay::getLeaveDate)
                .filter(this::isBusinessDay)
                .count();
        
        // Calculate available days (business days - holidays on business days - leave days on business days)
        long availableDays = businessDays - businessDayHolidays - businessDayLeaves;
        
        // Ensure availableDays is not negative
        availableDays = Math.max(0, availableDays);
        
        // Calculate total capacity
        BigDecimal dailyCapacity = member.getDailyCapacity() != null ? member.getDailyCapacity() : BigDecimal.ZERO;
        BigDecimal totalCapacity = dailyCapacity
                .multiply(BigDecimal.valueOf(availableDays))
                .setScale(2, RoundingMode.HALF_UP);
        
        log.debug("Capacity calculation for {}: businessDays={}, businessDayHolidays={}, businessDayLeaves={}, availableDays={}, dailyCapacity={}, totalCapacity={}",
                member.getName(), businessDays, businessDayHolidays, businessDayLeaves, 
                availableDays, dailyCapacity, totalCapacity);
        
        return totalCapacity;
    }
    
    /**
     * Count business days (Monday-Friday) between two dates (inclusive)
     * 
     * @param startDate Start date
     * @param endDate End date
     * @return Number of business days
     */
    private long countBusinessDays(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        long businessDays = 0;
        java.time.LocalDate current = startDate;
        
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
     * 
     * @param date Date to check
     * @return true if business day, false if weekend
     */
    private boolean isBusinessDay(java.time.LocalDate date) {
        java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != java.time.DayOfWeek.SATURDAY && dayOfWeek != java.time.DayOfWeek.SUNDAY;
    }

    /**
     * Determine utilization status based on gap
     * 
     * @param gap Gap between assigned work and capacity
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

