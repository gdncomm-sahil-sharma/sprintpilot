package com.sprintpilot.controller;

import com.sprintpilot.dto.ApiResponse;
import com.sprintpilot.dto.SprintDto;
import com.sprintpilot.dto.SprintEventDto;
import com.sprintpilot.dto.HolidayDto;
import com.sprintpilot.entity.SprintEvent;
import com.sprintpilot.service.SprintService;
import com.sprintpilot.service.SprintEventService;
import com.sprintpilot.service.HolidayService;
import com.sprintpilot.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Sprint Setup functionality
 * Handles all CRUD operations for sprint configuration, meetings, deployments, and timeline
 */
@RestController
@RequestMapping("/api/sprint-setup")
@Slf4j
public class SprintSetupController {

    @Autowired
    private SprintService sprintService;
    
    @Autowired
    private SprintEventService sprintEventService;
    
    @Autowired
    private HolidayService holidayService;
    
    @Autowired
    private com.sprintpilot.service.AIService aiService;

    /**
     * Get global/pre-configured holidays for a given date range
     * These can be selected and added to the sprint
     */
    @GetMapping("/holidays")
    public ResponseEntity<ApiResponse<List<HolidayDto>>> getGlobalHolidays(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            
            List<HolidayDto> holidays = holidayService.getHolidaysByDateRange(start, end);
            log.info("Retrieved {} global holidays for date range {} to {}", holidays.size(), startDate, endDate);
            
            return ResponseEntity.ok(ApiResponse.success("Global holidays retrieved successfully", holidays));
        } catch (Exception e) {
            log.error("Error retrieving global holidays", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("Failed to retrieve global holidays: " + e.getMessage()));
        }
    }

    /**
     * Save or update complete sprint configuration (dates, duration, freeze date, holidays, deployments, meetings)
     * This is the single endpoint for saving all sprint setup data
     */
    @PostMapping("/{sprintId}/configuration")
    public ResponseEntity<ApiResponse<SprintDto>> saveSprintConfiguration(
            @PathVariable String sprintId,
            @RequestBody Map<String, Object> config) {
        try {
            // Extract configuration values
            String sprintName = (String) config.get("sprintName");
            String startDateStr = (String) config.get("startDate");
            Integer duration = (Integer) config.get("duration");
            @SuppressWarnings("unchecked")
            List<Map<String, String>> holidays = (List<Map<String, String>>) config.get("holidays");
            @SuppressWarnings("unchecked")
            List<Map<String, String>> deployments = (List<Map<String, String>>) config.get("deployments");
            @SuppressWarnings("unchecked")
            List<Map<String, String>> codeFreezes = (List<Map<String, String>>) config.get("codeFreezes");
            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> meetings = (Map<String, Map<String, Object>>) config.get("meetings");
            
            if (!StringUtils.hasText(startDateStr) || duration == null || duration <= 0) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Start date and valid duration are required"));
            }
            
            LocalDate startDate = LocalDate.parse(startDateStr);
            
            // Get existing holidays for working day calculation
            List<String> existingHolidays = holidayService.getHolidayDatesForSprint(
                startDate, startDate.plusDays(duration + 10));
            
            // Calculate end date using working days
            LocalDate endDate = DateUtils.addWorkingDays(startDate, duration, existingHolidays);
            
            // Update sprint basic info (including sprint name if provided)
            SprintDto currentSprint = sprintService.getSprintById(sprintId);
            String updatedSprintName = StringUtils.hasText(sprintName) ? sprintName : currentSprint.sprintName();
            
            SprintDto updatedSprint = new SprintDto(
                currentSprint.id(),
                updatedSprintName,
                startDate,
                endDate,
                duration,
                null, // freezeDate is deprecated - now using code freeze events
                currentSprint.status(),
                currentSprint.events(),
                currentSprint.teamMembers(),
                currentSprint.tasks()
            );
            
            sprintService.updateSprint(sprintId, updatedSprint);
            
            // STEP 1: Delete existing holiday, deployment, and code freeze events to avoid duplicates
            sprintEventService.deleteEventsBySprintAndType(sprintId, SprintEvent.EventType.HOLIDAY);
            sprintEventService.deleteEventsBySprintAndType(sprintId, SprintEvent.EventType.DEPLOYMENT);
            sprintEventService.deleteEventsBySprintAndType(sprintId, SprintEvent.EventType.CODE_FREEZE);
            log.info("Cleared existing holidays, deployments, and code freezes for sprint: {}", sprintId);
            
            // STEP 2: Process holidays - create new ones
            if (holidays != null && !holidays.isEmpty()) {
                for (Map<String, String> holiday : holidays) {
                    String date = holiday.get("date");
                    String name = holiday.get("name");
                    
                    if (StringUtils.hasText(date) && StringUtils.hasText(name)) {
                        SprintEventDto holidayEvent = new SprintEventDto(
                            null,
                            sprintId,
                            SprintEvent.EventType.HOLIDAY,
                            null,
                            name,
                            LocalDate.parse(date),
                            null,
                            null,
                            "Sprint holiday: " + name
                        );
                        sprintEventService.createHolidayEvent(sprintId, holidayEvent);
                    }
                }
                log.info("Created {} holidays for sprint: {}", holidays.size(), sprintId);
            }
            
            // STEP 3: Process deployments - create new ones
            if (deployments != null && !deployments.isEmpty()) {
                for (Map<String, String> deployment : deployments) {
                    String date = deployment.get("date");
                    String name = deployment.get("name");
                    
                    if (StringUtils.hasText(date) && StringUtils.hasText(name)) {
                        SprintEventDto deploymentEvent = new SprintEventDto(
                            null,
                            sprintId,
                            SprintEvent.EventType.DEPLOYMENT,
                            null,
                            name,
                            LocalDate.parse(date),
                            null,
                            null,
                            "Deployment: " + name
                        );
                        sprintEventService.createDeploymentEvent(sprintId, deploymentEvent);
                    }
                }
                log.info("Created {} deployments for sprint: {}", deployments.size(), sprintId);
            }
            
            // STEP 4: Process code freeze dates - create new ones
            if (codeFreezes != null && !codeFreezes.isEmpty()) {
                for (Map<String, String> codeFreeze : codeFreezes) {
                    String date = codeFreeze.get("date");
                    String name = codeFreeze.get("name");
                    
                    if (StringUtils.hasText(date)) {
                        SprintEventDto codeFreezeEvent = new SprintEventDto(
                            null,
                            sprintId,
                            SprintEvent.EventType.CODE_FREEZE,
                            null,
                            StringUtils.hasText(name) ? name : "Code Freeze",
                            LocalDate.parse(date),
                            null,
                            null,
                            "Code Freeze: " + (StringUtils.hasText(name) ? name : "")
                        );
                        sprintEventService.createEvent(codeFreezeEvent);
                    }
                }
                log.info("Created {} code freeze dates for sprint: {}", codeFreezes.size(), sprintId);
            }
            
            // STEP 5: Process meetings (Planning, Grooming, Retrospective)
            if (meetings != null && !meetings.isEmpty()) {
                for (Map.Entry<String, Map<String, Object>> entry : meetings.entrySet()) {
                    String meetingType = entry.getKey();
                    Map<String, Object> meetingData = entry.getValue();
                    
                    try {
                        SprintEvent.MeetingType type = SprintEvent.MeetingType.valueOf(meetingType.toUpperCase());
                        
                        String dateStr = (String) meetingData.get("date");
                        String timeStr = (String) meetingData.get("time");
                        Object durationObj = meetingData.get("duration");
                        
                        // Skip if no date/time (meeting not configured yet)
                        if (!StringUtils.hasText(dateStr) || !StringUtils.hasText(timeStr)) {
                            log.info("Skipping {} meeting - no date/time provided", meetingType);
                            continue;
                        }
                        
                        LocalDate meetingDate = LocalDate.parse(dateStr);
                        LocalTime meetingTime = LocalTime.parse(timeStr);
                        Integer meetingDuration = durationObj instanceof Integer ? (Integer) durationObj : 
                                                  durationObj != null ? Integer.parseInt(durationObj.toString()) : 60;
                        
                        // Create or update meeting event
                        SprintEventDto meetingEvent = new SprintEventDto(
                            null,
                            sprintId,
                            SprintEvent.EventType.MEETING,
                            type,
                            getMeetingName(type),
                            meetingDate,
                            meetingTime,
                            meetingDuration,
                            getMeetingDescription(type)
                        );
                        
                        sprintEventService.createOrUpdateMeeting(sprintId, type, meetingEvent);
                        log.info("Saved {} meeting for sprint: {}", type, sprintId);
                        
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid meeting type: {}, skipping", meetingType);
                    }
                }
            }
            
            // STEP 6: Fetch the complete sprint with all events
            SprintDto completeSprintWithEvents = sprintService.getSprintById(sprintId);
            log.info("Updated sprint configuration for sprint: {} with {} events", 
                sprintId, completeSprintWithEvents.events() != null ? completeSprintWithEvents.events().size() : 0);
            
            return ResponseEntity.ok(ApiResponse.success("Sprint configuration saved successfully", completeSprintWithEvents));
            
        } catch (Exception e) {
            log.error("Error saving sprint configuration for sprint: {}", sprintId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("Failed to save sprint configuration: " + e.getMessage()));
        }
    }

    /**
     * Calculate sprint dates based on start date and duration
     */
    @PostMapping("/calculate-dates")
    public ResponseEntity<ApiResponse<Map<String, Object>>> calculateSprintDates(@RequestBody Map<String, Object> request) {
        try {
            String startDateStr = (String) request.get("startDate");
            Integer duration = (Integer) request.get("duration");
            
            if (!StringUtils.hasText(startDateStr) || duration == null || duration <= 0) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Start date and valid duration are required"));
            }
            
            LocalDate startDate = LocalDate.parse(startDateStr);
            
            // Get holidays for working day calculation
            List<String> holidays = holidayService.getHolidayDatesForSprint(
                startDate, startDate.plusDays(duration + 10));
            
            // Calculate dates
            LocalDate endDate = DateUtils.addWorkingDays(startDate, duration, holidays);
            LocalDate freezeDate = DateUtils.calculateCodeFreezeDate(endDate, 2, holidays);
            
            Map<String, Object> result = new HashMap<>();
            result.put("startDate", startDate.toString());
            result.put("endDate", endDate.toString());
            result.put("freezeDate", freezeDate.toString());
            result.put("duration", duration);
            result.put("workingDays", DateUtils.getWorkingDays(startDate, endDate, holidays));
            result.put("holidaysInSplit", holidays.size());
            
            return ResponseEntity.ok(ApiResponse.success("Dates calculated", result));
            
        } catch (Exception e) {
            log.error("Error calculating sprint dates", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("Failed to calculate dates: " + e.getMessage()));
        }
    }

    /**
     * Save or update meeting event (Planning, Grooming, Retrospective)
     */
    @PostMapping("/{sprintId}/meetings/{meetingType}")
    public ResponseEntity<ApiResponse<SprintEventDto>> saveMeeting(
            @PathVariable String sprintId,
            @PathVariable String meetingType,
            @RequestBody Map<String, Object> meetingData) {
        try {
            // Parse meeting type
            SprintEvent.MeetingType type = SprintEvent.MeetingType.valueOf(meetingType.toUpperCase());
            
            // Extract meeting data
            String dateStr = (String) meetingData.get("date");
            String timeStr = (String) meetingData.get("time");
            Integer duration = (Integer) meetingData.get("duration");
            
            if (!StringUtils.hasText(dateStr) || !StringUtils.hasText(timeStr)) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Date and time are required"));
            }
            
            LocalDate meetingDate = LocalDate.parse(dateStr);
            LocalTime meetingTime = LocalTime.parse(timeStr);
            
            // Create meeting event DTO
            SprintEventDto eventDto = new SprintEventDto(
                null, // ID will be generated
                sprintId,
                SprintEvent.EventType.MEETING,
                type,
                getMeetingName(type),
                meetingDate,
                meetingTime,
                duration,
                getMeetingDescription(type)
            );
            
            SprintEventDto savedEvent = sprintEventService.createOrUpdateMeeting(sprintId, type, eventDto);
            log.info("Saved {} meeting for sprint: {}", type, sprintId);
            
            return ResponseEntity.ok(ApiResponse.success("Meeting saved", savedEvent));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.failure("Invalid meeting type: " + meetingType));
        } catch (Exception e) {
            log.error("Error saving meeting for sprint: {}", sprintId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("Failed to save meeting: " + e.getMessage()));
        }
    }

    /**
     * Get meeting by type for a sprint
     */
    @GetMapping("/{sprintId}/meetings/{meetingType}")
    public ResponseEntity<ApiResponse<SprintEventDto>> getMeeting(
            @PathVariable String sprintId,
            @PathVariable String meetingType) {
        try {
            SprintEvent.MeetingType type = SprintEvent.MeetingType.valueOf(meetingType.toUpperCase());
            SprintEventDto meeting = sprintEventService.getMeetingBySprintAndType(sprintId, type);
            
            return ResponseEntity.ok(ApiResponse.success("Meeting retrieved", meeting));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.failure("Invalid meeting type: " + meetingType));
        } catch (Exception e) {
            log.error("Error getting meeting for sprint: {}", sprintId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("Failed to get meeting: " + e.getMessage()));
        }
    }

    /**
     * Create deployment event
     */
    @PostMapping("/{sprintId}/deployments")
    public ResponseEntity<ApiResponse<SprintEventDto>> createDeployment(
            @PathVariable String sprintId,
            @RequestBody Map<String, Object> deploymentData) {
        try {
            String name = (String) deploymentData.get("name");
            String dateStr = (String) deploymentData.get("date");
            String timeStr = (String) deploymentData.get("time");
            String description = (String) deploymentData.get("description");
            
            if (!StringUtils.hasText(name) || !StringUtils.hasText(dateStr)) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Name and date are required"));
            }
            
            LocalDate deploymentDate = LocalDate.parse(dateStr);
            LocalTime deploymentTime = StringUtils.hasText(timeStr) ? LocalTime.parse(timeStr) : null;
            
            SprintEventDto eventDto = new SprintEventDto(
                null,
                sprintId,
                SprintEvent.EventType.DEPLOYMENT,
                null,
                name,
                deploymentDate,
                deploymentTime,
                null,
                description
            );
            
            SprintEventDto savedEvent = sprintEventService.createDeploymentEvent(sprintId, eventDto);
            log.info("Created deployment event for sprint: {}", sprintId);
            
            return ResponseEntity.ok(ApiResponse.success("Deployment created", savedEvent));
            
        } catch (Exception e) {
            log.error("Error creating deployment for sprint: {}", sprintId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("Failed to create deployment: " + e.getMessage()));
        }
    }

    /**
     * Get all events for timeline visualization
     */
    @GetMapping("/{sprintId}/timeline")
    public ResponseEntity<ApiResponse<List<SprintEventDto>>> getTimelineEvents(@PathVariable String sprintId) {
        try {
            List<SprintEventDto> events = sprintEventService.getEventsBySprintId(sprintId);
            return ResponseEntity.ok(ApiResponse.success("Timeline events retrieved", events));
            
        } catch (Exception e) {
            log.error("Error getting timeline events for sprint: {}", sprintId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("Failed to get timeline events: " + e.getMessage()));
        }
    }

    /**
     * Get holidays for sprint date range
     */
    @GetMapping("/{sprintId}/holidays")
    public ResponseEntity<ApiResponse<List<HolidayDto>>> getSprintHolidays(@PathVariable String sprintId) {
        try {
            SprintDto sprint = sprintService.getSprintById(sprintId);
            List<HolidayDto> holidays = holidayService.getHolidaysByDateRange(
                sprint.startDate(), sprint.endDate());
            
            return ResponseEntity.ok(ApiResponse.success("Sprint holidays retrieved", holidays));
            
        } catch (Exception e) {
            log.error("Error getting holidays for sprint: {}", sprintId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("Failed to get sprint holidays: " + e.getMessage()));
        }
    }

    /**
     * Delete event (meeting or deployment)
     */
    @DeleteMapping("/events/{eventId}")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(@PathVariable String eventId) {
        try {
            sprintEventService.deleteEvent(eventId);
            log.info("Deleted event: {}", eventId);
            
            return ResponseEntity.ok(ApiResponse.success("Event deleted", null));
            
        } catch (Exception e) {
            log.error("Error deleting event: {}", eventId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("Failed to delete event: " + e.getMessage()));
        }
    }

    // Helper methods
    private String getMeetingName(SprintEvent.MeetingType type) {
        return switch (type) {
            case PLANNING -> "Sprint Planning";
            case GROOMING -> "Backlog Grooming";
            case RETROSPECTIVE -> "Sprint Retrospective";
        };
    }

    private String getMeetingDescription(SprintEvent.MeetingType type) {
        return switch (type) {
            case PLANNING -> "Sprint planning session to review and commit to sprint backlog";
            case GROOMING -> "Backlog refinement session to prepare upcoming user stories";
            case RETROSPECTIVE -> "Sprint retrospective to reflect on what went well and what can be improved";
        };
    }
    
    /**
     * Generate AI-powered meeting invite
     */
    @PostMapping("/{sprintId}/meetings/{meetingType}/generate-invite")
    public ResponseEntity<ApiResponse<Map<String, String>>> generateAIInvite(
            @PathVariable String sprintId,
            @PathVariable String meetingType) {
        try {
            // Parse meeting type
            SprintEvent.MeetingType type = SprintEvent.MeetingType.valueOf(meetingType.toUpperCase());
            
            // Get meeting details
            SprintEventDto meeting = sprintEventService.getMeetingBySprintAndType(sprintId, type);
            if (meeting == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Meeting not found. Please save meeting details first."));
            }
            
            // Get sprint details
            SprintDto sprint = sprintService.getSprintById(sprintId);
            
            // Generate AI invite
            String aiInvite = aiService.generateMeetingInvite(meeting, sprint);
            
            // Parse the AI response to extract subject and body
            Map<String, String> inviteData = parseAIInvite(aiInvite);
            inviteData.put("date", meeting.eventDate().toString());
            inviteData.put("time", meeting.eventTime() != null ? meeting.eventTime().toString() : "");
            inviteData.put("duration", meeting.durationMinutes() != null ? meeting.durationMinutes().toString() : "60");
            inviteData.put("rawInvite", aiInvite);
            
            log.info("Generated AI invite for {} meeting in sprint: {}", type, sprintId);
            
            return ResponseEntity.ok(ApiResponse.success("AI invite generated", inviteData));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.failure("Invalid meeting type: " + meetingType));
        } catch (Exception e) {
            log.error("Error generating AI invite for sprint: {}", sprintId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("Failed to generate AI invite: " + e.getMessage()));
        }
    }
    
    /**
     * Parse AI-generated invite to extract subject and body
     */
    private Map<String, String> parseAIInvite(String aiInvite) {
        Map<String, String> result = new HashMap<>();
        
        // Try to extract subject line
        String subject = "Sprint Meeting";
        String body = aiInvite;
        
        if (aiInvite.contains("Subject:")) {
            int subjectIndex = aiInvite.indexOf("Subject:");
            int newlineIndex = aiInvite.indexOf("\n", subjectIndex);
            if (newlineIndex > subjectIndex) {
                subject = aiInvite.substring(subjectIndex + 8, newlineIndex).trim();
                body = aiInvite.substring(newlineIndex + 1).trim();
            }
        }
        
        result.put("subject", subject);
        result.put("body", body);
        
        return result;
    }
}
