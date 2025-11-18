package com.sprintpilot.service.impl;

import com.sprintpilot.dto.SprintDto;
import com.sprintpilot.dto.TaskDto;
import com.sprintpilot.dto.TeamMemberDto;
import com.sprintpilot.service.SprintService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for performance insights generation
 * Contains shared logic for calculating velocity trends, work mix, and role utilization
 */
@Component
public class PerformanceInsightsHelper {
    
    private final SprintService sprintService;
    
    public PerformanceInsightsHelper(SprintService sprintService) {
        this.sprintService = sprintService;
    }
    
    /**
     * Fetch archived sprints and calculate performance metrics
     * 
     * @return PerformanceData containing sprints and calculated metrics, or null if no sprints found
     */
    public PerformanceData preparePerformanceData() {
        // Fetch archived sprints from database
        List<SprintDto> completedSprints = sprintService.getArchivedSprints();
        
        if (completedSprints.isEmpty()) {
            return null;
        }
        
        // Calculate velocity trend
        List<Map<String, Object>> velocityTrend = calculateVelocityTrend(completedSprints);
        
        // Calculate work mix trend
        List<Map<String, Object>> workMixTrend = calculateWorkMixTrend(completedSprints);
        
        // Calculate role utilization
        List<Map<String, Object>> roleUtilization = calculateRoleUtilization(completedSprints);
        
        return new PerformanceData(completedSprints, velocityTrend, workMixTrend, roleUtilization);
    }
    
    /**
     * Calculate velocity trend (total hours delivered per sprint)
     */
    private List<Map<String, Object>> calculateVelocityTrend(List<SprintDto> sprints) {
        List<Map<String, Object>> velocityTrend = new ArrayList<>();
        
        for (SprintDto sprint : sprints) {
            double totalHours = sprint.tasks().stream()
                .map(TaskDto::storyPoints)
                .filter(sp -> sp != null)
                .mapToDouble(BigDecimal::doubleValue)
                .sum();
            
            Map<String, Object> velocity = new HashMap<>();
            velocity.put("sprintId", sprint.id());
            velocity.put("endDate", sprint.endDate());
            velocity.put("totalHours", totalHours);
            
            velocityTrend.add(velocity);
        }
        
        return velocityTrend;
    }
    
    /**
     * Calculate work mix trend (percentage of work by category per sprint)
     */
    private List<Map<String, Object>> calculateWorkMixTrend(List<SprintDto> sprints) {
        List<Map<String, Object>> workMixTrend = new ArrayList<>();
        
        for (SprintDto sprint : sprints) {
            double totalHours = sprint.tasks().stream()
                .map(TaskDto::storyPoints)
                .filter(sp -> sp != null)
                .mapToDouble(BigDecimal::doubleValue)
                .sum();
            
            Map<String, Double> categoryHours = new HashMap<>();
            categoryHours.put("FEATURE", 0.0);
            categoryHours.put("TECH_DEBT", 0.0);
            categoryHours.put("PROD_ISSUE", 0.0);
            
            // Calculate hours per category
            for (TaskDto task : sprint.tasks()) {
                String category = task.category() != null ? task.category().name() : "OTHER";
                double storyPoints = task.storyPoints() != null ? task.storyPoints().doubleValue() : 0.0;
                categoryHours.put(category, 
                    categoryHours.getOrDefault(category, 0.0) + storyPoints);
            }
            
            // Calculate percentages
            Map<String, Object> mix = new HashMap<>();
            if (totalHours > 0) {
                Map<String, Double> mixPercentage = new HashMap<>();
                mixPercentage.put("FEATURE", (categoryHours.get("FEATURE") / totalHours) * 100);
                mixPercentage.put("TECH_DEBT", (categoryHours.get("TECH_DEBT") / totalHours) * 100);
                mixPercentage.put("PROD_ISSUE", (categoryHours.get("PROD_ISSUE") / totalHours) * 100);
                mix.put("mix", mixPercentage);
            } else {
                Map<String, Double> mixPercentage = new HashMap<>();
                mixPercentage.put("FEATURE", 0.0);
                mixPercentage.put("TECH_DEBT", 0.0);
                mixPercentage.put("PROD_ISSUE", 0.0);
                mix.put("mix", mixPercentage);
            }
            
            mix.put("sprintId", sprint.id());
            mix.put("endDate", sprint.endDate());
            
            workMixTrend.add(mix);
        }
        
        return workMixTrend;
    }
    
    /**
     * Calculate average role utilization across all sprints
     */
    private List<Map<String, Object>> calculateRoleUtilization(List<SprintDto> sprints) {
        Map<String, RoleData> roleDataMap = new HashMap<>();
        
        for (SprintDto sprint : sprints) {
            for (TeamMemberDto member : sprint.teamMembers()) {
                String role = member.role().name();
                
                // Calculate member's capacity for this sprint
                double capacity = calculateMemberCapacity(member, sprint);
                
                // Calculate assigned hours for this member
                double assignedHours = sprint.tasks().stream()
                    .filter(task -> member.id().equals(task.assigneeId()))
                    .map(TaskDto::storyPoints)
                    .filter(sp -> sp != null)
                    .mapToDouble(BigDecimal::doubleValue)
                    .sum();
                
                // Accumulate role data
                RoleData roleData = roleDataMap.getOrDefault(role, new RoleData());
                roleData.totalCapacity += capacity;
                roleData.assignedHours += assignedHours;
                roleDataMap.put(role, roleData);
            }
        }
        
        // Calculate utilization percentages
        List<Map<String, Object>> roleUtilization = new ArrayList<>();
        for (Map.Entry<String, RoleData> entry : roleDataMap.entrySet()) {
            Map<String, Object> utilization = new HashMap<>();
            utilization.put("role", entry.getKey());
            
            double utilizationPercent = entry.getValue().totalCapacity > 0 
                ? (entry.getValue().assignedHours / entry.getValue().totalCapacity) * 100 
                : 0;
            utilization.put("utilization", utilizationPercent);
            
            roleUtilization.add(utilization);
        }
        
        return roleUtilization;
    }
    
    /**
     * Calculate member capacity for a sprint
     * This is a simplified calculation - in reality, you'd need to account for
     * working days, holidays, and leave days
     */
    private double calculateMemberCapacity(TeamMemberDto member, SprintDto sprint) {
        // Simplified: assume sprint duration * daily capacity
        // In a real implementation, you'd calculate working days excluding weekends and holidays
        Integer duration = sprint.duration();
        if (duration == null) {
            duration = 10; // Default sprint duration
        }
        BigDecimal dailyCapacity = member.dailyCapacity();
        if (dailyCapacity == null) {
            dailyCapacity = BigDecimal.valueOf(6); // Default daily capacity
        }
        return duration * dailyCapacity.doubleValue();
    }
    
    /**
     * Helper class to accumulate role data
     */
    private static class RoleData {
        double totalCapacity = 0.0;
        double assignedHours = 0.0;
    }
    
    /**
     * Data class to hold sprints and calculated metrics for performance insights
     */
    public record PerformanceData(
        List<SprintDto> sprints,
        List<Map<String, Object>> velocityTrend,
        List<Map<String, Object>> workMixTrend,
        List<Map<String, Object>> roleUtilization
    ) {}
}

