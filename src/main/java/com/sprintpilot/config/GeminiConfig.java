package com.sprintpilot.config;

// Spring AI imports commented out until we have proper dependencies
// import org.springframework.ai.chat.ChatClient;
// import org.springframework.ai.chat.ChatModel;
// import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
// import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Configuration
public class GeminiConfig {
    
    @Value("${spring.ai.vertex.ai.gemini.api-key:}")
    private String apiKey;
    
    @Value("${spring.ai.vertex.ai.gemini.chat.options.model:gemini-1.5-flash}")
    private String model;
    
    @Value("${spring.ai.vertex.ai.gemini.chat.options.temperature:0.7}")
    private Float temperature;
    
    @Value("${spring.ai.vertex.ai.gemini.chat.options.max-output-tokens:2048}")
    private Integer maxOutputTokens;
    
    @Value("${app.ai.mock-mode:false}")
    private boolean mockMode;
    
    // Spring AI beans commented out until we have proper dependencies
    /*
    @Bean
    @ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true", matchIfMissing = true)
    public ChatModel chatModel() {
        if (mockMode || apiKey == null || apiKey.isBlank()) {
            // Return a mock implementation when in mock mode or API key is not configured
            return new MockChatModel();
        }
        
        VertexAiGeminiChatOptions options = VertexAiGeminiChatOptions.builder()
                .withModel(model)
                .withTemperature(temperature)
                .withMaxOutputTokens(maxOutputTokens)
                .build();
        
        return new VertexAiGeminiChatModel(apiKey, options);
    }
    
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
    */
    
    /**
     * Mock implementation of ChatModel for development/testing
     * Commented out until we have Spring AI dependencies
     */
    /*
    private static class MockChatModel implements ChatModel {
        @Override
        public String call(String message) {
            return generateMockResponse(message);
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
            } else if (prompt.toLowerCase().contains("confluence")) {
                return "h1. Sprint Plan\n\n" +
                       "h2. Sprint Goals\n" +
                       "* Deliver key features for the release\n" +
                       "* Improve system performance\n" +
                       "* Fix critical production issues\n\n" +
                       "h2. Team Capacity\n" +
                       "* Total capacity: 240 hours\n" +
                       "* Assigned work: 204 hours\n" +
                       "* Buffer: 36 hours\n\n" +
                       "h2. Work Items\n" +
                       "* [PROJ-101] Implement new dashboard - 40h\n" +
                       "* [PROJ-102] API optimization - 32h\n" +
                       "* [PROJ-103] Bug fixes - 24h";
            } else if (prompt.toLowerCase().contains("teams")) {
                return "**🚀 Sprint Kick-off!**\n\n" +
                       "Team, we're starting a new 2-week sprint today!\n\n" +
                       "**Key Focus Areas:**\n" +
                       "• Feature delivery for Q4 release\n" +
                       "• Performance improvements\n" +
                       "• Critical bug fixes\n\n" +
                       "Let's make this sprint a success! 💪";
            } else if (prompt.toLowerCase().contains("performance") || prompt.toLowerCase().contains("insights")) {
                return "**Performance Insights (Mock Response)**\n\n" +
                       "- **Velocity Trend:** Team velocity has increased by 15% over the last 3 sprints\n" +
                       "- **Work Mix:** Good balance with 60% features, 25% tech debt, 15% bugs\n" +
                       "- **Team Utilization:** Backend at 95%, Frontend at 88%, QA at 75%\n" +
                       "- **Recommendation:** Consider adding QA resources to balance workload";
            }
            
            return "Mock AI response for: " + prompt.substring(0, Math.min(prompt.length(), 100)) + "...";
        }
    }
    */
}
