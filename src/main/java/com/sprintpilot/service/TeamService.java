package com.sprintpilot.service;

import com.sprintpilot.dto.TeamMemberDto;
import com.sprintpilot.dto.CapacitySummaryDto;
import java.util.List;

public interface TeamService {
    
    TeamMemberDto createTeamMember(TeamMemberDto memberDto);
    
    TeamMemberDto updateTeamMember(String id, TeamMemberDto memberDto);
    
    TeamMemberDto getTeamMemberById(String id);
    
    List<TeamMemberDto> getAllTeamMembers();
    
    List<TeamMemberDto> getActiveTeamMembers();
    
    List<TeamMemberDto> getTeamMembersByRole(String role);
    
    void deleteTeamMember(String id);
    
    TeamMemberDto addLeaveDay(String memberId, String leaveDate);
    
    TeamMemberDto removeLeaveDay(String memberId, String leaveDate);
    
    List<CapacitySummaryDto> calculateTeamCapacity(String sprintId);
    
    CapacitySummaryDto getMemberCapacity(String memberId, String sprintId);
}
