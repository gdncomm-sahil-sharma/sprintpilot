package com.sprintpilot.service;

import java.util.Map;

/**
 * Client interface for Confluence REST API operations
 * Provides methods for creating, updating, and searching Confluence pages
 */
public interface ConfluenceClient {
    
    /**
     * Creates a new page in Confluence
     * 
     * @param spaceKey The Confluence space key (e.g., "SPRINT")
     * @param title Page title
     * @param content Page content in HTML or Confluence Storage Format
     * @param parentPageId Optional parent page ID for hierarchical structure
     * @return Created page ID
     * @throws Exception if page creation fails
     */
    String createPage(String spaceKey, String title, String content, String parentPageId) throws Exception;
    
    /**
     * Updates an existing Confluence page
     * 
     * @param pageId The page ID to update
     * @param content New page content
     * @param currentVersion Current page version (for optimistic locking)
     * @return Updated page ID
     * @throws Exception if page update fails
     */
    String updatePage(String pageId, String content, Integer currentVersion) throws Exception;
    
    /**
     * Appends content to an existing page
     * 
     * @param pageId The page ID to update
     * @param contentSection Content section to append
     * @param currentVersion Current page version
     * @return Updated page ID
     * @throws Exception if append operation fails
     */
    String appendToPage(String pageId, String contentSection, Integer currentVersion) throws Exception;
    
    /**
     * Gets a page by ID
     * 
     * @param pageId The page ID
     * @return Map containing page data (id, title, content, version, etc.)
     * @throws Exception if page retrieval fails
     */
    Map<String, Object> getPage(String pageId) throws Exception;
    
    /**
     * Searches for a page by title in a space
     * 
     * @param spaceKey The Confluence space key
     * @param title Exact page title to search for
     * @return Page ID if found, null otherwise
     * @throws Exception if search fails
     */
    String searchPageByTitle(String spaceKey, String title) throws Exception;
}

