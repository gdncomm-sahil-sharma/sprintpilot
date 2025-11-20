package com.sprintpilot.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import java.time.Duration;

/**
 * Configuration class for AI integration using Spring AI
 * 
 * This configuration sets up:
 * - ChatModel for interacting with OpenAI
 * - Rate limiting using Bucket4j
 * - Conditional bean creation based on provider setting
 * 
 * Supported Providers:
 * - openai: OpenAI GPT models (via spring-ai-openai)
 * - mock: Mock implementation for testing
 * 
 * Configuration:
 * Set app.ai.provider property to choose provider (default: openai)
 * 
 * References:
 * - OpenAI: https://docs.spring.io/spring-ai/reference/api/chat/openai-chat.html
 */
@Configuration
public class AIConfig {
    
    @Value("${app.ai.provider:openai}")
    private String aiProvider;
    
    @Value("${spring.ai.openai.api-key:}")
    private String openaiApiKey;
    
    @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}")
    private String openaiModel;
    
    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double temperature;
    
    @Value("${spring.ai.openai.chat.options.top-p:0.9}")
    private Double topP;
    
    @Value("${spring.ai.openai.chat.options.max-tokens:2048}")
    private Integer maxTokens;
    
    @Value("${app.ai.rate-limit.requests-per-minute:10}")
    private int requestsPerMinute;
    
    @Value("${app.ai.rate-limit.burst-capacity:20}")
    private int burstCapacity;
    
    /**
     * Creates the OpenAI ChatModel bean when provider is set to 'openai'
     * This bean is used by OpenAIService for AI-powered features
     * 
     * @return ChatModel instance configured for OpenAI
     * @throws IllegalStateException if API key is missing
     */
    @Bean
    @ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai", matchIfMissing = true)
    public ChatModel openAiChatModel() {
        if (openaiApiKey == null || openaiApiKey.isBlank()) {
            throw new IllegalStateException(
                "OpenAI API key is required when app.ai.provider=openai. " +
                "Set OPENAI_API_KEY environment variable or spring.ai.openai.api-key property. " +
                "Get your API key from: https://platform.openai.com/api-keys"
            );
        }
        
        // Create OpenAI API client (simplified constructor using just base URL and API key)
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl("https://api.openai.com")
                .apiKey(openaiApiKey)
                .build();
        
        // Configure chat options
        // Note: Some models (like gpt-5-nano) have restrictions on parameters
        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .model(openaiModel)
                .maxCompletionTokens(maxTokens);
        
        // Only set temperature and topP for models that support them (not gpt-5-nano)
        if (!openaiModel.contains("gpt-5-nano")) {
            optionsBuilder.temperature(temperature).topP(topP);
        }
        
        OpenAiChatOptions options = optionsBuilder.build();
        
        // Create and return ChatModel with required dependencies
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }
    
    /**
     * Creates a rate limiter bucket for controlling API request frequency
     * Implements token bucket algorithm with configurable rate and burst capacity
     * 
     * This is shared across all AI providers to prevent:
     * - API quota exhaustion
     * - Excessive costs
     * - Service degradation
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
     * This bean is only created when a real AI provider is selected
     */
    public static class AIConfigValidator {
        private final ChatModel chatModel;
        private final String provider;
        
        public AIConfigValidator(ChatModel chatModel, String provider) {
            this.chatModel = chatModel;
            this.provider = provider;
        }
        
        public ChatModel getChatModel() {
            return chatModel;
        }
        
        public String getProvider() {
            return provider;
        }
    }
}

