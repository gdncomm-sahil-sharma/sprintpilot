package com.sprintpilot.service.impl;

import com.sprintpilot.dto.*;
import com.sprintpilot.entity.Sprint;
import com.sprintpilot.entity.SprintEvent;
import com.sprintpilot.entity.TeamMember;
import com.sprintpilot.repository.SprintRepository;
import com.sprintpilot.service.SprintService;
import com.sprintpilot.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SprintServiceImpl implements SprintService {
    
    @Autowired
    private SprintRepository sprintRepository;
    
    @Value("${app.data.mock-data-path}")
    private String mockDataPath;
    
    private List<SprintDto> mockSprints = new ArrayList<>();
    
    @PostConstruct
    public void initializeMockData() {
        try {
            // Load mock sprint data
            createMockSprints();
        } catch (Exception e) {
            // Create default mock data if file loading fails
            createDefaultMockData();
        }
    }
    
    private void createMockSprints() {
        // Create current active sprint
        SprintDto activeSprint = new SprintDto(
            "sprint-current",
            LocalDate.now(),
            LocalDate.now().plusDays(14),
            10,
            LocalDate.now().plusDays(12),
            Sprint.SprintStatus.ACTIVE,
            createMockEvents("sprint-current"),
            createMockTeamMembers(),
            createMockTasks("sprint-current")
        );
        mockSprints.add(activeSprint);
        
        // Create historical sprints
        for (int i = 1; i <= 5; i++) {
            LocalDate startDate = LocalDate.now().minusMonths(i * 2).minusDays(14);
            LocalDate endDate = startDate.plusDays(14);
            
            SprintDto historicalSprint = new SprintDto(
                "sprint-hist-" + i,
                startDate,
                endDate,
                10,
                endDate.minusDays(2),
                Sprint.SprintStatus.COMPLETED,
                createMockEvents("sprint-hist-" + i),
                createMockTeamMembers(),
                createMockTasks("sprint-hist-" + i)
            );
            mockSprints.add(historicalSprint);
        }
    }
    
    private List<SprintEventDto> createMockEvents(String sprintId) {
        List<SprintEventDto> events = new ArrayList<>();
        
        // Add deployment event
        events.add(new SprintEventDto(
            "event-deploy-" + sprintId,
            sprintId,
            SprintEvent.EventType.DEPLOYMENT,
            null,
            "Production Deployment",
            LocalDate.now().plusDays(12),
            null,
            null,
            "Deploy to production environment"
        ));
        
        // Add meeting events
        events.add(new SprintEventDto(
            "event-planning-" + sprintId,
            sprintId,
            SprintEvent.EventType.MEETING,
            SprintEvent.MeetingType.PLANNING,
            "Sprint Planning",
            LocalDate.now(),
            java.time.LocalTime.of(10, 0),
            120,
            "Sprint planning session"
        ));
        
        events.add(new SprintEventDto(
            "event-retro-" + sprintId,
            sprintId,
            SprintEvent.EventType.MEETING,
            SprintEvent.MeetingType.RETROSPECTIVE,
            "Sprint Retrospective",
            LocalDate.now().plusDays(14),
            java.time.LocalTime.of(15, 0),
            90,
            "Sprint retrospective meeting"
        ));
        
        return events;
    }
    
    private List<TeamMemberDto> createMockTeamMembers() {
        List<TeamMemberDto> members = new ArrayList<>();
        
        members.add(new TeamMemberDto(
            "member-1",
            "Alice Johnson",
            TeamMember.Role.FRONTEND,
            new BigDecimal("6"),
            "alice@company.com",
            "Bangalore",
            true,
            List.of(), false
        ));
        
        members.add(new TeamMemberDto(
            "member-2",
            "Bob Smith",
            TeamMember.Role.BACKEND,
            new BigDecimal("7"),
            "bob@company.com",
            "Coimbatore",
            true,
            List.of(), false
        ));
        
        members.add(new TeamMemberDto(
            "member-3",
            "Charlie Davis",
            TeamMember.Role.QA,
            new BigDecimal("5"),
            "charlie@company.com",
            "Indonesia",
            true,
            List.of(), false
        ));
        
        members.add(new TeamMemberDto(
            "member-4",
            "David Wilson",
            TeamMember.Role.BACKEND,
            new BigDecimal("7"),
            "david@company.com",
            "Bangalore",
            true,
            List.of(), false
        ));
        
        return members;
    }
    
    private List<TaskDto> createMockTasks(String sprintId) {
        List<TaskDto> tasks = new ArrayList<>();
        
        tasks.add(new TaskDto(
            "task-1-" + sprintId,
            sprintId,
            "PROJ-101",
            "Implement user dashboard",
            "Create new user dashboard with analytics",
            new BigDecimal("40"),
            com.sprintpilot.entity.Task.TaskCategory.FEATURE,
            com.sprintpilot.entity.Task.TaskPriority.HIGH,
            com.sprintpilot.entity.Task.TaskStatus.IN_PROGRESS,
            LocalDate.now(),
            LocalDate.now().plusDays(5),
            new BigDecimal("16"),
            "member-1"
        ));
        
        tasks.add(new TaskDto(
            "task-2-" + sprintId,
            sprintId,
            "PROJ-102",
            "API optimization",
            "Optimize database queries for better performance",
            new BigDecimal("32"),
            com.sprintpilot.entity.Task.TaskCategory.TECH_DEBT,
            com.sprintpilot.entity.Task.TaskPriority.MEDIUM,
            com.sprintpilot.entity.Task.TaskStatus.TODO,
            LocalDate.now().plusDays(3),
            LocalDate.now().plusDays(8),
            BigDecimal.ZERO,
            "member-2"
        ));
        
        tasks.add(new TaskDto(
            "task-3-" + sprintId,
            sprintId,
            "PROJ-103",
            "Fix login bug",
            "Resolve authentication timeout issue",
            new BigDecimal("16"),
            com.sprintpilot.entity.Task.TaskCategory.PROD_ISSUE,
            com.sprintpilot.entity.Task.TaskPriority.CRITICAL,
            com.sprintpilot.entity.Task.TaskStatus.IN_PROGRESS,
            LocalDate.now(),
            LocalDate.now().plusDays(2),
            new BigDecimal("8"),
            "member-4"
        ));
        
        tasks.add(new TaskDto(
            "task-4-" + sprintId,
            sprintId,
            "PROJ-104",
            "E2E testing suite",
            "Develop comprehensive E2E tests",
            new BigDecimal("24"),
            com.sprintpilot.entity.Task.TaskCategory.FEATURE,
            com.sprintpilot.entity.Task.TaskPriority.MEDIUM,
            com.sprintpilot.entity.Task.TaskStatus.TODO,
            LocalDate.now().plusDays(5),
            LocalDate.now().plusDays(10),
            BigDecimal.ZERO,
            "member-3"
        ));
        
        return tasks;
    }
    
    private void createDefaultMockData() {
        createMockSprints();
    }
    
    @Override
    @Transactional
    public SprintDto createSprint(SprintDto sprintDto) {
        // In mock mode, just add to the list
        String newId = "sprint-" + System.currentTimeMillis();
        SprintDto newSprint = new SprintDto(
            newId,
            sprintDto.startDate(),
            sprintDto.endDate(),
            sprintDto.duration(),
            sprintDto.freezeDate(),
            Sprint.SprintStatus.PLANNING,
            sprintDto.events() != null ? sprintDto.events() : List.of(),
            sprintDto.teamMembers() != null ? sprintDto.teamMembers() : List.of(),
            sprintDto.tasks() != null ? sprintDto.tasks() : List.of()
        );
        Sprint sprint = Sprint.builder()
            .id(newId)
            .sprintName("SCRUM Sprint 1")
            .startDate(sprintDto.startDate())
            .endDate(sprintDto.endDate())
            .freezeDate(sprintDto.freezeDate())
            .duration(sprintDto.duration())
            .status(sprintDto.status())
            .build();
        sprintRepository.save(sprint);
        return newSprint;
    }
    
    @Override
    @Transactional
    public SprintDto updateSprint(String id, SprintDto sprintDto) {
        mockSprints.removeIf(s -> s.id().equals(id));
        mockSprints.add(sprintDto);
        return sprintDto;
    }
    
    @Override
    public SprintDto getSprintById(String id) {
        return mockSprints.stream()
            .filter(s -> s.id().equals(id))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Sprint not found: " + id));
    }
    
    @Override
    public SprintDto getSprintWithFullDetails(String id) {
        return getSprintById(id);
    }
    
    @Override
    public List<SprintDto> getAllSprints() {
        return new ArrayList<>(mockSprints);
    }
    
    @Override
    public List<SprintDto> getActiveSprints() {
        return mockSprints.stream()
            .filter(s -> s.status() == Sprint.SprintStatus.ACTIVE || 
                        s.status() == Sprint.SprintStatus.PLANNING)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<SprintDto> getCompletedSprints() {
        return mockSprints.stream()
            .filter(s -> s.status() == Sprint.SprintStatus.COMPLETED)
            .collect(Collectors.toList());
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
        mockSprints.removeIf(s -> s.id().equals(id));
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
        
        SprintDto cloned = new SprintDto(
            newId,
            LocalDate.now(),
            LocalDate.now().plusDays(14),
            source.duration(),
            null,
            Sprint.SprintStatus.PLANNING,
            List.of(),
            source.teamMembers(),
            List.of()
        );
        
        mockSprints.add(cloned);
        return cloned;
    }

    @Override
    public String getSprintName(String sprintId) {
        return sprintRepository.findById(sprintId)
            .map(Sprint::getSprintName)
            .orElseThrow(() -> new RuntimeException("Sprint not found: " + sprintId));
    }
}
