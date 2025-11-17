package com.sprintpilot.confluence;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Loads Confluence page templates from resources
 * Similar to TeamsTemplateLoader pattern
 */
@Component
public class ConfluenceTemplateLoader {
    
    private static final String TEMPLATE_BASE_PATH = "templates/confluence/";
    
    /**
     * Loads a template from the resources directory
     * 
     * @param templateName Name of the template file (e.g., "sprint-planning-page.html")
     * @return Template content as string
     * @throws IOException if template file cannot be loaded
     */
    public String loadTemplate(String templateName) throws IOException {
        String templatePath = TEMPLATE_BASE_PATH + templateName;
        ClassPathResource resource = new ClassPathResource(templatePath);
        
        if (!resource.exists()) {
            throw new IOException("Template not found: " + templatePath);
        }
        
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

