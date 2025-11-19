package com.sprintpilot.service.impl;

import com.sprintpilot.confluence.ConfluencePageBuilder;
import com.sprintpilot.dto.HolidayDto;
import com.sprintpilot.dto.SprintDto;
import com.sprintpilot.dto.WorkDistributionDto;
import com.sprintpilot.service.ConfluenceClient;
import com.sprintpilot.service.ConfluenceService;
import com.sprintpilot.service.HolidayService;
import com.sprintpilot.service.SprintMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of ConfluenceService
 * Provides sprint documentation in Confluence with search-based page management
 * Follows the same pattern as TeamsNotificationService
 */
@Service
public class ConfluenceServiceImpl implements ConfluenceService {
    
    private static final Logger log = LoggerFactory.getLogger(ConfluenceServiceImpl.class);
    
    private final ConfluenceClient confluenceClient;
    private final ConfluencePageBuilder pageBuilder;
    private final String defaultSpaceKey;
    private final HolidayService holidayService;
    private final SprintMetricsService sprintMetricsService;
    
    @Autowired
    public ConfluenceServiceImpl(
            ConfluenceClient confluenceClient,
            ConfluencePageBuilder pageBuilder,
            @Value("${confluence.space-key:SP}") String defaultSpaceKey,
            HolidayService holidayService,
            SprintMetricsService sprintMetricsService) {
        this.confluenceClient = confluenceClient;
        this.pageBuilder = pageBuilder;
        this.defaultSpaceKey = defaultSpaceKey;
        this.holidayService = holidayService;
        this.sprintMetricsService = sprintMetricsService;
    }
    
    @Override
    public String createSprintPage(SprintDto sprintDto, String spaceKey) {
        if (sprintDto == null) {
            log.warn("Cannot create Confluence page: SprintDto is null");
            return null;
        }
        
        String actualSpaceKey = spaceKey != null ? spaceKey : defaultSpaceKey;
        
        try {
            String pageId = ensureSprintPlanningPage(sprintDto, actualSpaceKey);
            log.info("Confluence page available for sprint {} (ID: {})", sprintDto.id(), pageId);
            return pageId;
            
        } catch (Exception e) {
            log.error("Failed to create Confluence page for sprint: {}", sprintDto.id(), e);
            return null;
        }
    }
    
    @Override
    public boolean updateSprintPageWithSummary(SprintDto sprintDto, String spaceKey, Map<String, Object> summaryData) {
        if (sprintDto == null) {
            log.warn("Cannot update Confluence page: SprintDto is null");
            return false;
        }
        
        String actualSpaceKey = spaceKey != null ? spaceKey : defaultSpaceKey;
        
        try {
            // Build page title: "Sprint {sprintId} - {month} {year}"
            String pageTitle = buildSprintPageTitle(sprintDto);
            
            // Search for existing page
            String pageId = confluenceClient.searchPageByTitle(actualSpaceKey, pageTitle);
            
            if (pageId == null) {
                log.warn("Confluence page not found: {}. Creating new page.", pageTitle);
                // Create page if it doesn't exist
                pageId = createSprintPage(sprintDto, actualSpaceKey);
                if (pageId == null) {
                    return false;
                }
            }
            
            // Get current page to get version
            Map<String, Object> currentPage = confluenceClient.getPage(pageId);
            Integer currentVersion = (Integer) currentPage.get("version");
            
            // Build summary section variables
            Map<String, String> variables = buildSummarySectionVariables(sprintDto, summaryData);
            
            // Build summary section from template
            String summaryContent = pageBuilder.buildPageContent("sprint-summary-section.html", variables);
            
            // Convert HTML to Confluence format
            String confluenceSummary = pageBuilder.htmlToConfluenceStorage(summaryContent);
            
            // Append summary to existing page
            confluenceClient.appendToPage(pageId, confluenceSummary, currentVersion);
            
            log.info("Confluence page updated with summary: {} (ID: {})", pageTitle, pageId);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to update Confluence page for sprint: {}", sprintDto.id(), e);
            return false;
        }
    }
    
    @Override
    public boolean isEnabled() {
        // Check if Confluence is configured (space key is set)
        return defaultSpaceKey != null && !defaultSpaceKey.trim().isEmpty();
    }

