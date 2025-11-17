package com.sprintpilot.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprintpilot.config.JiraConfigProperties;
import com.sprintpilot.dto.TaskImportRequest;
import com.sprintpilot.service.JiraClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@Slf4j
public class JiraClientImpl implements JiraClient {

    @Autowired
    private JiraConfigProperties jiraConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpClient httpClient;

    @Autowired
    public void init() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(jiraConfig.getConnectTimeout())
                .build();
    }

    @Override
    public List<TaskImportRequest.TaskImportDto> fetchTasks(String projectKey, String jqlQuery) {
        try {
            String jql = (jqlQuery != null && !jqlQuery.isBlank()) 
                ? jqlQuery 
                : "project = " + projectKey;
            
            // Use the new Jira API endpoint: /rest/api/3/search/jql (POST with JSON body)
            String url = jiraConfig.getBaseUrl() + "/rest/api/3/search/jql";
            
            // Build JSON request body
            String requestBody = String.format(
                "{\"jql\": \"%s\", \"maxResults\": 100, \"fields\": [\"summary\",\"description\",\"customfield_10016\",\"issuetype\",\"priority\",\"status\",\"assignee\",\"created\",\"duedate\",\"resolutiondate\"]}",
                jql.replace("\"", "\\\"")  // Escape quotes in JQL
            );
            
            log.debug("Jira API request URL: {}", url);
            log.debug("Jira API request body: {}", requestBody);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", getBasicAuthHeader())
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .timeout(jiraConfig.getReadTimeout())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new RuntimeException("Jira API returned status code: " + response.statusCode() + 
                        ", body: " + response.body());
            }
            
            return parseJiraResponse(response.body());
            
        } catch (IOException | InterruptedException e) {
            log.error("Error fetching tasks from Jira", e);
            throw new RuntimeException("Failed to fetch tasks from Jira: " + e.getMessage(), e);
        }
    }

    private String getBasicAuthHeader() {
        String auth = jiraConfig.getEmail() + ":" + jiraConfig.getApiToken();
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
    }

    private List<TaskImportRequest.TaskImportDto> parseJiraResponse(String responseBody) {
        List<TaskImportRequest.TaskImportDto> tasks = new ArrayList<>();
        
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode issues = root.get("issues");
            
            if (issues == null || !issues.isArray()) {
                log.warn("No issues found in Jira response");
                return tasks;
            }
            
            for (JsonNode issue : issues) {
                try {
                    String taskKey = issue.get("key").asText();
                    JsonNode fields = issue.get("fields");
                    
                    String summary = fields.has("summary") ? fields.get("summary").asText() : "";
                    String description = fields.has("description") ? extractDescription(fields.get("description")) : "";
                    
                    // Story points - customfield_10016 is typically used for story points in Jira
                    BigDecimal storyPoints = BigDecimal.ZERO;
                    if (fields.has("customfield_10016") && !fields.get("customfield_10016").isNull()) {
                        storyPoints = new BigDecimal(fields.get("customfield_10016").asText("0"));
                    }
                    
                    // Map Jira issue type to our category
                    String category = mapIssueTypeToCategory(fields.get("issuetype"));
                    
                    // Map Jira priority
                    String priority = fields.has("priority") && !fields.get("priority").isNull() 
                        ? fields.get("priority").get("name").asText().toUpperCase()
                        : "MEDIUM";
                    
                    // Map Jira status
                    String status = fields.has("status") 
                        ? mapJiraStatus(fields.get("status").get("name").asText())
                        : "TODO";
                    
                    // Get assignee name and email
                    String assignee = null;
                    String assigneeEmail = null;
                    if (fields.has("assignee") && !fields.get("assignee").isNull()) {
                        JsonNode assigneeNode = fields.get("assignee");
                        assignee = assigneeNode.has("displayName") 
                            ? assigneeNode.get("displayName").asText() 
                            : null;
                        assigneeEmail = assigneeNode.has("emailAddress") 
                            ? assigneeNode.get("emailAddress").asText() 
                            : null;
                    }
                    
                    // Extract dates
                    // startDate: use created date (when issue was created)
                    String startDate = null;
                    if (fields.has("created") && !fields.get("created").isNull()) {
                        startDate = extractDate(fields.get("created").asText());
                    }
                    
                    // dueDate: use duedate field
                    String dueDate = null;
                    if (fields.has("duedate") && !fields.get("duedate").isNull()) {
                        dueDate = extractDate(fields.get("duedate").asText());
                    }
                    
                    // endDate: use resolutiondate if available, otherwise null
                    String endDate = null;
                    if (fields.has("resolutiondate") && !fields.get("resolutiondate").isNull()) {
                        endDate = extractDate(fields.get("resolutiondate").asText());
                    }
                    
                    TaskImportRequest.TaskImportDto task = new TaskImportRequest.TaskImportDto(
                            taskKey,
                            summary,
                            description,
                            storyPoints,
                            category,
                            priority,
                            status,
                            assignee,
                            assigneeEmail,
                            startDate,
                            endDate,
                            dueDate
                    );
                    
                    tasks.add(task);
                    
                } catch (Exception e) {
                    log.error("Error parsing Jira issue: " + issue.get("key").asText(), e);
                    // Continue with next issue
                }
            }
            
            log.info("Successfully parsed {} tasks from Jira", tasks.size());
            return tasks;
            
        } catch (Exception e) {
            log.error("Error parsing Jira response", e);
            throw new RuntimeException("Failed to parse Jira response: " + e.getMessage(), e);
        }
    }

    private String extractDescription(JsonNode descriptionNode) {
        // Jira uses ADF (Atlassian Document Format) for description
        // Extract plain text from it
        if (descriptionNode == null || descriptionNode.isNull()) {
            return "";
        }
        
        StringBuilder description = new StringBuilder();
        extractTextFromADF(descriptionNode, description);
        return description.toString().trim();
    }

    private void extractTextFromADF(JsonNode node, StringBuilder text) {
        if (node.has("text")) {
            text.append(node.get("text").asText()).append(" ");
        }
        
        if (node.has("content") && node.get("content").isArray()) {
            for (JsonNode child : node.get("content")) {
                extractTextFromADF(child, text);
            }
        }
    }

    private String mapIssueTypeToCategory(JsonNode issueType) {
        if (issueType == null || issueType.isNull()) {
            return "OTHER";
        }
        
        String typeName = issueType.get("name").asText().toLowerCase();
        
        if (typeName.contains("story") || typeName.contains("feature") || typeName.contains("epic")) {
            return "FEATURE";
        } else if (typeName.contains("bug") || typeName.contains("defect")) {
            return "PROD_ISSUE";
        } else if (typeName.contains("tech") || typeName.contains("debt") || typeName.contains("task")) {
            return "TECH_DEBT";
        } else {
            return "OTHER";
        }
    }

    private String mapJiraStatus(String jiraStatus) {
        String status = jiraStatus.toLowerCase();
        
        if (status.contains("done") || status.contains("closed") || status.contains("resolved")) {
            return "DONE";
        } else if (status.contains("progress") || status.contains("develop")) {
            return "IN_PROGRESS";
        } else if (status.contains("review") || status.contains("testing") || status.contains("qa")) {
            return "IN_REVIEW";
        } else {
            return "TODO";
        }
    }

    private String extractDate(String jiraDateString) {
        if (jiraDateString == null || jiraDateString.isBlank()) {
            return null;
        }
        
        try {
            // Jira dates are in ISO 8601 format: "2023-12-25T10:30:00.000+0000"
            // We only need the date part: "2023-12-25"
            if (jiraDateString.contains("T")) {
                return jiraDateString.substring(0, jiraDateString.indexOf("T"));
            }
            // If it's already just a date string
            return jiraDateString;
        } catch (Exception e) {
            log.warn("Failed to extract date from: {}", jiraDateString, e);
            return null;
        }
    }
}

