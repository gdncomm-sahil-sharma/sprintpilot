package com.sprintpilot.controller;

import com.sprintpilot.dto.ApiResponse;
import com.sprintpilot.dto.SprintDto;
import com.sprintpilot.service.ConfluenceService;
import com.sprintpilot.service.SprintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    @Autowired
    private SprintService sprintService;

    @Autowired(required = false)
    private ConfluenceService confluenceService;

    @PostMapping("/confluence")
    public ResponseEntity<ApiResponse<String>> exportActiveSprintToConfluence() {
        if (confluenceService == null || !confluenceService.isEnabled()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Confluence integration is not configured.", "Confluence is disabled"));
        }

        // Get only ACTIVE sprints (not PLANNING)
        List<SprintDto> allSprints = sprintService.getActiveSprints();
        List<SprintDto> activeSprints = allSprints.stream()
                .toList();
        
        if (activeSprints.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("No active sprint available to export. Please ensure a sprint is in ACTIVE status.", "No active sprint"));
        }

        // Get the first active sprint with full details
        SprintDto sprint = sprintService.getSprintWithFullDetails(activeSprints.getFirst().id());
        if (sprint == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Sprint not found.", "Sprint not found"));
        }

        String pageUrl = confluenceService.generateSprintExportLink(sprint, null);
        if (pageUrl == null || pageUrl.isBlank()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to generate Confluence page link.", "Unable to determine page URL"));
        }

        return ResponseEntity.ok(ApiResponse.success("Confluence page generated successfully", pageUrl));
    }
}

