package com.sprintpilot.controller;

import com.sprintpilot.config.AtlassianConfigProperties;
import com.sprintpilot.dto.ApiResponse;
import com.sprintpilot.dto.CompleteSprintResponse;
import com.sprintpilot.dto.SprintDto;
import com.sprintpilot.dto.SprintEventDto;
import com.sprintpilot.dto.SprintMetricsDto;
import com.sprintpilot.service.SprintMetricsService;
import com.sprintpilot.service.SprintService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sprints")
@Slf4j
public class SprintController {
    
    @Autowired
    private SprintService sprintService;

    @Autowired
    private SprintMetricsService sprintMetricsService;

    @Autowired
    private AtlassianConfigProperties atlassianConfigProperties;
    
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<SprintDto>> createSprint(@RequestBody SprintDto sprintDto) {
        try {
            SprintDto created = sprintService.createSprint(sprintDto);
            return ResponseEntity.ok(ApiResponse.success("Sprint created successfully", created));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to create sprint", e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SprintDto>> updateSprint(
            @PathVariable String id, 
            @RequestBody SprintDto sprintDto) {
        try {
            SprintDto updated = sprintService.updateSprint(id, sprintDto);
            return ResponseEntity.ok(ApiResponse.success("Sprint updated successfully", updated));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to update sprint", e.getMessage()));
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SprintDto>> getSprintById(@PathVariable String id) {
        try {
            SprintDto sprint = sprintService.getSprintWithFullDetails(id);
            return ResponseEntity.ok(ApiResponse.success(sprint));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Sprint not found", e.getMessage()));
        }
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<SprintDto>>> getAllSprints() {
        try {
            List<SprintDto> sprints = sprintService.getAllSprints();
            return ResponseEntity.ok(ApiResponse.success(sprints));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to fetch sprints", e.getMessage()));
        }
    }
    
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<SprintDto>>> getActiveSprints() {
        try {
            List<SprintDto> sprints = sprintService.getActiveSprints();
            return ResponseEntity.ok(ApiResponse.success(sprints));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to fetch active sprints", e.getMessage()));
        }
    }
    
    @GetMapping("/archived")
    public ResponseEntity<ApiResponse<List<SprintDto>>> getArchivedSprints() {
        try {
            List<SprintDto> sprints = sprintService.getArchivedSprints();
            return ResponseEntity.ok(ApiResponse.success(sprints));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to fetch archived sprints", e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<SprintDto>> archiveSprint(@PathVariable String id) {
        try {
            SprintDto sprint = sprintService.archiveSprint(id);
            return ResponseEntity.ok(ApiResponse.success("Sprint archived successfully", sprint));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to archive sprint", e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<SprintDto>> reactivateSprint(@PathVariable String id) {
        try {
            SprintDto sprint = sprintService.reactivateSprint(id);
            return ResponseEntity.ok(ApiResponse.success("Sprint reactivated successfully", sprint));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to reactivate sprint", e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}/archived")
    public ResponseEntity<ApiResponse<Void>> deleteLatestArchivedSprint(@PathVariable String id) {
        try {
            sprintService.deleteLatestArchivedSprint(id);
            return ResponseEntity.ok(ApiResponse.success("Latest archived sprint deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to delete archived sprint", e.getMessage()));
        }
    }
    
    @GetMapping("/{id}/can-complete")
    public ResponseEntity<ApiResponse<Map<String, Object>>> canCompleteSprint(@PathVariable String id) {
        try {
            boolean canComplete = sprintService.canCompleteSprint(id);
            SprintDto sprint = sprintService.getSprintById(id);
            
            // Calculate days remaining
            long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(
                java.time.LocalDate.now(), 
                sprint.endDate()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("canComplete", canComplete);
            response.put("daysRemaining", daysRemaining);
            response.put("reason", canComplete ? 
                "Sprint is ready to be completed" : 
                "Sprint end date has not been reached yet (" + daysRemaining + " days remaining)");
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to check sprint completion status", e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/complete-and-archive")
    public ResponseEntity<ApiResponse<CompleteSprintResponse>> completeAndArchiveSprint(@PathVariable String id) {
        try {
            CompleteSprintResponse response = sprintService.completeAndArchiveSprint(id);
            return ResponseEntity.ok(ApiResponse.success(
                "Sprint archived and next sprint created successfully", 
                response
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to complete sprint", e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSprint(@PathVariable String id) {
        try {
            sprintService.deleteSprint(id);
            return ResponseEntity.ok(ApiResponse.success("Sprint deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to delete sprint", e.getMessage()));
        }
    }
    
    @PostMapping("/{sprintId}/events")
    public ResponseEntity<ApiResponse<SprintDto>> addEvent(
            @PathVariable String sprintId,
            @RequestBody SprintEventDto event) {
        try {
            SprintDto sprint = sprintService.addEvent(sprintId, event);
            return ResponseEntity.ok(ApiResponse.success("Event added successfully", sprint));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to add event", e.getMessage()));
        }
    }
    
    @DeleteMapping("/{sprintId}/events/{eventId}")
    public ResponseEntity<ApiResponse<SprintDto>> removeEvent(
            @PathVariable String sprintId,
            @PathVariable String eventId) {
        try {
            SprintDto sprint = sprintService.removeEvent(sprintId, eventId);
            return ResponseEntity.ok(ApiResponse.success("Event removed successfully", sprint));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to remove event", e.getMessage()));
        }
    }
    
    @PostMapping("/{sprintId}/team/{memberId}")
    public ResponseEntity<ApiResponse<SprintDto>> addTeamMember(
            @PathVariable String sprintId,
            @PathVariable String memberId) {
        try {
            SprintDto sprint = sprintService.addTeamMember(sprintId, memberId);
            return ResponseEntity.ok(ApiResponse.success("Team member added successfully", sprint));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to add team member", e.getMessage()));
        }
    }
    
    @DeleteMapping("/{sprintId}/team/{memberId}")
    public ResponseEntity<ApiResponse<SprintDto>> removeTeamMember(
            @PathVariable String sprintId,
            @PathVariable String memberId) {
        try {
            SprintDto sprint = sprintService.removeTeamMember(sprintId, memberId);
            return ResponseEntity.ok(ApiResponse.success("Team member removed successfully", sprint));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to remove team member", e.getMessage()));
        }
    }
    
    @PostMapping("/calculate-dates")
    public ResponseEntity<ApiResponse<SprintDto>> calculateSprintDates(@RequestBody Map<String, Object> request) {
        try {
            LocalDate startDate = LocalDate.parse((String) request.get("startDate"));
            Integer duration = (Integer) request.get("duration");
            @SuppressWarnings("unchecked")
            List<String> holidays = (List<String>) request.getOrDefault("holidays", List.of());
            
            SprintDto calculated = sprintService.calculateSprintDates(startDate, duration, holidays);
            return ResponseEntity.ok(ApiResponse.success(calculated));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to calculate sprint dates", e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/clone")
    public ResponseEntity<ApiResponse<SprintDto>> cloneSprint(@PathVariable String id) {
        try {
            SprintDto cloned = sprintService.cloneSprint(id);
            return ResponseEntity.ok(ApiResponse.success("Sprint cloned successfully", cloned));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to clone sprint", e.getMessage()));
        }
    }

    /**
     * Fetch sprint burndown and velocity metrics from Jira
     *
     * @param request Map containing projectName and sprintId
     * @return Sprint metrics including burndown and velocity data
     */
    @PostMapping("/metrics/burndown-velocity")
    public ResponseEntity<ApiResponse<SprintMetricsDto>> getSprintMetrics(@RequestBody Map<String, String> request) {
        String sprintId = request.get("sprintId");
        String projectName = request.get("projectName");
        if(!StringUtils.hasText(projectName)) {
            projectName = atlassianConfigProperties.getJiraProjectName();
        }

        if (!StringUtils.hasText(sprintId) || !StringUtils.hasText(projectName)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Both sprintId and projectName are required", null));
        }

        try {
            SprintMetricsDto metrics = sprintMetricsService.getSprintMetrics(sprintId, projectName);
            return ResponseEntity.ok(ApiResponse.success("Sprint metrics fetched successfully", metrics));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to fetch sprint metrics", e.getMessage()));
        }
    }

    /**
     * Get work distribution by task category for a sprint
     *
     * @param request Map containing sprintId and optional projectName
     * @return Work distribution data showing percentage of different task types
     */
    @PostMapping("/metrics/workDistribution")
    public ResponseEntity<ApiResponse<com.sprintpilot.dto.WorkDistributionDto>> getWorkDistribution(@RequestBody Map<String, String> request) {
        String sprintId = request.get("sprintId");
        String projectName = request.get("projectName");
        
        if(!StringUtils.hasText(projectName)) {
            projectName = atlassianConfigProperties.getJiraProjectName();
        }

        if (!StringUtils.hasText(sprintId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("sprintId is required", null));
        }

        try {
            com.sprintpilot.dto.WorkDistributionDto distribution = sprintMetricsService.getWorkDistribution(sprintId, projectName);
            return ResponseEntity.ok(ApiResponse.success("Work distribution calculated successfully", distribution));
        } catch (Exception e) {
            log.error("Failed to calculate work distribution", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to calculate work distribution", e.getMessage()));
        }
    }

    /**
     * Get velocity trend data for current sprint and last 5 completed sprints
     *
     * @param request Map containing currentSprintId
     * @return Velocity trend data across multiple sprints
     */
    @PostMapping("/metrics/velocityTrend")
    public ResponseEntity<ApiResponse<com.sprintpilot.dto.VelocityTrendDto>> getVelocityTrend(@RequestBody Map<String, String> request) {
        String currentSprintId = request.get("sprintId");

        if (!StringUtils.hasText(currentSprintId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("sprintId is required", null));
        }

        try {
            com.sprintpilot.dto.VelocityTrendDto velocityTrend = sprintMetricsService.getVelocityTrend(currentSprintId);
            return ResponseEntity.ok(ApiResponse.success("Velocity trend calculated successfully", velocityTrend));
        } catch (Exception e) {
            log.error("Failed to calculate velocity trend", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to calculate velocity trend", e.getMessage()));
        }
    }
    
    // New endpoints for sprint management starting point
    
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<SprintDto>> getCurrentActiveSprint() {
        try {
            SprintDto currentSprint = sprintService.getCurrentActiveSprint();
            if (currentSprint != null) {
                return ResponseEntity.ok(ApiResponse.success("Current active sprint found", currentSprint));
            } else {
                return ResponseEntity.ok(ApiResponse.success("No active sprint found", null));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to get current sprint", e.getMessage()));
        }
    }
    
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSprintStatus() {
        try {
            boolean hasActive = sprintService.hasActiveSprint();
            SprintDto currentSprint = sprintService.getCurrentActiveSprint();
            List<SprintDto> templates = sprintService.getSprintTemplates();
            
            Map<String, Object> status = new HashMap<>();
            status.put("hasActiveSprint", hasActive);
            status.put("currentSprint", currentSprint);
            status.put("hasTemplates", !templates.isEmpty());
            status.put("templatesCount", templates.size());
            
            return ResponseEntity.ok(ApiResponse.success("Sprint status retrieved", status));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to get sprint status", e.getMessage()));
        }
    }
    
    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<List<SprintDto>>> getSprintTemplates() {
        try {
            List<SprintDto> templates = sprintService.getSprintTemplates();
            return ResponseEntity.ok(ApiResponse.success("Sprint templates retrieved", templates));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to get sprint templates", e.getMessage()));
        }
    }
    
    @PostMapping("/create-from-template")
    public ResponseEntity<ApiResponse<SprintDto>> createSprintFromTemplate(@RequestBody Map<String, Object> request) {
        try {
            String templateId = (String) request.get("templateId");
            String startDateStr = (String) request.get("startDate");
            
            if (!StringUtils.hasText(templateId) || !StringUtils.hasText(startDateStr)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Template ID and start date are required", null));
            }
            
            LocalDate startDate = LocalDate.parse(startDateStr);
            SprintDto newSprint = sprintService.createSprintFromTemplate(templateId, startDate);
            
            return ResponseEntity.ok(ApiResponse.success("Sprint created from template successfully", newSprint));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to create sprint from template", e.getMessage()));
        }
    }
}
