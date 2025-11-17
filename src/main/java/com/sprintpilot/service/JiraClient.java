package com.sprintpilot.service;

import com.sprintpilot.dto.TaskImportRequest;
import java.util.List;

public interface JiraClient {
    
    /**
     * Fetch tasks from Jira based on project key and JQL query
     * 
     * @param projectKey The Jira project key
     * @param jqlQuery Optional JQL query to filter tasks (if null, fetches all tasks from the project)
     * @return List of tasks fetched from Jira
     */
    List<TaskImportRequest.TaskImportDto> fetchTasks(String projectKey, String jqlQuery);
    
}

