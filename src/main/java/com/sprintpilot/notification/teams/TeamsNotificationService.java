package com.sprintpilot.notification.teams;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Generic service for sending notifications to Microsoft Teams channels
 * Supports multiple use cases: sprint created, sprint completed, holiday created, etc.
 */
@Service
public class TeamsNotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(TeamsNotificationService.class);
    
    private final RestTemplate restTemplate;
    private final TeamsMessageBuilder messageBuilder;
    private final String webhookUrl;
    
    public TeamsNotificationService(
            RestTemplate restTemplate,
            TeamsMessageBuilder messageBuilder,
            @Value("${teams.webhook.url:}") String webhookUrl) {
        this.restTemplate = restTemplate;
        this.messageBuilder = messageBuilder;
        this.webhookUrl = webhookUrl;
    }
    
    /**
     * Sends a notification to Teams using a template
     * 
     * @param templateName Name of the template file (e.g., "sprint-created.json")
     * @param variables Map of variables to replace in the template (e.g., {{sprintId}} -> "SPRINT-123")
     * @return true if notification was sent successfully, false otherwise
     */
    public boolean sendNotification(String templateName, Map<String, String> variables) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            logger.warn("Teams webhook URL is not configured. Skipping notification: {}", templateName);
            return false;
        }
        
        try {
            String messageJson = messageBuilder.buildMessage(templateName, variables);
            return sendToTeams(messageJson);
        } catch (Exception e) {
            logger.error("Failed to send Teams notification for template: {}", templateName, e);
            return false;
        }
    }
    
    /**
     * Sends a raw JSON message to Teams webhook
     * 
     * @param messageJson JSON string in Teams MessageCard or Adaptive Card format
     * @return true if sent successfully, false otherwise
     */
    public boolean sendRawMessage(String messageJson) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            logger.warn("Teams webhook URL is not configured. Skipping raw message.");
            return false;
        }
        
        return sendToTeams(messageJson);
    }
    
    /**
     * Internal method to send HTTP POST request to Teams webhook
     */
    private boolean sendToTeams(String messageJson) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> request = new HttpEntity<>(messageJson, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(webhookUrl, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Teams notification sent successfully");
                return true;
            } else {
                logger.warn("Teams notification failed with status: {}", response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            logger.error("Error sending Teams notification", e);
            return false;
        }
    }
    
    /**
     * Checks if Teams notifications are enabled (webhook URL is configured)
     */
    public boolean isEnabled() {
        return webhookUrl != null && !webhookUrl.trim().isEmpty();
    }
}

