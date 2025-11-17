package com.sprintpilot.service;

import com.sprintpilot.dto.MemberUtilizationDto;
import java.util.List;

/**
 * Service interface for team member operations
 */
public interface MemberService {
    
    /**
     * Get utilization metrics for all members in a sprint
     * 
     * @param sprintId Sprint ID to calculate utilization for
     * @return List of member utilization metrics
     */
    List<MemberUtilizationDto> getMemberUtilizationBySprintId(String sprintId);
}