    @Override
    public String generateSprintExportLink(SprintDto sprintDto, String spaceKey) {
        if (!isEnabled() || sprintDto == null) {
            return null;
        }
        
        String actualSpaceKey = spaceKey != null ? spaceKey : defaultSpaceKey;
        try {
            String pageId = ensureSprintPlanningPage(sprintDto, actualSpaceKey);
            if (pageId == null) {
                return null;
            }
            Map<String, Object> pageData = confluenceClient.getPage(pageId);
            return extractPageUrl(pageData);
        } catch (Exception e) {
            log.error("Failed to generate Confluence export link for sprint: {}", sprintDto.id(), e);
            return null;
        }
    }
    
    /**
     * Builds page title using sprint name from Sprint entity
     */
    private String buildSprintPageTitle(SprintDto sprintDto) {
        if (sprintDto.sprintName() != null && !sprintDto.sprintName().trim().isEmpty()) {
            return sprintDto.sprintName().trim();
        }
        // Fallback if sprint name is not available
        if (sprintDto.id() != null) {
            return "Sprint " + sprintDto.id();
        }
        return "Sprint Planning";
    }
    
    private String ensureSprintPlanningPage(SprintDto sprintDto, String spaceKey) throws Exception {
        String pageTitle = buildSprintPageTitle(sprintDto);
        String pageId = confluenceClient.searchPageByTitle(spaceKey, pageTitle);
        
        Map<String, String> variables = buildPlanningPageVariables(sprintDto);
        String content = pageBuilder.buildPageContent("sprint-planning-page.html", variables);
        String confluenceContent = pageBuilder.htmlToConfluenceStorage(content);
        
        if (pageId != null) {
            // Page exists - update it with latest data
            Map<String, Object> currentPage = confluenceClient.getPage(pageId);
            Integer currentVersion = (Integer) currentPage.get("version");
            confluenceClient.updatePage(pageId, confluenceContent, currentVersion + 1);
            log.info("Updated existing Confluence page: {} (ID: {})", pageTitle, pageId);
            return pageId;
        } else {
            // Page doesn't exist - create new one
            String newPageId = confluenceClient.createPage(spaceKey, pageTitle, confluenceContent, null);
            log.info("Created new Confluence page: {} (ID: {})", pageTitle, newPageId);
            return newPageId;
        }
    }
    
    private String extractPageUrl(Map<String, Object> pageData) {
        if (pageData == null) {
            return null;
        }
        Object webUrl = pageData.get("webUrl");
        if (webUrl instanceof String url && !url.isBlank()) {
            return url;
        }
        Object tiny = pageData.get("tinyUrl");
        if (tiny instanceof String tinyUrl && !tinyUrl.isBlank()) {
            return tinyUrl;
        }
        return null;
    }
    
