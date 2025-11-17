package com.sprintpilot.service.impl;

import com.sprintpilot.dto.CapacitySummaryDto;
import com.sprintpilot.dto.SprintDto;
import com.sprintpilot.dto.SprintEventDto;
import com.sprintpilot.dto.TaskDto;
import com.sprintpilot.dto.TaskRiskDto;
import com.sprintpilot.dto.TeamMemberDto;
import com.sprintpilot.service.AIService;
import io.github.bucket4j.Bucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Real implementation of AIService using Spring AI and Google Vertex AI Gemini
 * This service provides AI-powered features for sprint management including:
 * - Sprint summaries and insights
 * - Risk analysis
 * - Meeting invite generation
 * - Performance analytics
 * 
 * Features:
 * - Rate limiting to prevent API quota exhaustion
 * - Comprehensive error handling with fallback responses
 * - Structured prompt engineering for consistent results
 */
@Service
@ConditionalOnProperty(name = "app.ai.mock-mode", havingValue = "false")
public class GeminiAIService implements AIService {
    
    private static final Logger logger = LoggerFactory.getLogger(GeminiAIService.class);
    
    @Autowired
    private ChatModel chatModel;
    
    @Autowired
    private Bucket rateLimiterBucket;
    
    @Value("${app.ai.enabled:true}")
    private boolean aiEnabled;
    
    /**
     * Generates a comprehensive sprint summary using AI analysis
     * Includes workload balance, risks, and strategic recommendations
     */
    @Override
    public String generateSprintSummary(List<TeamMemberDto> team, SprintDto sprint, 
                                       List<TaskDto> tasks, List<CapacitySummaryDto> workload) {
        if (!aiEnabled) {
            return "AI features are disabled";
        }
        
        // Build the prompt with sprint data
        String prompt = buildSprintDataPrompt(team, sprint, tasks, workload) + "\n\n" +
                       """
                       Analyze the following sprint data and generate a concise summary for an engineering manager.
                       
                       Generate a summary as a readable, bulleted list. The summary should be easy to scan and highlight key information.
                       Include the following points using markdown bullet points:
                       - **Primary Focus:** Briefly describe the main goal or theme of the sprint based on the work items.
                       - **Workload Balance:** Comment on the team's capacity vs. assigned work. Mention any members who are overloaded or underutilized.
                       - **Potential Risks:** Identify any potential risks, such as a high workload on a specific team member or a large amount of unplanned work.
                       - **Overall Assessment:** Provide a brief, overall assessment of the sprint plan.
                       """;
        
        return callAI(prompt, "Sprint Summary");
    }
    
    /**
     * Generates a professional meeting invite with agenda and details
     */
    @Override
    public String generateMeetingInvite(SprintEventDto meeting, SprintDto sprint) {
        if (!aiEnabled) {
            return "AI features are disabled";
        }
        
        String meetingType = meeting.eventSubtype() != null ? 
                           meeting.eventSubtype().toString() : "Sprint Meeting";
        
        String prompt = String.format(
            """
            Generate a professional meeting invite for a %s meeting.
            
            Meeting Details:
            - Date: %s
            - Time: %s
            - Duration: %d minutes
            - Sprint Period: %s to %s
            
            Include:
            - Subject line starting with "Subject: "
            - Meeting purpose
            - Detailed agenda items
            - Professional closing
            
            Format the response with clear sections.
            """,
            meetingType, meeting.eventDate(), meeting.eventTime(), 
            meeting.durationMinutes(), sprint.startDate(), sprint.endDate()
        );
        
        return callAI(prompt, "Meeting Invite");
    }
    
    /**
     * Analyzes sprint tasks and generates risk summary with actionable insights
     */
    @Override
    public String generateRiskSummary(List<TaskDto> tasks, List<TaskRiskDto> risks) {
        if (!aiEnabled) {
            return "AI features are disabled";
        }
        
        // Map risks by task ID for efficient lookup
        Map<String, TaskRiskDto> riskMap = risks.stream()
            .collect(Collectors.toMap(TaskRiskDto::taskId, r -> r));
        
        // Build task details with risk information
        StringBuilder taskDetails = new StringBuilder();
        for (TaskDto task : tasks) {
            TaskRiskDto risk = riskMap.get(task.id());
            taskDetails.append(String.format("- %s: %s (%s points) - Risk: %s - Reason: %s\n",
                task.taskKey(), task.summary(), task.storyPoints(),
                risk != null ? risk.riskLevel().getDisplayName() : "Unknown",
                risk != null ? risk.reason() : "Not analyzed"
            ));
        }
        
        String prompt = String.format(
            """
            You are an expert project manager. Analyze the following sprint tasks and their risk levels.
            Provide a high-level summary for an engineering manager.
            
            Tasks and Risks:
            %s
            
            Your analysis should include:
            - **Critical Issues:** Point out the most severe blockers ('Off Track' items)
            - **Pattern Analysis:** Identify bottlenecks or patterns in 'At Risk' items (e.g., multiple at-risk tasks assigned to one person)
            - **Overall Health:** Brief sprint health assessment
            - **Actionable Recommendations:** Specific steps to mitigate risks
            
            Use clear, concise markdown bullet points.
            """,
            taskDetails
        );
        
        return callAI(prompt, "Risk Summary");
    }
    
