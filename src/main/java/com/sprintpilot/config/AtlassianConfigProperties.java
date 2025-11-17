package com.sprintpilot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "atlassian")
@Data
public class AtlassianConfigProperties {
    
    private String baseUrl;
    private String email;
    private String apiToken;
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(10);
    private String wikiPath;
    
}