    /**
     * Builds variables for sprint planning page template
     */
    private Map<String, String> buildPlanningPageVariables(SprintDto sprintDto) {
        Map<String, String> variables = new HashMap<>();
        
        // Basic sprint information
        String sprintName = sprintDto.sprintName() != null && !sprintDto.sprintName().trim().isEmpty() 
            ? sprintDto.sprintName().trim() 
            : "Sprint " + (sprintDto.id() != null ? sprintDto.id() : "Unknown");
        variables.put("sprintName", sprintName);
        variables.put("startDate", sprintDto.startDate() != null ? sprintDto.startDate().toString() : "");
        variables.put("endDate", sprintDto.endDate() != null ? sprintDto.endDate().toString() : "");
        variables.put("duration", sprintDto.duration() != null ? String.valueOf(sprintDto.duration()) : "0");
        variables.put("status", sprintDto.status() != null ? sprintDto.status().toString() : "");
        
        // Freeze date
        if (sprintDto.freezeDate() != null) {
            variables.put("freezeDate", sprintDto.freezeDate().toString());
            variables.put("freezeDateRow", "<tr><td><strong>Freeze Date:</strong></td><td>" + sprintDto.freezeDate().toString() + "</td></tr>");
        } else {
            variables.put("freezeDate", "");
            variables.put("freezeDateRow", "");
        }
        
        // Team information with Confluence user mentions
        if (sprintDto.teamMembers() != null && !sprintDto.teamMembers().isEmpty()) {
            variables.put("teamSize", String.valueOf(sprintDto.teamMembers().size()));
            // Format team members list with Confluence user mentions
            StringBuilder teamMembersList = new StringBuilder();
            teamMembersList.append("<ul>");
            for (var member : sprintDto.teamMembers()) {
                teamMembersList.append("<li>");
                
                // Add Confluence user mention using @username format
                String mention = formatConfluenceUserMention(member.name());
                teamMembersList.append(mention);
                
                teamMembersList.append(" - ");
                teamMembersList.append(member.role() != null ? member.role().toString() : "");
                if (member.dailyCapacity() != null) {
                    teamMembersList.append(" (Daily Capacity: ").append(member.dailyCapacity()).append(" hours)");
                }
                teamMembersList.append("</li>");
            }
            teamMembersList.append("</ul>");
            variables.put("teamMembers", teamMembersList.toString());
        } else {
            variables.put("teamSize", "0");
            variables.put("teamMembers", "<p><em>No team members assigned.</em></p>");
        }
        
        // Capacity and metrics (calculated from sprint data)
        variables.put("totalCapacity", calculateTotalCapacity(sprintDto));
        variables.put("sprintCapacity", calculateSprintCapacity(sprintDto));
        variables.put("plannedStoryPoints", calculatePlannedStoryPoints(sprintDto));
        variables.put("averageVelocity", calculateAverageVelocity(sprintDto));
        variables.put("workingDays", String.valueOf(sprintDto.duration() != null ? sprintDto.duration() : 0));
        
        // Fetch holidays from database for sprint date range
        List<HolidayDto> holidays = List.of();
        if (sprintDto.startDate() != null && sprintDto.endDate() != null) {
            try {
                holidays = holidayService.getHolidaysByDateRange(sprintDto.startDate(), sprintDto.endDate());
            } catch (Exception e) {
                log.warn("Failed to fetch holidays for sprint {}: {}", sprintDto.id(), e.getMessage());
            }
        }
        
        if (holidays.isEmpty()) {
            variables.put("holidaysInfo", "<p><strong>Holidays during sprint:</strong> <em>No holidays during this sprint period.</em></p>");
        } else {
            // Format holidays as HTML list with names
            StringBuilder holidaysList = new StringBuilder();
            holidaysList.append("<p><strong>Holidays during sprint:</strong></p><ul>");
            for (HolidayDto holiday : holidays) {
                holidaysList.append("<li><strong>").append(escapeHtml(holiday.name())).append("</strong>");
                if (holiday.holidayDate() != null) {
                    holidaysList.append(" - ").append(holiday.holidayDate().toString());
                }
                if (holiday.location() != null && !holiday.location().isEmpty()) {
                    holidaysList.append(" (Locations: ").append(String.join(", ", holiday.location())).append(")");
                }
                holidaysList.append("</li>");
            }
            holidaysList.append("</ul>");
            variables.put("holidaysInfo", holidaysList.toString());
        }
        
        // Sprint goals (if available in future)
        variables.put("sprintGoals", "<p><em>Sprint goals to be defined during planning meeting.</em></p>");
        variables.put("risks", "<p><em>No identified risks at sprint start.</em></p>");
        
        // Tasks information - removed as per requirement
        variables.put("plannedTasks", "");
        variables.put("totalPlannedStoryPoints", "0");
        
        // Category breakdown
        Map<String, String> categoryBreakdown = calculateCategoryBreakdown(sprintDto);
        variables.putAll(categoryBreakdown);
        
        // Events
        if (sprintDto.events() != null && !sprintDto.events().isEmpty()) {
            variables.put("events", formatEventsForTemplate(new java.util.ArrayList<>(sprintDto.events())));
        } else {
            variables.put("events", "<p><em>Sprint events to be scheduled.</em></p>");
        }
        
        // Timestamps
        variables.put("createdAt", java.time.LocalDateTime.now().toString());
        variables.put("updatedAt", java.time.LocalDateTime.now().toString());
        
        return variables;
    }
    
