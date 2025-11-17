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

        List<SprintDto> activeSprints = sprintService.getActiveSprints();
        if (activeSprints == null || activeSprints.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("No active sprint available to export.", "No active sprint"));
        }

        SprintDto sprint = activeSprints.get(0);
        String pageUrl = confluenceService.generateSprintExportLink(sprint, null);
        if (pageUrl == null || pageUrl.isBlank()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to generate Confluence page link.", "Unable to determine page URL"));
        }

        return ResponseEntity.ok(ApiResponse.success("Confluence page generated successfully", pageUrl));
    }
}

