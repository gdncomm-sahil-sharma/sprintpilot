package com.sprintpilot.service.impl;

import com.sprintpilot.dto.*;
import com.sprintpilot.service.AIService;
// Spring AI imports commented out until we have proper dependencies
// import org.springframework.ai.chat.ChatClient;
// import org.springframework.ai.chat.ChatResponse;
// import org.springframework.ai.chat.messages.SystemMessage;
// import org.springframework.ai.chat.messages.UserMessage;
// import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "app.ai.mock-mode", havingValue = "false", matchIfMissing = true)
public class GeminiAIService implements AIService {
    
    // ChatClient commented out until we have Spring AI dependencies
    // @Autowired
    // private ChatClient chatClient;
    
    @Value("${app.ai.enabled:true}")
    private boolean aiEnabled;
    
    @Override
    public String generateSprintSummary(List<TeamMemberDto> team, SprintDto sprint, 
                                       List<TaskDto> tasks, List<CapacitySummaryDto> workload) {
        if (!aiEnabled) {
            return "AI features are disabled";
        }
        
        String prompt = buildSprintDataPrompt(team, sprint, tasks, workload) + "\n\n" +
                       "Generate a concise sprint summary for an engineering manager including:\n" +
                       "- Primary Focus\n" +
                       "- Workload Balance\n" +
                       "- Potential Risks\n" +
                       "- Overall Assessment\n" +
                       "Use markdown bullet points.";
        
        return callAI(prompt);
    }
    
    @Override
    public String generateMeetingInvite(SprintEventDto meeting, SprintDto sprint) {
        if (!aiEnabled) {
            return "AI features are disabled";
        }
        
        String meetingType = meeting.eventSubtype() != null ? 
                           meeting.eventSubtype().toString() : "Sprint Meeting";
        
        String prompt = String.format(
            "Generate a professional meeting invite for a %s meeting.\n" +
            "Meeting Date: %s\n" +
            "Meeting Time: %s\n" +
            "Duration: %d minutes\n" +
            "Sprint: %s to %s\n\n" +
            "Include:\n" +
            "- Subject line\n" +
            "- Meeting purpose\n" +
            "- Agenda items\n" +
            "- Professional closing",
            meetingType, meeting.eventDate(), meeting.eventTime(), 
            meeting.durationMinutes(), sprint.startDate(), sprint.endDate()
        );
        
        return callAI(prompt);
    }
    
    @Override
    public String generateRiskSummary(List<TaskDto> tasks, List<TaskRiskDto> risks) {
        if (!aiEnabled) {
            return "AI features are disabled";
        }
        
        Map<String, TaskRiskDto> riskMap = risks.stream()
            .collect(Collectors.toMap(TaskRiskDto::taskId, r -> r));
        
        StringBuilder taskDetails = new StringBuilder();
        for (TaskDto task : tasks) {
            TaskRiskDto risk = riskMap.get(task.id());
            taskDetails.append(String.format("- %s: %s (%s points) - Risk: %s - Reason: %s\n",
                task.taskKey(), task.summary(), task.storyPoints(),
                risk != null ? risk.riskLevel().getDisplayName() : "Unknown",
                risk != null ? risk.reason() : "Not analyzed"
            ));
        }
        
        String prompt = "Analyze these sprint tasks and their risk levels:\n\n" +
                       taskDetails + "\n\n" +
                       "Provide:\n" +
                       "- Summary of critical issues\n" +
                       "- Pattern analysis\n" +
                       "- Actionable recommendations\n" +
                       "Use markdown formatting.";
        
        return callAI(prompt);
    }
    
