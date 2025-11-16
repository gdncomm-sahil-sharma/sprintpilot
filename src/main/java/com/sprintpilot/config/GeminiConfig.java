package com.sprintpilot.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import java.time.Duration;

/**
 * Configuration class for Google GenAI (Gemini) integration using Spring AI
 * 
 * This configuration sets up:
 * - ChatModel for interacting with Gemini API (auto-configured by Spring AI starter)
 * - Rate limiting using Bucket4j
 * - Conditional bean creation based on mock-mode setting
 * 
 * Reference: https://docs.spring.io/spring-ai/reference/api/chat/google-genai-chat.html
 */
@Configuration
public class GeminiConfig {
    
    @Value("${spring.ai.google.genai.api-key:}")
    private String apiKey;
    
    @Value("${app.ai.mock-mode:false}")
    private boolean mockMode;
    
    @Value("${app.ai.rate-limit.requests-per-minute:10}")
    private int requestsPerMinute;
    
    @Value("${app.ai.rate-limit.burst-capacity:20}")
    private int burstCapacity;
    
    /**
     * Validates Google GenAI configuration when not in mock mode
     * The ChatModel bean is auto-configured by spring-ai-starter-model-google-genai
     * 
     * @param chatModel The auto-configured ChatModel from Spring AI starter
     * @return GeminiConfigValidator instance
     * @throws IllegalStateException if API key is missing when mock-mode is false
     */
    @Bean
    @ConditionalOnProperty(name = "app.ai.mock-mode", havingValue = "false", matchIfMissing = false)
    public GeminiConfigValidator geminiConfigValidator(ChatModel chatModel) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                "Google GenAI API key is required when mock-mode is false. " +
                "Set GEMINI_API_KEY environment variable or spring.ai.google.genai.api-key property. " +
                "Get your API key from: https://aistudio.google.com/app/apikey"
            );
        }
        return new GeminiConfigValidator(chatModel);
    }
    
    /**
     * Creates a rate limiter bucket for controlling API request frequency
     * Implements token bucket algorithm with configurable rate and burst capacity
     * 
     * Benefits:
     * - Prevents API quota exhaustion
     * - Controls costs by limiting request rate
     * - Provides burst capacity for occasional spikes
     * 
     * @return Bucket instance for rate limiting
     */
    @Bean
    public Bucket rateLimiterBucket() {
        // Define the refill rate (tokens added per minute)
        Refill refill = Refill.intervally(requestsPerMinute, Duration.ofMinutes(1));
        
        // Define bandwidth with burst capacity
        Bandwidth limit = Bandwidth.classic(burstCapacity, refill);
        
        // Create and return the bucket
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
    
    /**
     * Simple validator class to ensure ChatModel is properly configured
     * This bean is only created when not in mock mode, ensuring real AI setup is validated
     */
    public static class GeminiConfigValidator {
        private final ChatModel chatModel;
        
        public GeminiConfigValidator(ChatModel chatModel) {
            this.chatModel = chatModel;
        }
        
        public ChatModel getChatModel() {
            return chatModel;
        }
    }
}
