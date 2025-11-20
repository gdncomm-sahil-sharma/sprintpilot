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
 * OpenAI implementation of AIService using Spring AI and OpenAI GPT models
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
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai")
public class OpenAIService implements AIService {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenAIService.class);
    
    @Autowired
    private ChatModel chatModel;
    
    @Autowired
    private Bucket rateLimiterBucket;
    
    @Autowired
    private RiskSummaryHelper riskSummaryHelper;
    
    @Autowired
    private PerformanceInsightsHelper performanceInsightsHelper;
    
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
                       - **Remember this is not a chat bot, this is a summarizer so act accordingly
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
        
        // Get meeting-specific agenda and description
        String agendaTemplate = getMeetingAgendaTemplate(meetingType, meeting.durationMinutes());
        String meetingPurpose = getMeetingPurpose(meetingType);
        
        String prompt = String.format(
            """
            Generate a professional meeting invite for a %s meeting.
            
            Meeting Details:
            - Date: %s
            - Time: %s
            - Duration: %d minutes
            - Sprint Period: %s to %s
            
            IMPORTANT FORMATTING REQUIREMENTS:
            1. Start with "Subject: [Meeting Type] - Sprint Period: [dates]" on its own line
            2. After the subject, add TWO blank lines
            3. Use PLAIN TEXT formatting only (NO markdown, NO asterisks, NO special symbols)
            4. For the body:
               - Start with "Dear Team,"
               - Add a clear meeting purpose: %s
               - Include "MEETING DETAILS:" section with Date, Time, Duration
               - Create an "AGENDA:" section header
               - Use this agenda: %s
               - Add "Please come prepared" note
               - End with "Best regards," and "Sprint Team"
            5. Use simple numbered format (1., 2., 3.) for agenda items
            6. Add blank lines between sections for readability
            
            
            Generate the invite following this exact structure.
            """,
            meetingType, meeting.eventDate(), meeting.eventTime(), 
            meeting.durationMinutes(), sprint.startDate(), sprint.endDate(),
            meetingPurpose, agendaTemplate
        );
        
        return callAI(prompt, "Meeting Invite");
    }
    
    /**
     * Get meeting purpose based on type
     */
    private String getMeetingPurpose(String meetingType) {
        return switch (meetingType.toUpperCase()) {
            case "PLANNING" -> 
                "You are invited to our Sprint Planning meeting where we will define the sprint goal, select backlog items, and commit to the sprint scope.";
            case "GROOMING" -> 
                "You are invited to our Backlog Grooming session where we will refine user stories, clarify requirements, and prepare items for upcoming sprints.";
            case "RETROSPECTIVE" -> 
                "You are invited to our Sprint Retrospective where we will reflect on our sprint, celebrate successes, and identify improvements for future sprints.";
            default -> 
                "You are invited to our team meeting to discuss sprint activities and progress.";
        };
    }
    
    /**
     * Get meeting-specific agenda template
     */
    private String getMeetingAgendaTemplate(String meetingType, Integer duration) {
        int totalMin = duration != null ? duration : 60;
        
        return switch (meetingType.toUpperCase()) {
            case "PLANNING" -> String.format(
                """
                1. Welcome & Review Sprint Goal (%d min)
                2. Review Team Capacity & Availability (%d min)
                3. Backlog Review & Story Selection (%d min)
                4. Story Estimation & Discussion (%d min)
                5. Sprint Commitment & Finalization (%d min)
                6. Questions & Next Steps (%d min)
                """,
                Math.max(5, totalMin * 5 / 100),
                Math.max(10, totalMin * 10 / 100),
                Math.max(30, totalMin * 40 / 100),
                Math.max(20, totalMin * 25 / 100),
                Math.max(10, totalMin * 15 / 100),
                Math.max(5, totalMin * 5 / 100)
            );
            
            case "GROOMING" -> String.format(
                """
                1. Review Upcoming User Stories (%d min)
                2. Clarify Requirements & Acceptance Criteria (%d min)
                3. Story Breakdown & Task Identification (%d min)
                4. Estimation & Complexity Discussion (%d min)
                5. Prioritization & Dependencies (%d min)
                6. Action Items & Follow-ups (%d min)
                """,
                Math.max(10, totalMin * 15 / 100),
                Math.max(15, totalMin * 25 / 100),
                Math.max(15, totalMin * 25 / 100),
                Math.max(10, totalMin * 20 / 100),
                Math.max(5, totalMin * 10 / 100),
                Math.max(5, totalMin * 5 / 100)
            );
            
            case "RETROSPECTIVE" -> String.format(
                """
                1. Welcome & Set the Stage (%d min)
                2. Review Sprint Metrics & Outcomes (%d min)
                3. What Went Well (Celebrations) (%d min)
                4. What Didn't Go Well (Challenges) (%d min)
                5. Action Items & Improvements (%d min)
                6. Closing & Appreciation (%d min)
                """,
                Math.max(5, totalMin * 5 / 100),
                Math.max(10, totalMin * 15 / 100),
                Math.max(15, totalMin * 25 / 100),
                Math.max(15, totalMin * 25 / 100),
                Math.max(15, totalMin * 25 / 100),
                Math.max(5, totalMin * 5 / 100)
            );
            
            default -> 
                """
                1. Welcome & Introductions
                2. Main Discussion Topics
                3. Action Items & Next Steps
                """;
        };
    }
    
    /**
     * Analyzes sprint tasks and generates risk summary with actionable insights
     * This method maintains backward compatibility by calling the overloaded version with empty assignee map
     */
    @Override
    public String generateRiskSummary(List<TaskDto> tasks, List<TaskRiskDto> risks) {
        return generateRiskSummary(tasks, risks, Map.of());
    }
    
    /**
     * Analyzes sprint tasks and generates risk summary with actionable insights, including assignee information
     * 
     * @param tasks List of tasks
     * @param risks List of risk assessments
     * @param taskAssignees Map of taskId -> list of assignee names
     * @return AI-generated risk summary
     */
    public String generateRiskSummary(List<TaskDto> tasks, List<TaskRiskDto> risks, Map<String, List<String>> taskAssignees) {
        if (!aiEnabled) {
            return "AI features are disabled";
        }
        
        // Map risks by task ID for efficient lookup
        Map<String, TaskRiskDto> riskMap = risks.stream()
            .collect(Collectors.toMap(TaskRiskDto::taskId, r -> r));
        
        // Build task details with risk information and assignee names
        StringBuilder taskDetails = new StringBuilder();
        for (TaskDto task : tasks) {
            TaskRiskDto risk = riskMap.get(task.id());
            List<String> assignees = taskAssignees.getOrDefault(task.id(), List.of("Unassigned"));
            String assigneeStr = String.join(", ", assignees);
            
            taskDetails.append(String.format("- %s: %s (%s points) - Risk: %s - Assigned to: %s - Reason: %s\n",
                task.taskKey(), task.summary(), task.storyPoints(),
                risk != null ? risk.riskLevel().getDisplayName() : "Unknown",
                assigneeStr,
                risk != null ? risk.reason() : "Not analyzed"
            ));
        }
        
        String prompt = String.format(
            """
            You are an expert project manager. Analyze the following sprint tasks and their risk levels.
            Provide a high-level summary for an engineering manager.
            
            Tasks and Risks:
            %s
            
            IMPORTANT FORMATTING REQUIREMENTS:
            - Section headings like "**Critical Issues:**" should NOT have a leading dash (they are headings, not list items)
            - Under each heading, list items should use a single dash. Do NOT add extra dashes or dots
            - For EVERY 'Off Track' task, you MUST include the task key, assignee name(s), and issue description
            - Format: "- [TASK-KEY] assigned to [ASSIGNEE NAME(S)]: [Issue description]" (one dash per item, no duplication)
            - For 'At Risk' tasks, also include assignee names when relevant
            - Always mention assignee names when identifying patterns (e.g., "Multiple tasks assigned to [Name] are at risk")
            
            Your analysis should include:
            - **Critical Issues:** List ALL 'Off Track' items. Each item should start with a single dash. Example output format:
              **Critical Issues:**
              - PROJ-105 assigned to John Doe: Database migration script failing - Immediate attention required
              - PROJ-108 assigned to Jane Smith: Production API timeout issues - Blocking downstream tasks
            - **Pattern Analysis:** Identify bottlenecks or patterns in 'At Risk' items. Always mention assignee names when multiple tasks are assigned to the same person (e.g., "John Doe has 3 at-risk tasks: PROJ-110, PROJ-112, PROJ-115")
            - **Overall Health:** Brief sprint health assessment
            - **Actionable Recommendations:** Specific steps to mitigate risks, considering task assignments and mentioning specific assignees when relevant
            - **Remember this is not a chat bot, this is a summarizer so act accordingly
            Use clear, concise markdown formatting. Section headings should be plain text (no dash), and list items under headings should have ONE dash each. Make sure that each bullet point should not be more than 2 lines.
            """,
            taskDetails
        );
        
        return callAI(prompt, "Risk Summary");
    }
    
    /**
     * Generate risk summary for a sprint by fetching tasks from database
     * This method handles the complete flow: fetch tasks, convert DTOs, and generate summary
     */
    @Override
    public String generateRiskSummaryForSprint(String sprintId) {
        if (!aiEnabled) {
            return "AI features are disabled";
        }
        
        logger.info("Generating risk summary for sprint: {}", sprintId);
        
        // Use helper to fetch and convert tasks
        RiskSummaryHelper.RiskSummaryData data = riskSummaryHelper.prepareRiskSummaryData(sprintId);
        
        if (data == null) {
            return "No tasks found for this sprint. Please import tasks first.";
        }
        
        // Call generateRiskSummary method with prepared data including assignees
        return generateRiskSummary(data.tasks(), data.risks(), data.taskAssignees());
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
            Provide actionable insights based on these trends for an engineering manager. 
            
            **IMPORTANT FORMATTING REQUIREMENTS:**
            1. Generate EXACTLY 3 sections with these specific headers (use ** for bold):
               - **Performance Strengths:** (or similar positive title like "Trending Up", "What's Working Well")
               - **Areas for Improvement:** (or similar warning title like "Watch Area", "Team Health & Bottlenecks")
               - **Strategic Recommendations:** (or similar optimization title like "Optimization", "Action Items")
            
            2. Each section should have 2-3 bullet points using markdown format (- bullet point)
            3. Each bullet point should be concise (max 2 lines)
            4. Focus on:
               - Performance Strengths: Velocity trends, completion rates, positive patterns
               - Areas for Improvement: Overloaded/underutilized roles, bottlenecks, risks
               - Strategic Recommendations: Concrete, actionable steps to improve
            5. - **Remember this is not a chat bot, this is a summarizer so act accordingly   
            
            Example format:
            **Performance Strengths:**
            - Team velocity has improved 15% over the last 3 sprints with consistent delivery patterns
            - High sprint completion rate (94%) demonstrates good planning
            
            **Areas for Improvement:**
            - Backend team at 95% utilization (burnout risk)
            - QA team underutilized at 70% (growth opportunity)
            
            **Strategic Recommendations:**
            - Add one junior backend developer to reduce load
            - Cross-train QA team member in test automation
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
     * Generate performance insights by fetching sprint history from database
     * This method handles the complete flow: fetch sprints, calculate metrics, and generate insights
     */
    @Override
    public String generatePerformanceInsightsFromHistory() {
        if (!aiEnabled) {
            return "AI features are disabled";
        }
        
        logger.info("Generating performance insights from sprint history");
        
        // Use helper to fetch sprints and calculate metrics
        PerformanceInsightsHelper.PerformanceData data = performanceInsightsHelper.preparePerformanceData();
        
        if (data == null) {
            return "No completed sprints found. Please complete at least one sprint to generate performance insights.";
        }
        
        // Call existing analyzeHistoricalPerformance method with prepared data
        return analyzeHistoricalPerformance(data.sprints(), data.velocityTrend(), data.workMixTrend(), data.roleUtilization());
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
     * Calls the OpenAI GPT API with rate limiting and error handling
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
            
            logger.debug("Calling OpenAI for {} operation", operationType);
            
            // Call the AI using Spring AI ChatModel
            Prompt prompt = new Prompt(promptText);
            ChatResponse response = chatModel.call(prompt);
            
            String result = response.getResult().getOutput().getText();
            
            logger.debug("Successfully received AI response for {}", operationType);
            return result;
            
        } catch (Exception e) {
            logger.error("Error calling OpenAI for {} operation: {}", operationType, e.getMessage(), e);
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