    @Override
    public String generateConfluencePage(List<TeamMemberDto> team, SprintDto sprint, 
                                        List<TaskDto> tasks, List<CapacitySummaryDto> workload) {
        if (!aiEnabled) {
            return "AI features are disabled";
        }
        
        String prompt = buildSprintDataPrompt(team, sprint, tasks, workload) + "\n\n" +
                       "Generate a Confluence page using wiki markup with:\n" +
                       "h1. Sprint Plan title\n" +
                       "h2. Sprint Goals section\n" +
                       "h2. Team Capacity section\n" +
                       "h2. Work Items section\n" +
                       "Use proper Confluence formatting.";
        
        return callAI(prompt);
    }
    
    @Override
    public String generateTeamsMessage(List<TeamMemberDto> team, SprintDto sprint, 
                                      List<TaskDto> tasks, List<CapacitySummaryDto> workload) {
        if (!aiEnabled) {
            return "AI features are disabled";
        }
        
        String prompt = buildSprintDataPrompt(team, sprint, tasks, workload) + "\n\n" +
                       "Generate a Microsoft Teams announcement for sprint kick-off:\n" +
                       "- Cheerful opening\n" +
                       "- Sprint duration\n" +
                       "- Key focus areas (2-3 bullet points)\n" +
                       "- Motivating closing\n" +
                       "Use Teams markdown formatting.";
        
        return callAI(prompt);
    }
    
    @Override
    public String generateOutlookBody(List<TeamMemberDto> team, SprintDto sprint, 
                                     List<TaskDto> tasks, List<CapacitySummaryDto> workload) {
        if (!aiEnabled) {
            return "AI features are disabled";
        }
        
        String prompt = buildSprintDataPrompt(team, sprint, tasks, workload) + "\n\n" +
                       "Generate an Outlook meeting invite body for sprint planning:\n" +
                       "- Clear subject line\n" +
                       "- Brief introduction\n" +
                       "- Meeting agenda\n" +
                       "- Professional closing";
        
        return callAI(prompt);
    }
    
    @Override
    public String analyzeHistoricalPerformance(List<SprintDto> sprints, 
                                              List<Map<String, Object>> velocityTrend,
                                              List<Map<String, Object>> workMixTrend,
                                              List<Map<String, Object>> roleUtilization) {
        if (!aiEnabled) {
            return "AI features are disabled";
        }
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze historical sprint performance:\n\n");
        prompt.append("Number of sprints: ").append(sprints.size()).append("\n");
        
        if (!sprints.isEmpty()) {
            prompt.append("Period: ").append(sprints.get(0).startDate())
                  .append(" to ").append(sprints.get(sprints.size()-1).endDate()).append("\n\n");
        }
        
        prompt.append("Velocity Trend:\n");
        velocityTrend.forEach(v -> 
            prompt.append("- Sprint ending ").append(v.get("endDate"))
                  .append(": ").append(v.get("totalHours")).append(" hours\n")
        );
        
        prompt.append("\nWork Mix Trend:\n");
        workMixTrend.forEach(w -> 
            prompt.append("- Sprint ending ").append(w.get("endDate"))
                  .append(": ").append(w.get("mix")).append("\n")
        );
        
        prompt.append("\nRole Utilization:\n");
        roleUtilization.forEach(r -> 
            prompt.append("- ").append(r.get("role"))
                  .append(": ").append(r.get("utilization")).append("%\n")
        );
        
        prompt.append("\nProvide actionable insights:\n");
        prompt.append("- Overall Performance\n");
        prompt.append("- Strategic Focus\n");
        prompt.append("- Team Health & Bottlenecks\n");
        prompt.append("- Concrete Recommendations\n");
        prompt.append("Use markdown formatting.");
        
        return callAI(prompt.toString());
    }
    