    /**
     * Generates Confluence page content with wiki markup formatting
     */
    @Override
    public String generateConfluencePage(List<TeamMemberDto> team, SprintDto sprint, 
                                        List<TaskDto> tasks, List<CapacitySummaryDto> workload) {
        if (!aiEnabled) {
            return "AI features are disabled";
        }
        
        String prompt = buildSprintDataPrompt(team, sprint, tasks, workload) + "\n\n" +
                       """
                       Based on the sprint data above, generate a Confluence page content using Confluence wiki markup.
                       
                       Create a well-structured Confluence page with these sections:
                       1. A main title (h1.) for the sprint plan including the dates
                       2. A "Sprint Goals" section (h2.) with 2-3 plausible, high-level goals
                       3. A "Team Capacity" section (h2.) with a table showing team members and their capacity
                       4. A "Work Items" section (h2.) listing all tasks using bullet points (*)
                       5. A "Dependencies and Risks" section (h2.) if applicable
                       
                       Use proper Confluence wiki markup syntax.
                       """;
        
        return callAI(prompt, "Confluence Page");
    }
    
    /**
     * Generates an engaging Microsoft Teams announcement message
     */
    @Override
    public String generateTeamsMessage(List<TeamMemberDto> team, SprintDto sprint, 
                                      List<TaskDto> tasks, List<CapacitySummaryDto> workload) {
        if (!aiEnabled) {
            return "AI features are disabled";
        }
        
        String prompt = buildSprintDataPrompt(team, sprint, tasks, workload) + "\n\n" +
                       """
                       Generate a Microsoft Teams announcement message for a sprint kick-off.
                       
                       The message should:
                       - Start with a cheerful kick-off message with an emoji
                       - State the sprint duration clearly
                       - List 2-3 key focus areas or goals as bullet points
                       - Include sprint statistics (if relevant)
                       - End with a motivating closing sentence
                       
                       Use Teams markdown formatting (e.g., **bold**, bullet points).
                       Keep it concise and energetic!
                       """;
        
        return callAI(prompt, "Teams Message");
    }
    
    /**
     * Generates Outlook meeting invite body text
     */
    @Override
    public String generateOutlookBody(List<TeamMemberDto> team, SprintDto sprint, 
                                     List<TaskDto> tasks, List<CapacitySummaryDto> workload) {
        if (!aiEnabled) {
            return "AI features are disabled";
        }
        
        String prompt = buildSprintDataPrompt(team, sprint, tasks, workload) + "\n\n" +
                       """
                       Generate the body text for an Outlook meeting invite for the sprint planning session.
                       
                       Include:
                       - A clear subject line like "Subject: Sprint Planning: [Start Date] - [End Date]"
                       - Brief introductory sentence
                       - Detailed meeting agenda with time allocations
                       - What participants should prepare beforehand
                       - Professional closing
                       
                       Format professionally for a corporate email.
                       """;
        
        return callAI(prompt, "Outlook Body");
    }
    
    /**
     * Analyzes historical sprint performance and provides actionable insights
     */
    @Override
    public String analyzeHistoricalPerformance(List<SprintDto> sprints, 
                                              List<Map<String, Object>> velocityTrend,
                                              List<Map<String, Object>> workMixTrend,
                                              List<Map<String, Object>> roleUtilization) {
        if (!aiEnabled) {
            return "AI features are disabled";
        }
        
        // Build comprehensive performance data
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an experienced Agile Coach and Engineering Manager. Analyze the following historical sprint data.\n\n");
        prompt.append("**Data Overview:**\n");
        prompt.append("- Number of sprints analyzed: ").append(sprints.size()).append("\n");
        
        if (!sprints.isEmpty()) {
            prompt.append("- Time period: ").append(sprints.get(0).startDate())
                  .append(" to ").append(sprints.get(sprints.size()-1).endDate()).append("\n\n");
        }
        
        prompt.append("**Team Velocity Trend (Total Hours Delivered Per Sprint):**\n");
        velocityTrend.forEach(v -> 
            prompt.append("- Sprint ending ").append(v.get("endDate"))
                  .append(": ").append(v.get("totalHours")).append(" hours\n")
        );
        
        prompt.append("\n**Work Mix Trend (% of work by category per sprint):**\n");
        workMixTrend.forEach(w -> 
            prompt.append("- Sprint ending ").append(w.get("endDate"))
                  .append(": ").append(w.get("mix")).append("\n")
        );
        
        prompt.append("\n**Average Role Utilization (across all selected sprints):**\n");
        roleUtilization.forEach(r -> 
            prompt.append("- ").append(r.get("role"))
                  .append(": ").append(r.get("utilization")).append("%\n")
        );
        
        prompt.append("""
            
            **Your Task:**
            Provide actionable insights based on these trends for an engineering manager. Use markdown bullet points.
            - **Overall Performance:** Summarize velocity and delivery consistency. Is it improving, declining, or erratic?
            - **Strategic Focus:** Comment on work mix. Is there a healthy balance between features, tech debt, and production issues?
            - **Team Health & Bottlenecks:** Identify roles that are consistently overloaded (>100%) or underutilized (<70%). What are the risks?
            - **Recommendations:** Suggest 1-2 concrete, actionable steps to address issues and improve in the next sprint.
            """);
        
        return callAI(prompt.toString(), "Performance Insights");
    }
    
