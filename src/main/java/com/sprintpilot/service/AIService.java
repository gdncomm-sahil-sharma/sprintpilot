package com.sprintpilot.service;

import com.sprintpilot.dto.*;
import java.util.List;
import java.util.Map;

public interface AIService {
    
    String generateSprintSummary(List<TeamMemberDto> team, SprintDto sprint, 
                                 List<TaskDto> tasks, List<CapacitySummaryDto> workload);
    
    String generateMeetingInvite(SprintEventDto meeting, SprintDto sprint);
    
    String generateRiskSummary(List<TaskDto> tasks, List<TaskRiskDto> risks);
    
    String generateConfluencePage(List<TeamMemberDto> team, SprintDto sprint, 
                                 List<TaskDto> tasks, List<CapacitySummaryDto> workload);
    
    String generateTeamsMessage(List<TeamMemberDto> team, SprintDto sprint, 
                               List<TaskDto> tasks, List<CapacitySummaryDto> workload);
    
    String generateOutlookBody(List<TeamMemberDto> team, SprintDto sprint, 
                              List<TaskDto> tasks, List<CapacitySummaryDto> workload);
    
    String analyzeHistoricalPerformance(List<SprintDto> sprints, 
                                        List<Map<String, Object>> velocityTrend,
                                        List<Map<String, Object>> workMixTrend,
                                        List<Map<String, Object>> roleUtilization);
    
    Map<String, String> generateMeetingDetails(SprintEventDto meeting, SprintDto sprint);
}