    @Override
    public Map<String, String> generateMeetingDetails(SprintEventDto meeting, SprintDto sprint) {
        if (!aiEnabled) {
            Map<String, String> result = new HashMap<>();
            result.put("subject", "Sprint Meeting");
            result.put("body", "Meeting details unavailable - AI is disabled");
            return result;
        }
        
        String response = generateMeetingInvite(meeting, sprint);
        
        // Parse the response to extract subject and body
        Map<String, String> result = new HashMap<>();
        String[] lines = response.split("\n");
        
        boolean inBody = false;
        StringBuilder body = new StringBuilder();
        
        for (String line : lines) {
            if (line.toLowerCase().startsWith("subject:")) {
                result.put("subject", line.substring(8).trim());
            } else if (!inBody && (line.toLowerCase().contains("dear") || 
                                   line.toLowerCase().contains("hi ") || 
                                   line.toLowerCase().contains("hello"))) {
                inBody = true;
                body.append(line).append("\n");
            } else if (inBody) {
                body.append(line).append("\n");
            }
        }
        
        result.put("body", body.toString().trim());
        
        if (!result.containsKey("subject")) {
            result.put("subject", "Sprint " + (meeting.eventSubtype() != null ? 
                      meeting.eventSubtype().toString() : "Meeting"));
        }
        
        return result;
    }
    
    private String buildSprintDataPrompt(List<TeamMemberDto> team, SprintDto sprint,
                                        List<TaskDto> tasks, List<CapacitySummaryDto> workload) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Sprint Details:\n");
        prompt.append("- Start Date: ").append(sprint.startDate()).append("\n");
        prompt.append("- End Date: ").append(sprint.endDate()).append("\n");
        prompt.append("- Duration: ").append(sprint.duration()).append(" working days\n\n");
        
        prompt.append("Team Members:\n");
        team.forEach(m -> 
            prompt.append("- ").append(m.name()).append(" (").append(m.role()).append(")\n")
        );
        
        prompt.append("\nSprint Tasks:\n");
        tasks.forEach(t -> 
            prompt.append("- ").append(t.taskKey()).append(": ")
                  .append(t.summary()).append(" (").append(t.storyPoints()).append("h)\n")
        );
        
        prompt.append("\nWorkload Allocation:\n");
        workload.forEach(w -> 
            prompt.append("- ").append(w.memberName())
                  .append(": Capacity ").append(w.totalCapacity()).append("h")
                  .append(", Assigned ").append(w.assignedHours()).append("h")
                  .append(", Status: ").append(w.status()).append("\n")
        );
        
        return prompt.toString();
    }
    
    private String callAI(String prompt) {
        // Temporary mock implementation until Spring AI dependencies are resolved
        return generateMockResponse(prompt);
    }
    
    private String generateMockResponse(String prompt) {
        if (prompt.toLowerCase().contains("sprint summary")) {
            return "**Sprint Summary (Mock Response)**\n\n" +
                   "- **Primary Focus:** This sprint focuses on feature development and technical improvements.\n" +
                   "- **Workload Balance:** Team capacity is well-balanced with 85% utilization.\n" +
                   "- **Potential Risks:** One team member is slightly overloaded, consider redistribution.\n" +
                   "- **Overall Assessment:** Sprint is well-planned with achievable goals.";
        } else if (prompt.toLowerCase().contains("risk")) {
            return "**Risk Analysis (Mock Response)**\n\n" +
                   "- 2 tasks are **Off Track** and need immediate attention\n" +
                   "- 3 tasks are **At Risk** due to dependencies\n" +
                   "- Remaining tasks are **On Track**\n" +
                   "- Recommend daily check-ins for at-risk items";
        } else if (prompt.toLowerCase().contains("meeting") || prompt.toLowerCase().contains("invite")) {
            return "Subject: Sprint Planning Meeting\n\n" +
                   "Dear Team,\n\n" +
                   "This is an invitation for our upcoming sprint planning session.\n\n" +
                   "Agenda:\n" +
                   "- Review previous sprint\n" +
                   "- Discuss sprint goals\n" +
                   "- Estimate and assign tasks\n" +
                   "- Finalize sprint backlog\n\n" +
                   "Looking forward to a productive session.\n\n" +
                   "Best regards";
        }
        
        return "Mock AI response for: " + prompt.substring(0, Math.min(prompt.length(), 100)) + "...";
    }
}