    /**
     * Builds variables for sprint summary section template
     */
    private Map<String, String> buildSummarySectionVariables(SprintDto sprintDto, Map<String, Object> summaryData) {
        Map<String, String> variables = new HashMap<>();
        
        // Completion information
        variables.put("completionDate", java.time.LocalDate.now().toString());
        variables.put("completionStatus", sprintDto.status() != null ? sprintDto.status().toString() : "UNKNOWN");
        
        // Metrics from summaryData
        if (summaryData != null) {
            variables.put("totalStoryPoints", getStringValue(summaryData, "totalStoryPoints", "0"));
            variables.put("completedStoryPoints", getStringValue(summaryData, "completedStoryPoints", "0"));
            variables.put("incompleteStoryPoints", getStringValue(summaryData, "incompleteStoryPoints", "0"));
            variables.put("completionPercentage", getStringValue(summaryData, "completionPercentage", "0"));
            variables.put("velocity", getStringValue(summaryData, "velocity", "0"));
            variables.put("teamSize", getStringValue(summaryData, "teamSize", "0"));
            variables.put("averageVelocity", getStringValue(summaryData, "averageVelocity", "0"));
            variables.put("averageVelocity3Sprints", getStringValue(summaryData, "averageVelocity3Sprints", "0"));
            variables.put("velocityTrend", getStringValue(summaryData, "velocityTrend", "Stable"));
            variables.put("velocityVsPlan", getStringValue(summaryData, "velocityVsPlan", "N/A"));
            
            // Tasks
            java.util.List<?> completedTasksList = getListValue(summaryData, "completedTasks");
            variables.put("completedTasks", completedTasksList != null && !completedTasksList.isEmpty() 
                    ? formatTasksForTemplate(completedTasksList) 
                    : "<p><em>No tasks were completed in this sprint.</em></p>");
            variables.put("completedTasksCount", getStringValue(summaryData, "completedTasksCount", "0"));
            
            java.util.List<?> incompleteTasksList = getListValue(summaryData, "incompleteTasks");
            variables.put("incompleteTasks", incompleteTasksList != null && !incompleteTasksList.isEmpty()
                    ? formatTasksForTemplate(incompleteTasksList)
                    : "<p><strong>🎉 All planned tasks were completed!</strong></p>");
            variables.put("incompleteTasksCount", getStringValue(summaryData, "incompleteTasksCount", "0"));
            
            // Category analysis
            variables.put("featurePlanned", getStringValue(summaryData, "featurePlanned", "0"));
            variables.put("featureCompleted", getStringValue(summaryData, "featureCompleted", "0"));
            variables.put("featureCompletionPercentage", getStringValue(summaryData, "featureCompletionPercentage", "0"));
            variables.put("techDebtPlanned", getStringValue(summaryData, "techDebtPlanned", "0"));
            variables.put("techDebtCompleted", getStringValue(summaryData, "techDebtCompleted", "0"));
            variables.put("techDebtCompletionPercentage", getStringValue(summaryData, "techDebtCompletionPercentage", "0"));
            variables.put("prodIssuePlanned", getStringValue(summaryData, "prodIssuePlanned", "0"));
            variables.put("prodIssueCompleted", getStringValue(summaryData, "prodIssueCompleted", "0"));
            variables.put("prodIssueCompletionPercentage", getStringValue(summaryData, "prodIssueCompletionPercentage", "0"));
            
            // Team performance
            variables.put("activeTeamMembers", getStringValue(summaryData, "activeTeamMembers", "0"));
            variables.put("capacityUtilized", getStringValue(summaryData, "capacityUtilized", "0"));
            variables.put("storyPointsPerMember", getStringValue(summaryData, "storyPointsPerMember", "0"));
            
            // Additional insights
            java.util.List<?> blockersList = getListValue(summaryData, "blockers");
            variables.put("blockers", blockersList != null && !blockersList.isEmpty()
                    ? formatListForTemplate(blockersList)
                    : "<p><em>No major blockers encountered during this sprint.</em></p>");
            
            java.util.List<?> achievementsList = getListValue(summaryData, "achievements");
            variables.put("achievements", achievementsList != null && !achievementsList.isEmpty()
                    ? formatListForTemplate(achievementsList)
                    : "<p><em>Achievements to be documented.</em></p>");
            
            java.util.List<?> lessonsList = getListValue(summaryData, "lessonsLearned");
            variables.put("lessonsLearned", lessonsList != null && !lessonsList.isEmpty()
                    ? formatListForTemplate(lessonsList)
                    : "<p><em>Lessons learned to be captured during retrospective.</em></p>");
            
            java.util.List<?> recommendationsList = getListValue(summaryData, "recommendations");
            variables.put("recommendations", recommendationsList != null && !recommendationsList.isEmpty()
                    ? formatListForTemplate(recommendationsList)
                    : "<p><em>Recommendations to be discussed in retrospective.</em></p>");
            
            variables.put("dataSources", getStringValue(summaryData, "dataSources", "SprintPilot System"));
        } else {
            // Set defaults if summaryData is null
            variables.put("blockers", "<p><em>No major blockers encountered during this sprint.</em></p>");
            variables.put("achievements", "<p><em>Achievements to be documented.</em></p>");
            variables.put("lessonsLearned", "<p><em>Lessons learned to be captured during retrospective.</em></p>");
            variables.put("recommendations", "<p><em>Recommendations to be discussed in retrospective.</em></p>");
            variables.put("dataSources", "SprintPilot System");
        }
        
        // Completion rate calculation
        int total = Integer.parseInt(variables.getOrDefault("totalStoryPoints", "0"));
        int completed = Integer.parseInt(variables.getOrDefault("completedStoryPoints", "0"));
        double completionRate = total > 0 ? (completed * 100.0 / total) : 0;
        variables.put("completionRate", String.format("%.1f", completionRate));
        
        // Team performance assessment
        variables.put("teamPerformance", assessTeamPerformance(completionRate));
        
        variables.put("generatedAt", java.time.LocalDateTime.now().toString());
        
        return variables;
    }
    