    /**
     * Generates meeting details with separate subject and body
     */
    @Override
    public Map<String, String> generateMeetingDetails(SprintEventDto meeting, SprintDto sprint) {
        if (!aiEnabled) {
            Map<String, String> result = new HashMap<>();
            result.put("subject", "Sprint Meeting");
            result.put("body", "Meeting details unavailable - AI is disabled");
            return result;
        }
        
        // Generate the meeting invite
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
                                   line.toLowerCase().contains("hello") ||
                                   line.toLowerCase().contains("team"))) {
                inBody = true;
                body.append(line).append("\n");
            } else if (inBody) {
                body.append(line).append("\n");
            }
        }
        
        result.put("body", body.toString().trim());
        
        // Fallback subject if not found
        if (!result.containsKey("subject")) {
            result.put("subject", "Sprint " + (meeting.eventSubtype() != null ? 
                      meeting.eventSubtype().toString() : "Meeting"));
        }
        
        return result;
    }
    
    /**
     * Builds a structured prompt with sprint data for AI analysis
     */
    private String buildSprintDataPrompt(List<TeamMemberDto> team, SprintDto sprint,
                                        List<TaskDto> tasks, List<CapacitySummaryDto> workload) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("**Sprint Details:**\n");
        prompt.append("- Start Date: ").append(sprint.startDate()).append("\n");
        prompt.append("- End Date: ").append(sprint.endDate()).append("\n");
        prompt.append("- Duration: ").append(sprint.duration()).append(" working days\n\n");
        
        prompt.append("**Team Members:**\n");
        team.forEach(m -> 
            prompt.append("- ").append(m.name()).append(" (").append(m.role()).append(")\n")
        );
        
        prompt.append("\n**Sprint Tasks:**\n");
        tasks.forEach(t -> 
            prompt.append("- ").append(t.taskKey()).append(": ")
                  .append(t.summary()).append(" (").append(t.storyPoints()).append("h)\n")
        );
        
        prompt.append("\n**Workload Allocation:**\n");
        workload.forEach(w -> 
            prompt.append("- ").append(w.memberName())
                  .append(": Capacity ").append(w.totalCapacity()).append("h")
                  .append(", Assigned ").append(w.assignedHours()).append("h")
                  .append(", Status: ").append(w.status()).append("\n")
        );
        
        return prompt.toString();
    }
    
    /**
     * Calls the Vertex AI Gemini API with rate limiting and error handling
     * 
     * @param promptText The prompt to send to the AI
     * @param operationType Description of the operation for logging
     * @return AI-generated response or error message
     */
    private String callAI(String promptText, String operationType) {
        try {
            // Check rate limit before making API call
            if (!rateLimiterBucket.tryConsume(1)) {
                logger.warn("Rate limit exceeded for {} operation", operationType);
                return String.format(
                    "**Rate Limit Exceeded**\n\n" +
                    "Too many AI requests. Please try again in a moment.\n" +
                    "Operation: %s", 
                    operationType
                );
            }
            
            logger.debug("Calling Vertex AI Gemini for {} operation", operationType);
            
            // Call the AI using Spring AI ChatModel
            Prompt prompt = new Prompt(promptText);
            ChatResponse response = chatModel.call(prompt);
            
            String result = response.getResult().getOutput().getText();
            
            logger.debug("Successfully received AI response for {}", operationType);
            return result;
            
        } catch (Exception e) {
            logger.error("Error calling Vertex AI Gemini for {} operation: {}", operationType, e.getMessage(), e);
            return String.format(
                "**AI Service Error**\n\n" +
                "An error occurred while generating the %s. Please try again.\n" +
                "Error details: %s",
                operationType,
                e.getMessage()
            );
        }
    }
}
