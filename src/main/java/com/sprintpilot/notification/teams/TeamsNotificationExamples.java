package com.sprintpilot.notification.teams;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Example usage of TeamsNotificationService
 * This class demonstrates how to use the service in different scenarios
 */
@Component
public class TeamsNotificationExamples {
    
    private final TeamsNotificationService teamsNotificationService;
    
    public TeamsNotificationExamples(TeamsNotificationService teamsNotificationService) {
        this.teamsNotificationService = teamsNotificationService;
    }
    
    /**
     * Example: Send notification when a holiday is created
     */
    public void notifyHolidayCreated(String holidayName, String holidayDate, 
                                     String holidayType, String locations, boolean recurring) {
        Map<String, String> variables = new HashMap<>();
        variables.put("holidayName", holidayName);
        variables.put("holidayDate", holidayDate);
        variables.put("holidayType", holidayType);
        variables.put("locations", locations);
        variables.put("recurring", recurring ? "Yes" : "No");
        
        teamsNotificationService.sendNotification("holiday-created.json", variables);
    }
    
    /**
     * Example: Send notification when a sprint is created
     */
    public void notifySprintCreated(String sprintId, String startDate, 
                                   String endDate, int duration, String status) {
        Map<String, String> variables = new HashMap<>();
        variables.put("sprintId", sprintId);
        variables.put("startDate", startDate);
        variables.put("endDate", endDate);
        variables.put("duration", String.valueOf(duration));
        variables.put("status", status);
        
        teamsNotificationService.sendNotification("sprint-created.json", variables);
    }
    
    /**
     * Example: Send notification when a sprint is completed
     */
    public void notifySprintCompleted(String sprintId, String completionDate,
                                      String totalStoryPoints, String completedStoryPoints,
                                      int teamSize, String velocity) {
        Map<String, String> variables = new HashMap<>();
        variables.put("sprintId", sprintId);
        variables.put("completionDate", completionDate);
        variables.put("totalStoryPoints", totalStoryPoints);
        variables.put("completedStoryPoints", completedStoryPoints);
        variables.put("teamSize", String.valueOf(teamSize));
        variables.put("velocity", velocity);
        
        teamsNotificationService.sendNotification("sprint-completed.json", variables);
    }
    
    /**
     * Example: Send notification when a task is assigned
     */
    public void notifyTaskAssigned(String taskKey, String summary, String assignedTo,
                                   String storyPoints, String priority, String sprintId) {
        Map<String, String> variables = new HashMap<>();
        variables.put("taskKey", taskKey);
        variables.put("summary", summary);
        variables.put("assignedTo", assignedTo);
        variables.put("storyPoints", storyPoints);
        variables.put("priority", priority);
        variables.put("sprintId", sprintId);
        
        teamsNotificationService.sendNotification("task-assigned.json", variables);
    }
}

