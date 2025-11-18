package com.sprintpilot.service.impl;

import com.sprintpilot.dto.*;
import com.sprintpilot.entity.Sprint;
import com.sprintpilot.entity.TeamMember;
import com.sprintpilot.repository.SprintRepository;
import com.sprintpilot.repository.TaskRepository;
import com.sprintpilot.repository.TeamMemberRepository;
import com.sprintpilot.service.SprintService;
import com.sprintpilot.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SprintServiceImpl implements SprintService {

    @Autowired
    private SprintRepository sprintRepository;
    
    @Autowired
    private com.sprintpilot.service.SprintEventService sprintEventService;
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private TeamMemberRepository teamMemberRepository;
    
    @Value("${app.data.mock-data-path}")
    private String mockDataPath;

    // Mock data removed - using real database queries
    
    // All mock data methods removed - now using actual database queries
    
    @Override
    @Transactional
    public SprintDto createSprint(SprintDto sprintDto) {
        // Calculate endDate and freezeDate if not provided
        LocalDate endDate = sprintDto.endDate();
        LocalDate freezeDate = sprintDto.freezeDate();
        
        if (endDate == null) {
            endDate = DateUtils.addWorkingDays(sprintDto.startDate(), sprintDto.duration(), List.of());
        }
        
        if (freezeDate == null) {
            freezeDate = DateUtils.addWorkingDays(sprintDto.startDate(), sprintDto.duration() - 2, List.of());
        }
        
        // In mock mode, just add to the list
        String newId = "sprint-" + System.currentTimeMillis();
        SprintDto newSprint = new SprintDto(
            newId,
            sprintDto.startDate(),
            endDate,
            sprintDto.duration(),
            freezeDate,
            Sprint.SprintStatus.PLANNING,
            sprintDto.events() != null ? sprintDto.events() : List.of(),
            sprintDto.teamMembers() != null ? sprintDto.teamMembers() : List.of(),
            sprintDto.tasks() != null ? sprintDto.tasks() : List.of()
        );
        Sprint sprint = Sprint.builder()
            .id(newId)
            .sprintName("SCRUM Sprint 1")
            .startDate(sprintDto.startDate())
            .endDate(endDate)
            .freezeDate(freezeDate)
            .duration(sprintDto.duration())
            .status(sprintDto.status())
            .build();
        sprintRepository.save(sprint);
        log.info("Created new sprint: {} from {} to {}", newId, newSprint.startDate(), newSprint.endDate());

        return newSprint;
    }
    
    @Override
    @Transactional
    public SprintDto updateSprint(String id, SprintDto sprintDto) {
        Sprint sprint = sprintRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Sprint not found: " + id));
        
        sprint.setStartDate(sprintDto.startDate());
        sprint.setEndDate(sprintDto.endDate());
        sprint.setDuration(sprintDto.duration());
        sprint.setFreezeDate(sprintDto.freezeDate());
        sprint.setStatus(sprintDto.status());
        
        sprintRepository.save(sprint);
        return convertToDto(sprint);
    }
    
    @Override
    @Transactional(readOnly = true)
    public SprintDto getSprintById(String id) {
        Sprint sprint = sprintRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Sprint not found: " + id));
        return convertToDto(sprint);
    }
    
    @Override
    @Transactional(readOnly = true)
    public SprintDto getSprintWithFullDetails(String id) {
        return getSprintById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<SprintDto> getAllSprints() {
        return sprintRepository.findAll().stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<SprintDto> getActiveSprints() {
        // Query actual database for active sprints using repository query
        List<Sprint> activeSprints = sprintRepository.findActiveSprints();
        
        // Convert to DTOs
        return activeSprints.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<SprintDto> getCompletedSprints() {
        // Query actual database for completed sprints using repository query
        List<Sprint> completedSprints = sprintRepository.findCompletedSprints();
        
        // Convert to DTOs
        return completedSprints.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
    
    private SprintDto convertToDto(Sprint sprint) {
        // Fetch related data for complete DTO
        List<SprintEventDto> events = sprintEventService.getEventsBySprintId(sprint.getId());
        
        // Fetch tasks from database
        List<TaskDto> tasks = taskRepository.findTaskDtosBySprintId(sprint.getId());
        
        // Fetch team members from database
        List<TeamMember> teamMembers = teamMemberRepository.findBySprintId(sprint.getId());
        List<TeamMemberDto> teamMemberDtos = teamMembers.stream()
            .map(member -> new TeamMemberDto(
                member.getId(),
                member.getName(),
                member.getRole(),
                member.getDailyCapacity(),
                member.getEmail(),
                member.getLocation(),
                member.getActive(),
                List.of(), // Leave days not needed for sprint DTO
                true // Assigned to current sprint
            ))
            .collect(Collectors.toList());
        
        return new SprintDto(
            sprint.getId(),
            sprint.getStartDate(),
            sprint.getEndDate(),
            sprint.getDuration(),
            sprint.getFreezeDate(),
            sprint.getStatus(),
            events != null ? events : List.of(),
            teamMemberDtos,
            tasks != null ? tasks : List.of()
        );
    }
    
    @Override
    @Transactional
    public SprintDto startSprint(String id) {
        SprintDto sprint = getSprintById(id);
        SprintDto updatedSprint = new SprintDto(
            sprint.id(),
            sprint.startDate(),
            sprint.endDate(),
            sprint.duration(),
            sprint.freezeDate(),
            Sprint.SprintStatus.ACTIVE,
            sprint.events(),
            sprint.teamMembers(),
            sprint.tasks()
        );
        return updateSprint(id, updatedSprint);
    }
    
    @Override
    @Transactional
    public SprintDto completeSprint(String id) {
        SprintDto sprint = getSprintById(id);
        SprintDto updatedSprint = new SprintDto(
            sprint.id(),
            sprint.startDate(),
            sprint.endDate(),
            sprint.duration(),
            sprint.freezeDate(),
            Sprint.SprintStatus.COMPLETED,
            sprint.events(),
            sprint.teamMembers(),
            sprint.tasks()
        );
        return updateSprint(id, updatedSprint);
    }
    
    @Override
    @Transactional
    public SprintDto archiveSprint(String id) {
        SprintDto sprint = getSprintById(id);
        SprintDto updatedSprint = new SprintDto(
            sprint.id(),
            sprint.startDate(),
            sprint.endDate(),
            sprint.duration(),
            sprint.freezeDate(),
            Sprint.SprintStatus.ARCHIVED,
            sprint.events(),
            sprint.teamMembers(),
            sprint.tasks()
        );
        return updateSprint(id, updatedSprint);
    }
    
    @Override
    @Transactional
    public void deleteSprint(String id) {
        sprintRepository.deleteById(id);
    }
    
    @Override
    @Transactional
    public SprintDto addEvent(String sprintId, SprintEventDto event) {
        SprintDto sprint = getSprintById(sprintId);
        List<SprintEventDto> updatedEvents = new ArrayList<>(sprint.events());
        updatedEvents.add(event);
        
        SprintDto updatedSprint = new SprintDto(
            sprint.id(),
            sprint.startDate(),
            sprint.endDate(),
            sprint.duration(),
            sprint.freezeDate(),
            sprint.status(),
            updatedEvents,
            sprint.teamMembers(),
            sprint.tasks()
        );
        return updateSprint(sprintId, updatedSprint);
    }
    
    @Override
    @Transactional
    public SprintDto removeEvent(String sprintId, String eventId) {
        SprintDto sprint = getSprintById(sprintId);
        List<SprintEventDto> updatedEvents = sprint.events().stream()
            .filter(e -> !e.id().equals(eventId))
            .collect(Collectors.toList());
        
        SprintDto updatedSprint = new SprintDto(
            sprint.id(),
            sprint.startDate(),
            sprint.endDate(),
            sprint.duration(),
            sprint.freezeDate(),
            sprint.status(),
            updatedEvents,
            sprint.teamMembers(),
            sprint.tasks()
        );
        return updateSprint(sprintId, updatedSprint);
    }
    
    @Override
    @Transactional
    public SprintDto addTeamMember(String sprintId, String memberId) {
        // Mock implementation
        return getSprintById(sprintId);
    }
    
    @Override
    @Transactional
    public SprintDto removeTeamMember(String sprintId, String memberId) {
        // Mock implementation
        return getSprintById(sprintId);
    }
    
    @Override
    public SprintDto calculateSprintDates(LocalDate startDate, Integer duration, List<String> holidays) {
        LocalDate endDate = DateUtils.addWorkingDays(startDate, duration, holidays);
        LocalDate freezeDate = DateUtils.addWorkingDays(startDate, duration - 2, holidays);
        
        return new SprintDto(
            null,
            startDate,
            endDate,
            duration,
            freezeDate,
            Sprint.SprintStatus.PLANNING,
            List.of(),
            List.of(),
            List.of()
        );
    }
    
    @Override
    @Transactional
    public SprintDto cloneSprint(String sourceSprintId) {
        SprintDto source = getSprintById(sourceSprintId);
        String newId = "sprint-clone-" + System.currentTimeMillis();
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(14);
        
        Sprint sprint = Sprint.builder()
            .id(newId)
            .sprintName("Cloned Sprint")
            .startDate(startDate)
            .endDate(endDate)
            .duration(source.duration())
            .freezeDate(null)
            .status(Sprint.SprintStatus.PLANNING)
            .build();
        
        sprintRepository.save(sprint);
        return convertToDto(sprint);
    }

    @Override
    public String getSprintName(String sprintId) {
        return sprintRepository.findById(sprintId)
            .map(Sprint::getSprintName)
            .orElseThrow(() -> new RuntimeException("Sprint not found: " + sprintId));
    }
    
    @Override
    public SprintDto getCurrentActiveSprint() {
        List<SprintDto> activeSprints = getActiveSprints();
        return activeSprints.isEmpty() ? null : activeSprints.get(0);
    }
    
    @Override
    public boolean hasActiveSprint() {
        return !getActiveSprints().isEmpty();
    }
    
    @Override
    public List<SprintDto> getSprintTemplates() {
        // Return completed sprints as templates, limited to last 10
        return getCompletedSprints().stream()
            .limit(10)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public SprintDto createSprintFromTemplate(String templateSprintId, LocalDate newStartDate) {
        SprintDto template = getSprintById(templateSprintId);
        String newId = "sprint-" + System.currentTimeMillis();
        
        // Calculate new dates based on template duration
        LocalDate newEndDate = DateUtils.addWorkingDays(newStartDate, template.duration(), List.of());
        LocalDate newFreezeDate = DateUtils.addWorkingDays(newStartDate, template.duration() - 2, List.of());
        
        Sprint sprint = Sprint.builder()
            .id(newId)
            .sprintName("Sprint from Template")
            .startDate(newStartDate)
            .endDate(newEndDate)
            .duration(template.duration())
            .freezeDate(newFreezeDate)
            .status(Sprint.SprintStatus.PLANNING)
            .build();
        
        sprintRepository.save(sprint);
        log.info("Created new sprint from template: {} -> {}", templateSprintId, newId);
        return convertToDto(sprint);
    }
}
