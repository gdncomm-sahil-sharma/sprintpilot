package com.sprintpilot.service;

import com.sprintpilot.dto.TeamMemberDto;
import com.sprintpilot.dto.CapacitySummaryDto;
import com.sprintpilot.dto.SprintAssignmentRequest;
import java.util.List;

public interface TeamService {
    
    TeamMemberDto createTeamMember(TeamMemberDto memberDto);
    
    TeamMemberDto updateTeamMember(String id, TeamMemberDto memberDto);
    
    TeamMemberDto getTeamMemberById(String id);
    
    TeamMemberDto getTeamMemberById(String id, String sprintId);
    
    List<TeamMemberDto> getAllTeamMembers();
    
    List<TeamMemberDto> getAllTeamMembers(String sprintId);
    
    List<TeamMemberDto> getActiveTeamMembers(String sprintId);
    
    List<TeamMemberDto> getTeamMembersByRole(String role);
    
    void deleteTeamMember(String id);
    
    List<CapacitySummaryDto> calculateTeamCapacity(String sprintId);
    
    void assignMembersToSprint(SprintAssignmentRequest request);
    
    List<String> getMemberIdsForSprint(String sprintId);
    
    List<TeamMemberDto> getTeamMembersForSprint(String sprintId);
}
