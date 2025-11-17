package com.sprintpilot.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprintpilot.config.AtlassianConfigProperties;
import com.sprintpilot.service.ConfluenceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of ConfluenceClient using Confluence REST API
 * Follows the same pattern as JiraClientImpl
 */
@Service
public class ConfluenceClientImpl implements ConfluenceClient {
    
    private static final Logger log = LoggerFactory.getLogger(ConfluenceClientImpl.class);
    
    @Autowired
    private AtlassianConfigProperties atlassianConfig;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpClient httpClient;
    
    @Autowired
    public void init() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(atlassianConfig.getConnectTimeout())
                .build();
    }
    
    /**
     * Constructs the Confluence base URL by properly handling trailing/leading slashes
     */
    private String getConfluenceBaseUrl() {
        String baseUrl = atlassianConfig.getBaseUrl();
        String wikiPath = atlassianConfig.getWikiPath();
        
        if (baseUrl == null) {
            baseUrl = "";
        }
        
        // Remove trailing slash from baseUrl if present
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        
        // Handle wikiPath - if null or empty, default to /wiki
        if (wikiPath == null || wikiPath.isEmpty()) {
            wikiPath = "wiki";
        }
        
        // Remove leading slash from wikiPath if present
        if (wikiPath.startsWith("/")) {
            wikiPath = wikiPath.substring(1);
        }
        
        // Combine with single slash
        return baseUrl + "/" + wikiPath;
    }
    
    @Override
    public String createPage(String spaceKey, String title, String content, String parentPageId) throws Exception {
        String baseUrl = getConfluenceBaseUrl();
        String url = baseUrl + "/rest/api/content";
        
        // Build request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("type", "page");
        requestBody.put("title", title);
        requestBody.put("space", Map.of("key", spaceKey));
        
        // Build content structure
        // Using "storage" representation allows HTML content
        // Confluence will convert HTML to Storage Format automatically
        Map<String, Object> storage = new HashMap<>();
        storage.put("value", content);
        storage.put("representation", "storage");
        
        Map<String, Object> body = new HashMap<>();
        body.put("storage", storage);
        requestBody.put("body", body);
        
        // Add parent if provided
        if (parentPageId != null && !parentPageId.isEmpty()) {
            Map<String, Object> ancestors = new HashMap<>();
            ancestors.put("id", parentPageId);
            requestBody.put("ancestors", java.util.List.of(ancestors));
        }
        
        String requestBodyJson = objectMapper.writeValueAsString(requestBody);
        
        log.debug("Creating Confluence page: {}", title);
        log.debug("Request URL: {}", url);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", getBasicAuthHeader())
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .timeout(atlassianConfig.getReadTimeout())
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("Confluence API returned status code: " + response.statusCode() + 
                    ", body: " + response.body());
        }
        
        JsonNode responseJson = objectMapper.readTree(response.body());
        String pageId = responseJson.get("id").asText();
        log.info("Confluence page created successfully: {} (ID: {})", title, pageId);
        return pageId;
    }
    
    @Override
    public String updatePage(String pageId, String content, Integer currentVersion) throws Exception {
        String baseUrl = getConfluenceBaseUrl();
        String url = baseUrl + "/rest/api/content/" + pageId;
        
        // First, get current page to preserve structure
        Map<String, Object> currentPage = getPage(pageId);
        Integer version = currentVersion != null ? currentVersion : ((Number) currentPage.get("version")).intValue() + 1;
        
        // Build request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("id", pageId);
        requestBody.put("type", "page");
        requestBody.put("title", currentPage.get("title"));
        requestBody.put("version", Map.of("number", version));
        
        // Build content structure
        Map<String, Object> storage = new HashMap<>();
        storage.put("value", content);
        storage.put("representation", "storage");
        
        Map<String, Object> body = new HashMap<>();
        body.put("storage", storage);
        requestBody.put("body", body);
        
        String requestBodyJson = objectMapper.writeValueAsString(requestBody);
        
        log.debug("Updating Confluence page: {}", pageId);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", getBasicAuthHeader())
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .timeout(atlassianConfig.getReadTimeout())
                .PUT(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("Confluence API returned status code: " + response.statusCode() + 
                    ", body: " + response.body());
        }
        
        JsonNode responseJson = objectMapper.readTree(response.body());
        String updatedPageId = responseJson.get("id").asText();
        log.info("Confluence page updated successfully: {}", updatedPageId);
        return updatedPageId;
    }
    
    @Override
    public String appendToPage(String pageId, String contentSection, Integer currentVersion) throws Exception {
        // Get current page content
        Map<String, Object> currentPage = getPage(pageId);
        String currentContent = extractContentFromPage(currentPage);
        
        // Append new section
        String newContent = currentContent + "\n\n" + contentSection;
        
        // Update page with appended content
        return updatePage(pageId, newContent, currentVersion);
    }
    
    @Override
    public Map<String, Object> getPage(String pageId) throws Exception {
        String baseUrl = getConfluenceBaseUrl();
        String url = baseUrl + "/rest/api/content/" + pageId + "?expand=body.storage,version";
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", getBasicAuthHeader())
                .header("Accept", "application/json")
                .timeout(atlassianConfig.getReadTimeout())
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("Confluence API returned status code: " + response.statusCode() + 
                    ", body: " + response.body());
        }
        
        JsonNode responseJson = objectMapper.readTree(response.body());
        
        Map<String, Object> pageData = new HashMap<>();
        pageData.put("id", responseJson.get("id").asText());
        pageData.put("title", responseJson.get("title").asText());
        pageData.put("version", responseJson.get("version").get("number").asInt());
        
        // Extract content
        if (responseJson.has("body") && responseJson.get("body").has("storage")) {
            pageData.put("content", responseJson.get("body").get("storage").get("value").asText());
        }
        
        // Extract links
        if (responseJson.has("_links")) {
            JsonNode links = responseJson.get("_links");
            if (links.has("webui")) {
                String webui = links.get("webui").asText();
                pageData.put("webUrl", baseUrl + webui);
            }
            if (links.has("tinyui")) {
                pageData.put("tinyUrl", baseUrl + links.get("tinyui").asText());
            }
        }
        
        return pageData;
    }
    
    @Override
    public String searchPageByTitle(String spaceKey, String title) throws Exception {
        String baseUrl = getConfluenceBaseUrl();
        // Use CQL (Confluence Query Language) to search
        String cql = String.format("space=%s AND title=\"%s\"", spaceKey, title);
        String url = baseUrl + "/rest/api/content/search?cql=" + java.net.URLEncoder.encode(cql, "UTF-8");
        
        log.debug("Searching for Confluence page: {} in space: {}", title, spaceKey);
        log.debug("Request URL: {}", url);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", getBasicAuthHeader())
                .header("Accept", "application/json")
                .timeout(atlassianConfig.getReadTimeout())
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("Confluence API returned status code: " + response.statusCode() + 
                    ", body: " + response.body());
        }
        
        JsonNode responseJson = objectMapper.readTree(response.body());
        JsonNode results = responseJson.get("results");
        
        if (results != null && results.isArray() && results.size() > 0) {
            String pageId = results.get(0).get("id").asText();
            log.debug("Found Confluence page: {} (ID: {})", title, pageId);
            return pageId;
        }
        
        log.debug("Confluence page not found: {}", title);
        return null;
    }
    
    /**
     * Extracts content from page data map
     */
    private String extractContentFromPage(Map<String, Object> pageData) {
        Object content = pageData.get("content");
        if (content != null) {
            return content.toString();
        }
        // If content is not in the expected format, try to get it from the page
        try {
            String pageId = (String) pageData.get("id");
            if (pageId != null) {
                Map<String, Object> fullPage = getPage(pageId);
                return extractContentFromPage(fullPage);
            }
        } catch (Exception e) {
            log.warn("Failed to extract content from page", e);
        }
        return "";
    }
    
    /**
     * Creates Basic Auth header using Atlassian credentials
     */
    private String getBasicAuthHeader() {
        String auth = atlassianConfig.getEmail() + ":" + atlassianConfig.getApiToken();
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
    }
}

