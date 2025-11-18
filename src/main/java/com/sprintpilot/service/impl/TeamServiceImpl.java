package com.sprintpilot.service.impl;

import com.sprintpilot.dto.CapacitySummaryDto;
import com.sprintpilot.dto.SprintAssignmentRequest;
import com.sprintpilot.dto.TeamMemberDto;
import com.sprintpilot.entity.Holiday;
import com.sprintpilot.entity.LeaveDay;
import com.sprintpilot.entity.Sprint;
import com.sprintpilot.entity.SprintEvent;
import com.sprintpilot.entity.SprintTeam;
import com.sprintpilot.entity.TeamMember;
import com.sprintpilot.repository.HolidayRepository;
import com.sprintpilot.repository.LeaveDayRepository;
import com.sprintpilot.repository.SprintEventRepository;
import com.sprintpilot.repository.SprintRepository;
import com.sprintpilot.repository.SprintTeamRepository;
import com.sprintpilot.repository.TeamMemberRepository;
import com.sprintpilot.service.TeamService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TeamServiceImpl implements TeamService {
    
    @Autowired
    private TeamMemberRepository teamMemberRepository;
    
    @Autowired
    private LeaveDayRepository leaveDayRepository;
    
    @Autowired
    private SprintRepository sprintRepository;
    
    @Autowired
    private SprintTeamRepository sprintTeamRepository;
    
    @Autowired
    private HolidayRepository holidayRepository;
    
    @Autowired
    private SprintEventRepository sprintEventRepository;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String INVALID_DATE_FORMAT = "Invalid date format: {}";
    
    @Override
    @Transactional
    public TeamMemberDto createTeamMember(TeamMemberDto memberDto) {
        // Validate input
        if (memberDto.name() == null || memberDto.name().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (memberDto.role() == null) {
            throw new IllegalArgumentException("Role is required");
        }
        if (memberDto.dailyCapacity() == null || memberDto.dailyCapacity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Daily capacity must be positive");
        }
        
        // Create entity
        TeamMember member = new TeamMember();
        member.setName(memberDto.name());
        member.setRole(memberDto.role());
        member.setDailyCapacity(memberDto.dailyCapacity());
        member.setEmail(memberDto.email());
        member.setLocation(memberDto.location());
        member.setActive(memberDto.active() != null ? memberDto.active() : true);
        
        // Save member first
        TeamMember savedMember = teamMemberRepository.save(member);
        
        // Process leave days if provided - save them separately
        if (memberDto.leaveDays() != null && !memberDto.leaveDays().isEmpty()) {
            for (String leaveDateStr : memberDto.leaveDays()) {
                try {
                    LocalDate leaveDate = LocalDate.parse(leaveDateStr, DATE_FORMATTER);
                    LeaveDay leaveDay = new LeaveDay();
                    leaveDay.setMember(savedMember);
                    leaveDay.setLeaveDate(leaveDate);
                    leaveDay.setLeaveType(LeaveDay.LeaveType.PERSONAL);
                    // Save directly to database
                    leaveDayRepository.save(leaveDay);
                } catch (DateTimeParseException e) {
                    // Skip invalid dates
                    log.error(INVALID_DATE_FORMAT, leaveDateStr);
                }
            }
        }
        
        return convertToDto(savedMember);
    }
    
    @Override
    @Transactional
    public TeamMemberDto updateTeamMember(String id, TeamMemberDto memberDto, String sprintId) {
        TeamMember existingMember = teamMemberRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Team member not found: " + id));;
        
        // Update fields
        if (memberDto.name() != null && !memberDto.name().isBlank()) {
            existingMember.setName(memberDto.name());
        }
        if (memberDto.role() != null) {
            existingMember.setRole(memberDto.role());
        }
        if (memberDto.dailyCapacity() != null && memberDto.dailyCapacity().compareTo(BigDecimal.ZERO) > 0) {
            existingMember.setDailyCapacity(memberDto.dailyCapacity());
        }
        if (memberDto.email() != null) {
            existingMember.setEmail(memberDto.email());
        }
        if (memberDto.location() != null) {
            existingMember.setLocation(memberDto.location());
        }
        if (memberDto.active() != null) {
            existingMember.setActive(memberDto.active());
        }
        
        // Save basic member updates first
        teamMemberRepository.save(existingMember);
        
        // Update leave days if provided - use native SQL to completely avoid Hibernate session issues
        if (memberDto.leaveDays() != null) {
            // Delete all existing leave days using native SQL (bypasses Hibernate completely)
            leaveDayRepository.deleteByMemberId(id);
            
            // Flush and clear to ensure clean session state
            entityManager.flush();
            entityManager.clear();
            
            // Re-fetch the member
            existingMember = teamMemberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Team member not found: " + id));
            
            // Add new leave days - save them directly to avoid collection management issues
            // But first validate that the dates are not already holidays
            Sprint sprint = null;
            if (sprintId != null && !sprintId.isBlank()) {
                sprint = sprintRepository.findById(sprintId).orElse(null);
            }
            
            for (String leaveDateStr : memberDto.leaveDays()) {
                try {
                    LocalDate leaveDate = LocalDate.parse(leaveDateStr, DATE_FORMATTER);
                    
                    // Check if this date is already a holiday
                    if (isDateAHoliday(leaveDate, id)) {
                        log.info("Skipping leave date {} for member {} - already a holiday", leaveDateStr, id);
                        continue; // Don't save as leave if it's already a holiday
                    }
                    
                    LeaveDay leaveDay = new LeaveDay();
                    leaveDay.setMember(existingMember);
                    leaveDay.setSprint(sprint); // Assign sprint to leave day
                    leaveDay.setLeaveDate(leaveDate);
                    leaveDay.setLeaveType(LeaveDay.LeaveType.PERSONAL);
                    // Save directly to database, not through collection
                    leaveDayRepository.save(leaveDay);
                    log.debug("Saved leave date {} for member {} in sprint {}", leaveDateStr, id, sprintId);
                } catch (DateTimeParseException e) {
                    // Skip invalid dates
                    log.error(INVALID_DATE_FORMAT, leaveDateStr);
                }
            }
        }
        
        TeamMember updatedMember = teamMemberRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Team member not found: " + id));
        return convertToDto(updatedMember, sprintId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public TeamMemberDto getTeamMemberById(String id) {
        TeamMember member = teamMemberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Team member not found: " + id));
        return convertToDto(member);
    }
    
    @Override
    @Transactional(readOnly = true)
    public TeamMemberDto getTeamMemberById(String id, String sprintId) {
        TeamMember member = teamMemberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Team member not found: " + id));
        return convertToDto(member, getMemberIdsForSprint(sprintId), sprintId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TeamMemberDto> getAllTeamMembers() {
        return getAllTeamMembers(null);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TeamMemberDto> getAllTeamMembers(String sprintId) {
        return teamMemberRepository.findAll().stream()
                .map(member -> convertToDto(member, getMemberIdsForSprint(sprintId), sprintId))
                .toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TeamMemberDto> getActiveTeamMembers(String sprintId) {
        return teamMemberRepository.findActiveMembers().stream()
                .map(member -> convertToDto(member, getMemberIdsForSprint(sprintId), sprintId))
                .toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TeamMemberDto> getTeamMembersByRole(String role) {
        try {
            TeamMember.Role roleEnum = TeamMember.Role.valueOf(role.toUpperCase());
            return teamMemberRepository.findByRole(roleEnum).stream()
                    .map(this::convertToDto)
                    .toList();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + role + ". Valid roles are: BACKEND, FRONTEND, QA, DEVOPS, MANAGER, DESIGNER");
        }
    }
    
    @Override
    @Transactional
    public void deleteTeamMember(String id) {
        TeamMember member = teamMemberRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Team member not found: " + id));;
        member.setDeleted(true);
        member.setActive(false);
        teamMemberRepository.save(member);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CapacitySummaryDto> calculateTeamCapacity(String sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new RuntimeException("Sprint not found: " + sprintId));
        
        List<TeamMember> teamMembers = teamMemberRepository.findBySprintId(sprintId);
        
        return teamMembers.stream()
                .map(member -> calculateMemberCapacity(member, sprint))
                .collect(Collectors.toList());
    }
    
    // Helper methods
    
    private TeamMemberDto convertToDto(TeamMember member) {
        return convertToDto(member, new ArrayList<>(), null);
    }
    
    private TeamMemberDto convertToDto(TeamMember member, String sprintId) {
        return convertToDto(member, new ArrayList<>(), sprintId);
    }
    
    private TeamMemberDto convertToDto(TeamMember member, List<String> assignedMemberIds, String sprintId) {
        // Fetch leave days - filter by sprint if sprintId is provided
        List<String> leaveDays;
        if (sprintId != null && !sprintId.isBlank()) {
            // Get only leave days for the current sprint
            leaveDays = leaveDayRepository.findByMemberIdAndSprintId(member.getId(), sprintId).stream()
                    .map(ld -> ld.getLeaveDate().format(DATE_FORMATTER))
                    .toList();
            log.debug("Fetched {} leave days for member {} in sprint {}", leaveDays.size(), member.getId(), sprintId);
        } else {
            // Get all leave days (backward compatibility)
            leaveDays = leaveDayRepository.findByMemberId(member.getId()).stream()
                    .map(ld -> ld.getLeaveDate().format(DATE_FORMATTER))
                    .toList();
        }
        
        return new TeamMemberDto(
                member.getId(),
                member.getName(),
                member.getRole(),
                member.getDailyCapacity(),
                member.getEmail(),
                member.getLocation(),
                member.getActive(),
                leaveDays,
                assignedMemberIds.contains(member.getId())
        );
    }
    
    private CapacitySummaryDto calculateMemberCapacity(TeamMember member, Sprint sprint) {
        // Calculate working days in sprint
        long sprintDays = ChronoUnit.DAYS.between(sprint.getStartDate(), sprint.getEndDate()) + 1;
        
        // Count holidays in sprint from holidays table
        long holidaysInSprint = holidayRepository.countHolidaysInRange(
                sprint.getStartDate(), 
                sprint.getEndDate()
        );
        
        // Count holidays from sprint events
        List<SprintEvent> sprintHolidayEvents = sprintEventRepository.findBySprintIdAndEventTypeOrderByEventDate(
                sprint.getId(), 
                SprintEvent.EventType.HOLIDAY
        );
        long sprintEventHolidays = sprintHolidayEvents.size();
        
        // Get leave days during sprint
        List<LeaveDay> leaveDaysInSprint = leaveDayRepository.findByMemberIdAndDateRange(
                member.getId(),
                sprint.getStartDate(),
                sprint.getEndDate()
        );
        
        // Calculate available days: total days - holidays - sprint event holidays - personal leaves
        long availableDays = sprintDays - holidaysInSprint - sprintEventHolidays - leaveDaysInSprint.size();
        
        // Calculate total capacity
        BigDecimal totalCapacity = member.getDailyCapacity()
                .multiply(BigDecimal.valueOf(availableDays))
                .setScale(2, RoundingMode.HALF_UP);
        
        // Calculate allocated hours (from assigned tasks using story points)
        BigDecimal allocatedHours = member.getAssignedTasks().stream()
                .filter(task -> task.getSprint() != null && task.getSprint().getId().equals(sprint.getId()))
                .map(task -> task.getStoryPoints() != null ? task.getStoryPoints() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Calculate remaining capacity
        BigDecimal remainingCapacity = totalCapacity.subtract(allocatedHours);
        
        // Determine capacity status
        CapacitySummaryDto.CapacityStatus status;
        BigDecimal utilizationPercentage = totalCapacity.compareTo(BigDecimal.ZERO) > 0
                ? allocatedHours.divide(totalCapacity, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
        
        if (utilizationPercentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            status = CapacitySummaryDto.CapacityStatus.OVERLOADED;
        } else if (utilizationPercentage.compareTo(BigDecimal.valueOf(70)) < 0) {
            status = CapacitySummaryDto.CapacityStatus.UNDERUTILIZED;
        } else {
            status = CapacitySummaryDto.CapacityStatus.OK;
        }
        
        return new CapacitySummaryDto(
                member.getId(),
                member.getName(),
                totalCapacity,
                allocatedHours,
                remainingCapacity,
                status
        );
    }
    
    @Override
    @Transactional
    public void assignMembersToSprint(SprintAssignmentRequest request) {
        // Validate request
        request.validate();
        
        // Verify sprint exists
        if (!sprintRepository.existsById(request.sprintId())) {
            throw new RuntimeException("Sprint not found: " + request.sprintId());
        }
        
        // Verify all members exist
        for (String memberId : request.memberIds()) {
            if (!teamMemberRepository.existsById(memberId)) {
                throw new RuntimeException("Team member not found: " + memberId);
            }
        }
        
        // Get existing member IDs for this sprint to avoid duplicates
        List<String> existingMemberIds = sprintTeamRepository.findMemberIdsBySprintId(request.sprintId());
        
        // Only create assignments for members not already assigned
        int newAssignments = 0;
        for (String memberId : request.memberIds()) {
            if (!existingMemberIds.contains(memberId)) {
                SprintTeam mapping = new SprintTeam();
                mapping.setSprintId(request.sprintId());
                mapping.setMemberId(memberId);
                sprintTeamRepository.save(mapping);
                newAssignments++;
            }
        }
        
        // Remove members that are no longer in the request
        for (String existingMemberId : existingMemberIds) {
            if (!request.memberIds().contains(existingMemberId)) {
                SprintTeam mapping = sprintTeamRepository.findBySprintIdAndMemberId(
                    request.sprintId(), existingMemberId);
                if (mapping != null) {
                    sprintTeamRepository.delete(mapping);
                }
            }
        }
        
        log.info("Updated sprint {} assignments: {} new, {} total members", 
                request.sprintId(), newAssignments, request.memberIds().size());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<String> getMemberIdsForSprint(String sprintId) {
        List<String> assignedMemberIds = new ArrayList<>();
        if(StringUtils.isNotBlank(sprintId)) {
            assignedMemberIds = sprintTeamRepository.findMemberIdsBySprintId(sprintId);
        }
        return assignedMemberIds;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TeamMemberDto> getTeamMembersForSprint(String sprintId) {
        List<String> memberIds = getMemberIdsForSprint(sprintId);
        
        List<TeamMemberDto> members = new ArrayList<>();
        for (String memberId : memberIds) {
            try {
                TeamMemberDto member = getTeamMemberById(memberId);
                members.add(member);
            } catch (RuntimeException e) {
                // Skip members that no longer exist
                log.warn("Member {} assigned to sprint {} no longer exists", memberId, sprintId);
            }
        }
        
        return members;
    }
    
    /**
     * Check if a date is already a holiday (either in holidays table or in sprint events)
     * @param date The date to check
     * @param memberId The member ID (to find their sprint assignments)
     * @return true if the date is a holiday, false otherwise
     */
    private boolean isDateAHoliday(LocalDate date, String memberId) {
        // Check in global holidays table
        List<Holiday> holidays = holidayRepository.findByDateRange(date, date);
        if (!holidays.isEmpty()) {
            log.debug("Date {} is a holiday in holidays table", date);
            return true;
        }
        
        // Check in sprint events for all sprints this member is assigned to
        List<String> sprintIds = sprintTeamRepository.findSprintIdsByMemberId(memberId);
        for (String sprintId : sprintIds) {
            List<SprintEvent> sprintEvents = sprintEventRepository.findBySprintIdAndEventTypeOrderByEventDate(
                sprintId, SprintEvent.EventType.HOLIDAY);
            for (SprintEvent event : sprintEvents) {
                if (event.getEventDate().equals(date)) {
                    log.debug("Date {} is a holiday in sprint {} events", date, sprintId);
                    return true;
                }
            }
        }
        
        return false;
    }
}