    // Helper methods for calculations and formatting
    private String calculateTotalCapacity(SprintDto sprintDto) {
        if (sprintDto.teamMembers() == null || sprintDto.teamMembers().isEmpty()) {
            return "0";
        }
        
        // Calculate total capacity: sum of all team members' daily capacity * sprint duration
        double totalCapacity = 0;
        int duration = sprintDto.duration() != null ? sprintDto.duration() : 0;
        
        for (var member : sprintDto.teamMembers()) {
            if (member.dailyCapacity() != null && member.active() != null && member.active()) {
                totalCapacity += member.dailyCapacity().doubleValue() * duration;
            }
        }
        
        return String.format("%.1f", totalCapacity);
    }
    
    private String calculateSprintCapacity(SprintDto sprintDto) {
        // Same as total capacity for now (can be adjusted for holidays/leaves)
        return calculateTotalCapacity(sprintDto);
    }
    
    private String calculateAverageVelocity(SprintDto sprintDto) {
        // For now, return planned story points as velocity
        // In future, can calculate from historical sprint data
        return calculatePlannedStoryPoints(sprintDto);
    }
    
    private String calculatePlannedStoryPoints(SprintDto sprintDto) {
        if (sprintDto.tasks() != null && !sprintDto.tasks().isEmpty()) {
            double total = sprintDto.tasks().stream()
                    .mapToDouble(t -> t.storyPoints() != null ? t.storyPoints().doubleValue() : 0)
                    .sum();
            return String.format("%.1f", total);
        }
        return "0";
    }
    
