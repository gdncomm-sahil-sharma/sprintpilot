package com.sprintpilot.notification.teams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Builds Microsoft Teams messages by replacing placeholders in templates
 */
@Component
public class TeamsMessageBuilder {
    
    private final ObjectMapper objectMapper;
    private final TeamsTemplateLoader templateLoader;
    
    public TeamsMessageBuilder(ObjectMapper objectMapper, TeamsTemplateLoader templateLoader) {
        this.objectMapper = objectMapper;
        this.templateLoader = templateLoader;
    }
    
    /**
     * Builds a Teams message from a template with variable replacements
     * 
     * @param templateName Name of the template file (e.g., "sprint-created.json")
     * @param variables Map of variable names to values for placeholder replacement
     * @return JSON string ready to send to Teams webhook
     * @throws Exception if template cannot be loaded or processed
     */
    public String buildMessage(String templateName, Map<String, String> variables) throws Exception {
        JsonNode template = templateLoader.loadTemplate(templateName);
        JsonNode processedTemplate = replacePlaceholders(template, variables);
        return objectMapper.writeValueAsString(processedTemplate);
    }
    
    /**
     * Recursively replaces placeholders in JSON template
     * Placeholders should be in format: {{variableName}}
     */
    private JsonNode replacePlaceholders(JsonNode node, Map<String, String> variables) {
        if (node.isTextual()) {
            String text = node.asText();
            String replaced = replacePlaceholdersInString(text, variables);
            return objectMapper.valueToTree(replaced);
        } else if (node.isObject()) {
            ObjectNode objectNode = objectMapper.createObjectNode();
            node.fields().forEachRemaining(entry -> {
                objectNode.set(entry.getKey(), replacePlaceholders(entry.getValue(), variables));
            });
            return objectNode;
        } else if (node.isArray()) {
            com.fasterxml.jackson.databind.node.ArrayNode arrayNode = objectMapper.createArrayNode();
            for (JsonNode element : node) {
                arrayNode.add(replacePlaceholders(element, variables));
            }
            return arrayNode;
        }
        return node;
    }
    
    /**
     * Replaces placeholders in a string
     * Format: {{variableName}} -> value from variables map
     */
    private String replacePlaceholdersInString(String text, Map<String, String> variables) {
        if (text == null || variables == null) {
            return text;
        }
        
        String result = text;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }
}

