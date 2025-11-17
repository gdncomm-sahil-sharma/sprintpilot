package com.sprintpilot.controller;

import com.sprintpilot.dto.ApiResponse;
import com.sprintpilot.dto.SprintDto;
import com.sprintpilot.dto.SprintEventDto;
import com.sprintpilot.dto.SprintMetricsDto;
import com.sprintpilot.service.SprintMetricsService;
import com.sprintpilot.service.SprintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sprints")
public class SprintController {
    
    @Autowired
    private SprintService sprintService;

    @Autowired
    private SprintMetricsService sprintMetricsService;
    
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
    
    @GetMapping("/completed")
    public ResponseEntity<ApiResponse<List<SprintDto>>> getCompletedSprints() {
        try {
            List<SprintDto> sprints = sprintService.getCompletedSprints();
            return ResponseEntity.ok(ApiResponse.success(sprints));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to fetch completed sprints", e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/start")
    public ResponseEntity<ApiResponse<SprintDto>> startSprint(@PathVariable String id) {
        try {
            SprintDto sprint = sprintService.startSprint(id);
            return ResponseEntity.ok(ApiResponse.success("Sprint started successfully", sprint));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to start sprint", e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<SprintDto>> completeSprint(@PathVariable String id) {
        try {
            SprintDto sprint = sprintService.completeSprint(id);
            return ResponseEntity.ok(ApiResponse.success("Sprint completed successfully", sprint));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to complete sprint", e.getMessage()));
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
}
