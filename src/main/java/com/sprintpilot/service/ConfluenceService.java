package com.sprintpilot.service;

import com.sprintpilot.dto.SprintDto;
import java.util.Map;

/**
 * Service for managing Confluence pages related to sprints
 * Provides high-level operations for creating and updating sprint documentation
 */
public interface ConfluenceService {
    
    /**
     * Creates a Confluence page when sprint is started
     * Page title format: "Sprint {sprintId} - {month} {year}"
     * 
     * @param sprintDto Sprint data
     * @param spaceKey Confluence space key (e.g., "SPRINT")
     * @return Confluence page ID if created successfully, null otherwise
     */
    String createSprintPage(SprintDto sprintDto, String spaceKey);
    
    /**
     * Updates existing sprint page with completion summary
     * Searches for page by title format: "Sprint {sprintId} - {month} {year}"
     * 
     * @param sprintDto Sprint data with completion information
     * @param spaceKey Confluence space key
     * @param summaryData Additional summary data (metrics, tasks, etc.)
     * @return true if updated successfully, false otherwise
     */
    boolean updateSprintPageWithSummary(SprintDto sprintDto, String spaceKey, Map<String, Object> summaryData);
    
    /**
     * Checks if Confluence integration is enabled
     * 
     * @return true if enabled, false otherwise
     */
    boolean isEnabled();
    
    /**
     * Ensures that the sprint page exists in Confluence and returns the shareable link
     *
     * @param sprintDto Sprint details
     * @param spaceKey Optional space key override
     * @return URL to the Confluence page or null if generation failed
     */
    String generateSprintExportLink(SprintDto sprintDto, String spaceKey);
}

