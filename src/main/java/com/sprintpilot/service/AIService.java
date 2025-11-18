package com.sprintpilot.service;

import com.sprintpilot.dto.*;
import java.util.List;
import java.util.Map;

public interface AIService {
    
    String generateSprintSummary(List<TeamMemberDto> team, SprintDto sprint, 
                                 List<TaskDto> tasks, List<CapacitySummaryDto> workload);
    
    String generateMeetingInvite(SprintEventDto meeting, SprintDto sprint);
    
    String generateRiskSummary(List<TaskDto> tasks, List<TaskRiskDto> risks);
    
    /**
     * Generate risk summary for a sprint by fetching tasks from database
     * @param sprintId The sprint ID
     * @return AI-generated risk summary
     */
    String generateRiskSummaryForSprint(String sprintId);
    
    String generateTeamsMessage(List<TeamMemberDto> team, SprintDto sprint, 
                               List<TaskDto> tasks, List<CapacitySummaryDto> workload);
    
    String generateOutlookBody(List<TeamMemberDto> team, SprintDto sprint, 
                              List<TaskDto> tasks, List<CapacitySummaryDto> workload);
    
    String analyzeHistoricalPerformance(List<SprintDto> sprints, 
                                        List<Map<String, Object>> velocityTrend,
                                        List<Map<String, Object>> workMixTrend,
                                        List<Map<String, Object>> roleUtilization);
    
    /**
     * Generate performance insights by fetching sprint history from database
     * and calculating velocity trends, work mix, and role utilization
     * @return AI-generated performance insights
     */
    String generatePerformanceInsightsFromHistory();
    
    Map<String, String> generateMeetingDetails(SprintEventDto meeting, SprintDto sprint);
}
