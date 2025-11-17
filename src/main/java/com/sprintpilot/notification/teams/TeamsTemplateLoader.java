package com.sprintpilot.notification.teams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Loads Microsoft Teams message templates from JSON files in resources
 */
@Component
public class TeamsTemplateLoader {
    
    private static final String TEMPLATE_BASE_PATH = "templates/teams/";
    private final ObjectMapper objectMapper;
    
    public TeamsTemplateLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * Loads a template from the resources directory
     * 
     * @param templateName Name of the template file (e.g., "sprint-created.json")
     * @return JsonNode representing the template
     * @throws IOException if template file cannot be loaded
     */
    public JsonNode loadTemplate(String templateName) throws IOException {
        String templatePath = TEMPLATE_BASE_PATH + templateName;
        ClassPathResource resource = new ClassPathResource(templatePath);
        
        if (!resource.exists()) {
            throw new IOException("Template not found: " + templatePath);
        }
        
        try (InputStream inputStream = resource.getInputStream()) {
            String templateContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return objectMapper.readTree(templateContent);
        }
    }
    
    /**
     * Loads template as a JSON string
     * 
     * @param templateName Name of the template file
     * @return Template as JSON string
     * @throws IOException if template file cannot be loaded
     */
    public String loadTemplateAsString(String templateName) throws IOException {
        JsonNode template = loadTemplate(templateName);
        return objectMapper.writeValueAsString(template);
    }
}

