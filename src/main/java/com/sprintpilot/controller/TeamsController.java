package com.sprintpilot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprintpilot.dto.ApiResponse;
import com.sprintpilot.dto.SprintDto;
import com.sprintpilot.notification.teams.TeamsNotificationService;
import com.sprintpilot.service.SprintService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for Microsoft Teams notification endpoints
 */
@RestController
@RequestMapping("/api/teams")
@Slf4j
public class TeamsController {
    
    @Autowired
    private TeamsNotificationService teamsNotificationService;
    
    @Autowired
    private SprintService sprintService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Sends AI Sprint Summary to Teams channel
     * 
     * @param request Map containing sprintId and summaryText
     * @return Success or error response
     */
    @PostMapping("/send-sprint-summary")
    public ResponseEntity<ApiResponse<String>> sendSprintSummaryToTeams(@RequestBody Map<String, String> request) {
        String sprintId = request.get("sprintId");
        String summaryText = request.get("summaryText");
        
        if (sprintId == null || sprintId.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("sprintId is required", null));
        }
        
        if (summaryText == null || summaryText.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("summaryText is required", null));
        }
        
        if (!teamsNotificationService.isEnabled()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Teams notifications are not configured. Please configure teams.webhook.url in application properties.", null));
        }
        
        try {
            // Fetch sprint details
            SprintDto sprint = sprintService.getSprintById(sprintId);
            if (sprint == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Sprint not found: " + sprintId, null));
            }
            
            // Build Teams MessageCard dynamically
            String messageJson = buildSprintSummaryMessageCard(sprint, summaryText);
            
            // Send to Teams
            boolean sent = teamsNotificationService.sendRawMessage(messageJson);
            
            if (sent) {
                log.info("Sprint summary sent to Teams for sprint: {}", sprintId);
                return ResponseEntity.ok(ApiResponse.success("Sprint summary sent to Teams successfully", null));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to send sprint summary to Teams", null));
            }
            
        } catch (Exception e) {
            log.error("Failed to send sprint summary to Teams for sprint: {}", sprintId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to send sprint summary to Teams: " + e.getMessage(), null));
        }
    }
    
    /**
     * Builds a Teams MessageCard JSON for sprint summary
     * Uses MessageCard format (not Adaptive Card) for better compatibility
     */
    private String buildSprintSummaryMessageCard(SprintDto sprint, String summaryText) {
        try {
            Map<String, Object> messageCard = new HashMap<>();
            messageCard.put("@type", "MessageCard");
            messageCard.put("@context", "https://schema.org/extensions");
            messageCard.put("themeColor", "0078D4");
            
            // Title
            String sprintName = sprint.sprintName() != null && !sprint.sprintName().trim().isEmpty() 
                ? sprint.sprintName() 
                : "Sprint " + sprint.id();
            messageCard.put("summary", "AI Sprint Summary - " + sprintName);
            messageCard.put("title", "🤖 AI Sprint Summary");
            
            // Sections
            java.util.List<Map<String, Object>> sections = new java.util.ArrayList<>();
            
            // Sprint Info Section
            Map<String, Object> sprintInfoSection = new HashMap<>();
            sprintInfoSection.put("activityTitle", sprintName);
            sprintInfoSection.put("activitySubtitle", "Sprint Overview");
            
            java.util.List<Map<String, String>> sprintFacts = new java.util.ArrayList<>();
            
            if (sprint.startDate() != null) {
                Map<String, String> startDateFact = new HashMap<>();
                startDateFact.put("name", "Start Date:");
                startDateFact.put("value", sprint.startDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
                sprintFacts.add(startDateFact);
            }
            
            if (sprint.endDate() != null) {
                Map<String, String> endDateFact = new HashMap<>();
                endDateFact.put("name", "End Date:");
                endDateFact.put("value", sprint.endDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
                sprintFacts.add(endDateFact);
            }
            
            if (sprint.duration() != null) {
                Map<String, String> durationFact = new HashMap<>();
                durationFact.put("name", "Duration:");
                durationFact.put("value", sprint.duration() + " working days");
                sprintFacts.add(durationFact);
            }
            
            if (sprint.teamMembers() != null && !sprint.teamMembers().isEmpty()) {
                Map<String, String> teamSizeFact = new HashMap<>();
                teamSizeFact.put("name", "Team Size:");
                teamSizeFact.put("value", String.valueOf(sprint.teamMembers().size()) + " members");
                sprintFacts.add(teamSizeFact);
            }
            
            if (sprint.tasks() != null && !sprint.tasks().isEmpty()) {
                int totalStoryPoints = sprint.tasks().stream()
                    .mapToInt(t -> t.storyPoints() != null ? t.storyPoints().intValue() : 0)
                    .sum();
                if (totalStoryPoints > 0) {
                    Map<String, String> storyPointsFact = new HashMap<>();
                    storyPointsFact.put("name", "Total Story Points:");
                    storyPointsFact.put("value", String.valueOf(totalStoryPoints) + " SP");
                    sprintFacts.add(storyPointsFact);
                }
            }
            
            sprintInfoSection.put("facts", sprintFacts);
            sections.add(sprintInfoSection);
            
            // AI Summary Section
            Map<String, Object> summarySection = new HashMap<>();
            summarySection.put("activityTitle", "📊 AI-Generated Insights");
            summarySection.put("activitySubtitle", "Summary generated by SprintPilot AI");
            
            // Format summary text - convert markdown-like formatting to plain text with line breaks
            String formattedSummary = formatSummaryForTeams(summaryText);
            summarySection.put("text", formattedSummary);
            
            sections.add(summarySection);
            
            messageCard.put("sections", sections);
            
            // Footer
            Map<String, Object> potentialAction = new HashMap<>();
            potentialAction.put("@type", "OpenUri");
            potentialAction.put("name", "View Sprint in SprintPilot");
            
            java.util.List<Map<String, String>> targets = new java.util.ArrayList<>();
            Map<String, String> target = new HashMap<>();
            target.put("os", "default");
            target.put("uri", "https://sprintpilot.example.com/dashboard"); // Update with actual URL if needed
            targets.add(target);
            potentialAction.put("targets", targets);
            
            messageCard.put("potentialAction", java.util.Arrays.asList(potentialAction));
            
            return objectMapper.writeValueAsString(messageCard);
            
        } catch (Exception e) {
            log.error("Failed to build Teams message card", e);
            throw new RuntimeException("Failed to build Teams message card: " + e.getMessage(), e);
        }
    }
    
    /**
     * Formats summary text for Teams MessageCard
     * Converts markdown-style formatting to plain text with proper line breaks
     */
    private String formatSummaryForTeams(String summaryText) {
        if (summaryText == null || summaryText.trim().isEmpty()) {
            return "No summary available.";
        }
        
        // Remove markdown bold (**text**) but keep the text
        String formatted = summaryText.replaceAll("\\*\\*(.*?)\\*\\*", "$1");
        
        // Convert bullet points (- item) to Teams-friendly format
        formatted = formatted.replaceAll("^- ", "• ");
        
        // Ensure proper line breaks
        formatted = formatted.replaceAll("\n", "\n\n");
        
        // Clean up multiple consecutive newlines
        formatted = formatted.replaceAll("\n{3,}", "\n\n");
        
        return formatted.trim();
    }
    
    /**
     * Sends AI Risk Summary to Teams channel
     * 
     * @param request Map containing sprintId and summaryText
     * @return Success or error response
     */
    @PostMapping("/send-risk-summary")
    public ResponseEntity<ApiResponse<String>> sendRiskSummaryToTeams(@RequestBody Map<String, String> request) {
        String sprintId = request.get("sprintId");
        String summaryText = request.get("summaryText");
        
        if (sprintId == null || sprintId.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("sprintId is required", null));
        }
        
        if (summaryText == null || summaryText.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("summaryText is required", null));
        }
        
        if (!teamsNotificationService.isEnabled()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Teams notifications are not configured. Please configure teams.webhook.url in application properties.", null));
        }
        
        try {
            // Fetch sprint details
            SprintDto sprint = sprintService.getSprintById(sprintId);
            if (sprint == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Sprint not found: " + sprintId, null));
            }
            
            // Build Teams MessageCard dynamically for risk summary
            String messageJson = buildRiskSummaryMessageCard(sprint, summaryText);
            
            // Send to Teams
            boolean sent = teamsNotificationService.sendRawMessage(messageJson);
            
            if (sent) {
                log.info("Risk summary sent to Teams for sprint: {}", sprintId);
                return ResponseEntity.ok(ApiResponse.success("Risk summary sent to Teams successfully", null));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to send risk summary to Teams", null));
            }
            
        } catch (Exception e) {
            log.error("Failed to send risk summary to Teams for sprint: {}", sprintId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to send risk summary to Teams: " + e.getMessage(), null));
        }
    }
    
    /**
     * Builds a Teams MessageCard JSON for risk summary
     * Uses MessageCard format with risk-specific styling (red/orange theme)
     */
    private String buildRiskSummaryMessageCard(SprintDto sprint, String summaryText) {
        try {
            Map<String, Object> messageCard = new HashMap<>();
            messageCard.put("@type", "MessageCard");
            messageCard.put("@context", "https://schema.org/extensions");
            messageCard.put("themeColor", "FF6B35"); // Orange/Red theme for risks
            
            // Title
            String sprintName = sprint.sprintName() != null && !sprint.sprintName().trim().isEmpty() 
                ? sprint.sprintName() 
                : "Sprint " + sprint.id();
            messageCard.put("summary", "AI Risk Summary - " + sprintName);
            messageCard.put("title", "⚠️ AI Risk Summary");
            
            // Sections
            java.util.List<Map<String, Object>> sections = new java.util.ArrayList<>();
            
            // Sprint Info Section
            Map<String, Object> sprintInfoSection = new HashMap<>();
            sprintInfoSection.put("activityTitle", sprintName);
            sprintInfoSection.put("activitySubtitle", "Risk Analysis Overview");
            
            java.util.List<Map<String, String>> sprintFacts = new java.util.ArrayList<>();
            
            if (sprint.startDate() != null) {
                Map<String, String> startDateFact = new HashMap<>();
                startDateFact.put("name", "Start Date:");
                startDateFact.put("value", sprint.startDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
                sprintFacts.add(startDateFact);
            }
            
            if (sprint.endDate() != null) {
                Map<String, String> endDateFact = new HashMap<>();
                endDateFact.put("name", "End Date:");
                endDateFact.put("value", sprint.endDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
                sprintFacts.add(endDateFact);
            }
            
            if (sprint.tasks() != null && !sprint.tasks().isEmpty()) {
                int totalTasks = sprint.tasks().size();
                Map<String, String> tasksFact = new HashMap<>();
                tasksFact.put("name", "Total Tasks:");
                tasksFact.put("value", String.valueOf(totalTasks));
                sprintFacts.add(tasksFact);
            }
            
            sprintInfoSection.put("facts", sprintFacts);
            sections.add(sprintInfoSection);
            
            // Risk Summary Section
            Map<String, Object> summarySection = new HashMap<>();
            summarySection.put("activityTitle", "🔍 AI-Generated Risk Analysis");
            summarySection.put("activitySubtitle", "Critical issues and recommendations");
            
            // Format summary text for Teams
            String formattedSummary = formatSummaryForTeams(summaryText);
            summarySection.put("text", formattedSummary);
            
            sections.add(summarySection);
            
            messageCard.put("sections", sections);
            
            // Footer with action
            Map<String, Object> potentialAction = new HashMap<>();
            potentialAction.put("@type", "OpenUri");
            potentialAction.put("name", "View Tasks in SprintPilot");
            
            java.util.List<Map<String, String>> targets = new java.util.ArrayList<>();
            Map<String, String> target = new HashMap<>();
            target.put("os", "default");
            target.put("uri", "https://sprintpilot.example.com/tasks"); // Update with actual URL if needed
            targets.add(target);
            potentialAction.put("targets", targets);
            
            messageCard.put("potentialAction", java.util.Arrays.asList(potentialAction));
            
            return objectMapper.writeValueAsString(messageCard);
            
        } catch (Exception e) {
            log.error("Failed to build Teams risk summary message card", e);
            throw new RuntimeException("Failed to build Teams risk summary message card: " + e.getMessage(), e);
        }
    }
}

