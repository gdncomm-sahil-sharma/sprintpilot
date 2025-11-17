package com.sprintpilot.confluence;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds Confluence page content by replacing placeholders in templates
 * Similar to TeamsMessageBuilder pattern
 */
@Component
public class ConfluencePageBuilder {
    
    private final ConfluenceTemplateLoader templateLoader;
    
    public ConfluencePageBuilder(ConfluenceTemplateLoader templateLoader) {
        this.templateLoader = templateLoader;
    }
    
    /**
     * Builds a Confluence page content from a template with variable replacements
     * 
     * @param templateName Name of the template file (e.g., "sprint-planning-page.html")
     * @param variables Map of variable names to values for placeholder replacement
     * @return HTML content ready for Confluence
     * @throws Exception if template cannot be loaded or processed
     */
    public String buildPageContent(String templateName, Map<String, String> variables) throws Exception {
        String template = templateLoader.loadTemplate(templateName);
        return replacePlaceholders(template, variables);
    }
    
    /**
     * Replaces placeholders in template string
     * Supports:
     * - Simple placeholders: {{variableName}}
     * - Conditional blocks: {{#if variable}}...{{/if}}
     * - Loops: {{#each items}}...{{/each}}
     */
    private String replacePlaceholders(String template, Map<String, String> variables) {
        if (template == null || variables == null) {
            return template;
        }
        
        String result = template;
        
        // Replace simple placeholders: {{variableName}}
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, value);
        }
        
        // Handle conditional blocks: {{#if variable}}...{{/if}}
        result = processConditionalBlocks(result, variables);
        
        // Handle loops: {{#each items}}...{{/each}}
        result = processLoops(result, variables);
        
        return result;
    }
    
    /**
     * Processes conditional blocks: {{#if variable}}...{{/if}}
     */
    private String processConditionalBlocks(String template, Map<String, String> variables) {
        Pattern pattern = Pattern.compile("\\{\\{#if\\s+(\\w+)\\}\\}(.*?)\\{\\{/if\\}\\}", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(template);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String variableName = matcher.group(1);
            String blockContent = matcher.group(2);
            
            String value = variables.get(variableName);
            boolean condition = value != null && !value.isEmpty() && !value.equals("false") && !value.equals("0");
            
            if (condition) {
                // Keep the content, but process nested placeholders
                String processedContent = replacePlaceholders(blockContent, variables);
                matcher.appendReplacement(result, Matcher.quoteReplacement(processedContent));
            } else {
                // Remove the block
                matcher.appendReplacement(result, "");
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Processes loop blocks: {{#each items}}...{{/each}}
     * Note: This is a simplified implementation. For complex nested structures,
     * consider using a proper templating engine like Handlebars or Mustache
     */
    private String processLoops(String template, Map<String, String> variables) {
        // For now, loops are handled by passing pre-formatted HTML strings
        // This can be enhanced later if needed
        return template;
    }
    
    /**
     * Converts HTML to Confluence Storage Format
     * 
     * Note: Confluence REST API accepts HTML with representation="storage"
     * For basic HTML, we can use it directly. For complex formatting,
     * consider using Confluence Storage Format XML or a conversion library.
     * 
     * @param html HTML content
     * @return Content ready for Confluence (currently returns HTML as-is)
     */
    public String htmlToConfluenceStorage(String html) {
        // For now, return HTML as-is. The ConfluenceClient will set representation="storage"
        // which tells Confluence to interpret the content as Storage Format
        // If needed, this can be enhanced to convert HTML to proper Confluence Storage Format XML
        return html;
    }
}