    private Map<String, String> calculateCategoryBreakdown(SprintDto sprintDto) {
        Map<String, String> breakdown = new HashMap<>();
        
        // Initialize with defaults
        breakdown.put("featureStoryPoints", "0");
        breakdown.put("featurePercentage", "0");
        breakdown.put("techDebtStoryPoints", "0");
        breakdown.put("techDebtPercentage", "0");
        breakdown.put("prodIssueStoryPoints", "0");
        breakdown.put("prodIssuePercentage", "0");
        
        if (sprintDto.id() == null) {
            return breakdown;
        }
        
        // Calculate story points by category (for display)
        double featureSP = 0, techDebtSP = 0, prodIssueSP = 0;
        
        if (sprintDto.tasks() != null && !sprintDto.tasks().isEmpty()) {
            for (var task : sprintDto.tasks()) {
                double sp = task.storyPoints() != null ? task.storyPoints().doubleValue() : 0;
                if (task.category() != null) {
                    switch (task.category()) {
                        case FEATURE -> featureSP += sp;
                        case TECH_DEBT -> techDebtSP += sp;
                        case PROD_ISSUE -> prodIssueSP += sp;
                        default -> {}
                    }
                }
            }
        }
        
        breakdown.put("featureStoryPoints", String.format("%.1f", featureSP));
        breakdown.put("techDebtStoryPoints", String.format("%.1f", techDebtSP));
        breakdown.put("prodIssueStoryPoints", String.format("%.1f", prodIssueSP));
        
        // Get work distribution percentages from SprintMetricsService (based on task counts)
        try {
            WorkDistributionDto workDistribution = sprintMetricsService.getWorkDistribution(
                sprintDto.id(), 
                null // projectName is optional
            );
            
            if (workDistribution != null && workDistribution.getLabels() != null 
                && workDistribution.getPercentages() != null) {
                
                // Map the work distribution percentages to our categories
                for (int i = 0; i < workDistribution.getLabels().size(); i++) {
                    String label = workDistribution.getLabels().get(i);
                    java.math.BigDecimal percentage = workDistribution.getPercentages().get(i);
                    
                    switch (label) {
                        case "Features" -> breakdown.put("featurePercentage", 
                            String.format("%.1f", percentage.doubleValue()));
                        case "Tech Debt" -> breakdown.put("techDebtPercentage", 
                            String.format("%.1f", percentage.doubleValue()));
                        case "Bug Fixes" -> breakdown.put("prodIssuePercentage", 
                            String.format("%.1f", percentage.doubleValue()));
                        // "Support" category is not displayed in the template, so we skip it
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get work distribution for sprint {}: {}", sprintDto.id(), e.getMessage());
            // If work distribution fails, calculate percentages based on story points as fallback
            double totalSP = featureSP + techDebtSP + prodIssueSP;
            if (totalSP > 0) {
                breakdown.put("featurePercentage", String.format("%.1f", (featureSP * 100 / totalSP)));
                breakdown.put("techDebtPercentage", String.format("%.1f", (techDebtSP * 100 / totalSP)));
                breakdown.put("prodIssuePercentage", String.format("%.1f", (prodIssueSP * 100 / totalSP)));
            }
        }
        
        return breakdown;
    }
    
    private String formatTasksForTemplate(java.util.List<?> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return "<p><em>No tasks available.</em></p>";
        }
        
        StringBuilder html = new StringBuilder();
        html.append("<table><thead><tr>");
        html.append("<th>Task Key</th><th>Summary</th><th>Story Points</th>");
        html.append("<th>Category</th><th>Priority</th><th>Status</th>");
        html.append("</tr></thead><tbody>");
        
        for (Object taskObj : tasks) {
            if (taskObj instanceof com.sprintpilot.dto.TaskDto task) {
                html.append("<tr>");
                html.append("<td>").append(escapeHtml(task.taskKey())).append("</td>");
                html.append("<td>").append(escapeHtml(task.summary())).append("</td>");
                html.append("<td>").append(task.storyPoints() != null ? task.storyPoints() : "0").append(" SP</td>");
                html.append("<td>").append(task.category() != null ? task.category().toString() : "").append("</td>");
                html.append("<td>").append(task.priority() != null ? task.priority().toString() : "").append("</td>");
                html.append("<td>").append(task.status() != null ? task.status().toString() : "").append("</td>");
                html.append("</tr>");
            }
        }
        
        html.append("</tbody></table>");
        return html.toString();
    }
    
    private String formatEventsForTemplate(java.util.List<?> events) {
        if (events == null || events.isEmpty()) {
            return "";
        }
        
        StringBuilder html = new StringBuilder();
        html.append("<ul>");
        
        for (Object eventObj : events) {
            if (eventObj instanceof com.sprintpilot.dto.SprintEventDto event) {
                html.append("<li><strong>").append(event.eventDate() != null ? event.eventDate().toString() : "").append(":</strong> ");
                html.append(event.eventType() != null ? event.eventType().toString() : "").append(" - ");
                html.append(escapeHtml(event.name()));
                if (event.eventTime() != null) {
                    html.append(" at ").append(event.eventTime().toString());
                }
                html.append("</li>");
            }
        }
        
        html.append("</ul>");
        return html.toString();
    }
    
    private String formatListForTemplate(java.util.List<?> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        
        StringBuilder html = new StringBuilder();
        html.append("<ul>");
        
        for (Object item : items) {
            html.append("<li>").append(escapeHtml(item.toString())).append("</li>");
        }
        
        html.append("</ul>");
        return html.toString();
    }
    
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
    
    /**
     * Formats a Confluence user mention using @username format
     * Simple approach: just prefix the username with @
     * 
     * @param username User's name/username
     * @return Formatted mention with @ prefix
     */
    private String formatConfluenceUserMention(String username) {
        if (username == null || username.trim().isEmpty()) {
            return "<strong>Unknown User</strong>";
        }
        
        // Simple @username format - Confluence will try to resolve it
        return "<strong>@" + escapeHtml(username.trim()) + "</strong>";
    }
    
    private String assessTeamPerformance(double completionRate) {
        if (completionRate >= 90) return "Excellent";
        if (completionRate >= 75) return "Good";
        if (completionRate >= 50) return "Average";
        return "Needs Improvement";
    }
    
    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map != null ? map.get(key) : null;
        return value != null ? value.toString() : defaultValue;
    }
    
    private java.util.List<?> getListValue(Map<String, Object> map, String key) {
        Object value = map != null ? map.get(key) : null;
        return value instanceof java.util.List ? (java.util.List<?>) value : null;
    }
}

