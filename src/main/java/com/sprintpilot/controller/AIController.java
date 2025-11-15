package com.sprintpilot.controller;

import com.sprintpilot.dto.*;
import com.sprintpilot.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AIController {
    
    @Autowired
    private AIService aiService;
    
    @PostMapping("/sprint-summary")
    public ResponseEntity<ApiResponse<String>> generateSprintSummary(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<TeamMemberDto> team = (List<TeamMemberDto>) request.get("team");
            SprintDto sprint = (SprintDto) request.get("sprint");
            @SuppressWarnings("unchecked")
            List<TaskDto> tasks = (List<TaskDto>) request.get("tasks");
            @SuppressWarnings("unchecked")
            List<CapacitySummaryDto> workload = (List<CapacitySummaryDto>) request.get("workload");
            
            String summary = aiService.generateSprintSummary(team, sprint, tasks, workload);
            return ResponseEntity.ok(ApiResponse.success(summary));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to generate sprint summary", e.getMessage()));
        }
    }
    
    @PostMapping("/meeting-invite")
    public ResponseEntity<ApiResponse<Map<String, String>>> generateMeetingInvite(@RequestBody Map<String, Object> request) {
        try {
            SprintEventDto meeting = (SprintEventDto) request.get("meeting");
            SprintDto sprint = (SprintDto) request.get("sprint");
            
            Map<String, String> details = aiService.generateMeetingDetails(meeting, sprint);
            return ResponseEntity.ok(ApiResponse.success(details));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to generate meeting invite", e.getMessage()));
        }
    }
    
    @PostMapping("/risk-summary")
    public ResponseEntity<ApiResponse<String>> generateRiskSummary(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<TaskDto> tasks = (List<TaskDto>) request.get("tasks");
            @SuppressWarnings("unchecked")
            List<TaskRiskDto> risks = (List<TaskRiskDto>) request.get("risks");
            
            String summary = aiService.generateRiskSummary(tasks, risks);
            return ResponseEntity.ok(ApiResponse.success(summary));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to generate risk summary", e.getMessage()));
        }
    }
    
    @PostMapping("/confluence-page")
    public ResponseEntity<ApiResponse<String>> generateConfluencePage(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<TeamMemberDto> team = (List<TeamMemberDto>) request.get("team");
            SprintDto sprint = (SprintDto) request.get("sprint");
            @SuppressWarnings("unchecked")
            List<TaskDto> tasks = (List<TaskDto>) request.get("tasks");
            @SuppressWarnings("unchecked")
            List<CapacitySummaryDto> workload = (List<CapacitySummaryDto>) request.get("workload");
            
            String page = aiService.generateConfluencePage(team, sprint, tasks, workload);
            return ResponseEntity.ok(ApiResponse.success(page));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to generate Confluence page", e.getMessage()));
        }
    }
    
    @PostMapping("/teams-message")
    public ResponseEntity<ApiResponse<String>> generateTeamsMessage(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<TeamMemberDto> team = (List<TeamMemberDto>) request.get("team");
            SprintDto sprint = (SprintDto) request.get("sprint");
            @SuppressWarnings("unchecked")
            List<TaskDto> tasks = (List<TaskDto>) request.get("tasks");
            @SuppressWarnings("unchecked")
            List<CapacitySummaryDto> workload = (List<CapacitySummaryDto>) request.get("workload");
            
            String message = aiService.generateTeamsMessage(team, sprint, tasks, workload);
            return ResponseEntity.ok(ApiResponse.success(message));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to generate Teams message", e.getMessage()));
        }
    }
    
    @PostMapping("/outlook-body")
    public ResponseEntity<ApiResponse<String>> generateOutlookBody(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<TeamMemberDto> team = (List<TeamMemberDto>) request.get("team");
            SprintDto sprint = (SprintDto) request.get("sprint");
            @SuppressWarnings("unchecked")
            List<TaskDto> tasks = (List<TaskDto>) request.get("tasks");
            @SuppressWarnings("unchecked")
            List<CapacitySummaryDto> workload = (List<CapacitySummaryDto>) request.get("workload");
            
            String body = aiService.generateOutlookBody(team, sprint, tasks, workload);
            return ResponseEntity.ok(ApiResponse.success(body));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to generate Outlook body", e.getMessage()));
        }
    }
    
    @PostMapping("/performance-insights")
    public ResponseEntity<ApiResponse<String>> analyzeHistoricalPerformance(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<SprintDto> sprints = (List<SprintDto>) request.get("sprints");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> velocityTrend = (List<Map<String, Object>>) request.get("velocityTrend");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> workMixTrend = (List<Map<String, Object>>) request.get("workMixTrend");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> roleUtilization = (List<Map<String, Object>>) request.get("roleUtilization");
            
            String insights = aiService.analyzeHistoricalPerformance(sprints, velocityTrend, workMixTrend, roleUtilization);
            return ResponseEntity.ok(ApiResponse.success(insights));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to generate performance insights", e.getMessage()));
        }
    }
}
