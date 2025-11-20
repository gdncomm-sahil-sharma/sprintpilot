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
    
    @Autowired
    private com.sprintpilot.repository.SprintTeamRepository sprintTeamRepository;

    @Value("${app.data.mock-data-path}")
    private String mockDataPath;

    // Mock data removed - using real database queries
    
    // All mock data methods removed - now using actual database queries
    
    @Override
    @Transactional
    public SprintDto createSprint(SprintDto sprintDto) {
        // Enforce one active sprint rule
        List<Sprint> activeSprints = sprintRepository.findAll().stream()
            .filter(s -> s.getStatus() == Sprint.SprintStatus.ACTIVE)
            .collect(Collectors.toList());

        if (!activeSprints.isEmpty()) {
            Sprint existingActive = activeSprints.get(0);
            throw new RuntimeException(
                "Cannot create sprint. An active sprint already exists: " + existingActive.getId() +
                ". Please complete/archive the current sprint first."
            );
        }

        // Auto-generate sprint name if not provided (format: "Sprint YYYY-MM-DD")
        String sprintName = sprintDto.sprintName();
        if (sprintName == null || sprintName.isBlank()) {
            sprintName = "Sprint " + sprintDto.startDate().toString();
            log.info("Auto-generated sprint name: {}", sprintName);
        }

        // Check for uniqueness
        if (sprintRepository.existsBySprintName(sprintName)) {
            throw new RuntimeException("Sprint name already exists: " + sprintName + ". Please choose a unique name.");
        }

        // Calculate endDate and freezeDate if not provided
        LocalDate endDate = sprintDto.endDate();
        LocalDate freezeDate = sprintDto.freezeDate();
        
        if (endDate == null) {
            endDate = DateUtils.addWorkingDays(sprintDto.startDate(), sprintDto.duration(), List.of());
        }
        
        if (freezeDate == null) {
            freezeDate = DateUtils.addWorkingDays(sprintDto.startDate(), sprintDto.duration() - 2, List.of());
        }
        
        // Generate unique sprint ID
        String newId = "sprint-" + System.currentTimeMillis();

        Sprint sprint = Sprint.builder()
            .id(newId)
            .sprintName(sprintName)
            .startDate(sprintDto.startDate())
            .endDate(endDate)
            .freezeDate(freezeDate)
            .duration(sprintDto.duration())
            .status(Sprint.SprintStatus.ACTIVE)
            .build();
        Sprint savedSprint = sprintRepository.save(sprint);
        log.info("Created new sprint: {} '{}' from {} to {}", newId, sprintName, savedSprint.getStartDate(), savedSprint.getEndDate());

        // Auto-assign all active and non-deleted members to this sprint
        autoAssignActiveMembers(newId);

        SprintDto newSprint = convertToDto(savedSprint);

        return newSprint;
    }
    
    @Override
    @Transactional
    public SprintDto updateSprint(String id, SprintDto sprintDto) {
        Sprint sprint = sprintRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Sprint not found: " + id));
        
        // Validate sprint name uniqueness if provided and changed
        if (sprintDto.sprintName() != null && !sprintDto.sprintName().isBlank()) {
            if (!sprintDto.sprintName().equals(sprint.getSprintName())) {
                if (sprintRepository.existsBySprintNameAndIdNot(sprintDto.sprintName(), id)) {
                    throw new RuntimeException("Sprint name already exists: " + sprintDto.sprintName() + ". Please choose a unique name.");
                }
                sprint.setSprintName(sprintDto.sprintName());
            }
        }

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
        // Query actual database for active sprints (only ACTIVE status)
        List<Sprint> activeSprints = sprintRepository.findAll().stream()
            .filter(s -> s.getStatus() == Sprint.SprintStatus.ACTIVE)
            .collect(Collectors.toList());
        
        // Convert to DTOs
        return activeSprints.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<SprintDto> getArchivedSprints() {
        // Query actual database for archived sprints
        List<Sprint> archivedSprints = sprintRepository.findAll().stream()
            .filter(s -> s.getStatus() == Sprint.SprintStatus.ARCHIVED)
            .collect(Collectors.toList());
        
        // Convert to DTOs
        return archivedSprints.stream()
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
            sprint.getSprintName(),
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
    public SprintDto archiveSprint(String id) {
        SprintDto sprint = getSprintById(id);
        SprintDto updatedSprint = new SprintDto(
            sprint.id(),
            sprint.sprintName(),
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
    public SprintDto reactivateSprint(String id) {
        // Find the sprint to reactivate
        Sprint sprint = sprintRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Sprint not found: " + id));

        if (sprint.getStatus() != Sprint.SprintStatus.ARCHIVED) {
            throw new RuntimeException("Only archived sprints can be reactivated");
        }

        // Check if there's already an active sprint - prevent reactivation
        List<Sprint> activeSprints = sprintRepository.findAll().stream()
            .filter(s -> s.getStatus() == Sprint.SprintStatus.ACTIVE)
            .collect(Collectors.toList());

        if (!activeSprints.isEmpty()) {
            Sprint existingActive = activeSprints.get(0);
            throw new RuntimeException(
                "Cannot reactivate sprint. An active sprint already exists: " + existingActive.getSprintName() +
                ". Please complete the current active sprint first before reactivating an archived one."
            );
        }

        // Reactivate the requested sprint
        sprint.setStatus(Sprint.SprintStatus.ACTIVE);
        Sprint reactivated = sprintRepository.save(sprint);

        log.info("Sprint reactivated successfully: {}", id);
        return convertToDto(reactivated);
    }

    @Override
    public boolean canCompleteSprint(String id) {
        Sprint sprint = sprintRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Sprint not found: " + id));

        // Sprint can be completed anytime if it's ACTIVE
        return sprint.getStatus() == Sprint.SprintStatus.ACTIVE;
    }

    @Override
    @Transactional
    public SprintDto completeAndArchiveSprint(String id) {
        // 1. Validate sprint exists and is ACTIVE
        Sprint sprint = sprintRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Sprint not found: " + id));

        if (sprint.getStatus() != Sprint.SprintStatus.ACTIVE) {
            throw new RuntimeException("Only active sprints can be completed");
        }

        // 2. Verify there's only one active sprint (sanity check)
        List<Sprint> activeSprints = sprintRepository.findAll().stream()
            .filter(s -> s.getStatus() == Sprint.SprintStatus.ACTIVE)
            .collect(Collectors.toList());

        if (activeSprints.size() > 1) {
            log.warn("Multiple active sprints detected! This should not happen.");
        }

        // 3. Archive the current sprint
        sprint.setStatus(Sprint.SprintStatus.ARCHIVED);
        Sprint archivedSprint = sprintRepository.save(sprint);
        log.info("Sprint archived successfully: {}", id);

        // ✅ Removed auto-creation of next sprint - users must manually create new sprints

        return convertToDto(archivedSprint);
    }

    /**
     * Helper method to create the next sprint automatically
     * Calculates start date as the next Monday after the previous sprint's end date
     */
    // Removed auto-create next sprint functionality - users must manually create new sprints

    @Override
    @Transactional
    public void deleteSprint(String id) {
        sprintRepository.deleteById(id);
    }
    
    @Override
    @Transactional
    public void deleteLatestArchivedSprint(String id) {
        // 1. Validate sprint exists and is ARCHIVED
        Sprint sprint = sprintRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Sprint not found: " + id));

        if (sprint.getStatus() != Sprint.SprintStatus.ARCHIVED) {
            throw new RuntimeException("Only archived sprints can be deleted");
        }

        // 2. Get all archived sprints and find the latest one by endDate
        List<Sprint> archivedSprints = sprintRepository.findAll().stream()
            .filter(s -> s.getStatus() == Sprint.SprintStatus.ARCHIVED)
            .collect(Collectors.toList());

        if (archivedSprints.isEmpty()) {
            throw new RuntimeException("No archived sprints found");
        }

        // Find the latest archived sprint by endDate
        Sprint latestArchivedSprint = archivedSprints.stream()
            .max(Comparator.comparing(Sprint::getEndDate))
            .orElseThrow(() -> new RuntimeException("Could not determine latest archived sprint"));

        // 3. Verify the sprint being deleted is the latest one
        if (!sprint.getId().equals(latestArchivedSprint.getId())) {
            throw new RuntimeException(
                "Only the latest archived sprint can be deleted. Latest sprint: " +
                latestArchivedSprint.getSprintName()
            );
        }

        // 4. Delete the sprint (this will cascade delete events, team assignments, tasks)
        sprintRepository.deleteById(id);
        log.info("Successfully deleted latest archived sprint: {} ({})", sprint.getSprintName(), id);
    }

    @Override
    @Transactional
    public SprintDto addEvent(String sprintId, SprintEventDto event) {
        SprintDto sprint = getSprintById(sprintId);
        List<SprintEventDto> updatedEvents = new ArrayList<>(sprint.events());
        updatedEvents.add(event);
        
        SprintDto updatedSprint = new SprintDto(
            sprint.id(),
            sprint.sprintName(),
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
            sprint.sprintName(),
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
        // TODO: Implement actual team member assignment logic
        return getSprintById(sprintId);
    }
    
    @Override
    @Transactional
    public SprintDto removeTeamMember(String sprintId, String memberId) {
        // TODO: Implement actual team member removal logic
        return getSprintById(sprintId);
    }
    
    @Override
    public SprintDto calculateSprintDates(LocalDate startDate, Integer duration, List<String> holidays) {
        LocalDate endDate = DateUtils.addWorkingDays(startDate, duration, holidays);
        LocalDate freezeDate = DateUtils.addWorkingDays(startDate, duration - 2, holidays);
        
        return new SprintDto(
            null,
            null, // Sprint name will be auto-generated during creation
            startDate,
            endDate,
            duration,
            freezeDate,
            Sprint.SprintStatus.ACTIVE,
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
            .status(Sprint.SprintStatus.ACTIVE)
            .build();
        
        sprintRepository.save(sprint);
        
        // Auto-assign all active and non-deleted members to this sprint
        autoAssignActiveMembers(newId);
        
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
        // Return archived sprints as templates, limited to last 10
        return getArchivedSprints().stream()
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
            .status(Sprint.SprintStatus.ACTIVE)
            .build();
        
        sprintRepository.save(sprint);
        
        // Auto-assign all active and non-deleted members to this sprint
        autoAssignActiveMembers(newId);
        
        log.info("Created new sprint from template: {} -> {}", templateSprintId, newId);
        return convertToDto(sprint);
    }
    
    /**
     * Auto-assign all active and non-deleted team members to a sprint
     * This is called whenever a new sprint is created
     * @param sprintId The sprint ID to assign members to
     */
    private void autoAssignActiveMembers(String sprintId) {
        // Get all active and non-deleted team members
        List<TeamMember> activeMembers = teamMemberRepository.findActiveMembers();
        
        int assignedCount = 0;
        for (TeamMember member : activeMembers) {
            // Check if member is not deleted (extra safety check)
            if (member.getDeleted() != null && member.getDeleted()) {
                continue;
            }
            
            // Create sprint-member mapping
            com.sprintpilot.entity.SprintTeam mapping = new com.sprintpilot.entity.SprintTeam();
            mapping.setSprintId(sprintId);
            mapping.setMemberId(member.getId());
            sprintTeamRepository.save(mapping);
            assignedCount++;
        }
        
        log.info("Auto-assigned {} active members to sprint {}", assignedCount, sprintId);
    }
}
