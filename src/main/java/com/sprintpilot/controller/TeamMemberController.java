package com.sprintpilot.controller;

import com.sprintpilot.dto.ApiResponse;
import com.sprintpilot.dto.SprintAssignmentRequest;
import com.sprintpilot.dto.SprintDto;
import com.sprintpilot.dto.TeamMemberDto;
import com.sprintpilot.entity.TeamMember;
import com.sprintpilot.service.SprintService;
import com.sprintpilot.service.TeamService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for team member management operations
 */
@RestController
@RequestMapping("/api/team-members")
@Tag(name = "Team Members", description = "APIs for managing team members, their roles, and leave days")
public class TeamMemberController {
    
    private static final Logger log = LoggerFactory.getLogger(TeamMemberController.class);
    
    @Autowired
    private TeamService teamService;
    
    @Autowired
    private SprintService sprintService;
    
    /**
     * Create a new team member
     * POST /api/team-members
     * If an active sprint exists, automatically assigns the member to it
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TeamMemberDto>> createTeamMember(
            @Parameter(description = "Team member details to create", required = true)
            @RequestBody TeamMemberDto memberDto) {
        try {
            // Create the team member
            TeamMemberDto createdMember = teamService.createTeamMember(memberDto);
            
            // Check if there's an active sprint
            List<SprintDto> activeSprints = sprintService.getActiveSprints();
            
            if (!activeSprints.isEmpty()) {
                // Get the first active sprint (assuming there's typically only one active sprint)
                SprintDto activeSprint = activeSprints.get(0);
                log.info("Found active sprint '{}' (ID: {}). Auto-assigning member '{}' to sprint.",
                        activeSprint.sprintName(), activeSprint.id(), createdMember.name());
                
                try {
                    // Assign the member to the active sprint
                    teamService.assignSingleMemberToSprint(createdMember.id(), activeSprint.id());
                    log.info("Successfully auto-assigned member '{}' to active sprint '{}'",
                            createdMember.name(), activeSprint.sprintName());
                    
                    return ResponseEntity
                            .status(HttpStatus.CREATED)
                            .body(ApiResponse.success(
                                    "Team member created and assigned to active sprint '" + activeSprint.sprintName() + "'", 
                                    createdMember));
                } catch (Exception e) {
                    // If auto-assignment fails, log the error but still return success for member creation
                    log.warn("Failed to auto-assign member '{}' to active sprint '{}': {}",
                            createdMember.name(), activeSprint.sprintName(), e.getMessage());
                    
                    return ResponseEntity
                            .status(HttpStatus.CREATED)
                            .body(ApiResponse.success(
                                    "Team member created successfully, but auto-assignment to sprint failed: " + e.getMessage(), 
                                    createdMember));
                }
            } else {
                log.info("No active sprint found. Member '{}' created without sprint assignment.",
                        createdMember.name());
                
                return ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(ApiResponse.success("Team member created successfully (no active sprint to assign)", createdMember));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.failure("Validation failed: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("Failed to create team member: " + e.getMessage()));
        }
    }
    
    /**
     * Update an existing team member
     * PUT /api/team-members/{id}
     * Reads current sprint ID from cookie 'currentSprintId' to assign sprint to leave days
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TeamMemberDto>> updateTeamMember(
            @PathVariable String id,
            @RequestBody TeamMemberDto memberDto,
            HttpServletRequest request) {
        try {
            // Get sprintId from cookie to assign to leave days
            String currentSprintId = getSprintIdFromCookie(request);
            
            TeamMemberDto updatedMember = teamService.updateTeamMember(id, memberDto, currentSprintId);
            return ResponseEntity.ok(
                    ApiResponse.success("Team member updated successfully", updatedMember));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.failure("Validation failed: " + e.getMessage()));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.failure(e.getMessage()));
            }
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("Failed to update team member: " + e.getMessage()));
        }
    }
    
    /**
     * Delete a team member
     * DELETE /api/team-members/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteTeamMember(@PathVariable String id) {
        try {
            teamService.deleteTeamMember(id);
            return ResponseEntity.ok(
                    ApiResponse.success("Team member deleted successfully", null));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.failure(e.getMessage()));
            }
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("Failed to delete team member: " + e.getMessage()));
        }
    }
    
    /**
     * Get all team members
     * GET /api/team-members
     * Reads current sprint ID from cookie 'currentSprintId'
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TeamMemberDto>>> getAllTeamMembers(
            @RequestParam(value = "active", required = false) Boolean active,
            HttpServletRequest request) {
        try {
            // Get sprintId from cookie
            String currentSprint = getSprintIdFromCookie(request);
            
            List<TeamMemberDto> members;
            if (active != null && active) {
                members = teamService.getActiveTeamMembers(currentSprint);
            } else {
                members = teamService.getAllTeamMembers(currentSprint);
            }
            return ResponseEntity.ok(ApiResponse.success(members));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("Failed to retrieve team members: " + e.getMessage()));
        }
    }
    
    /**
     * Get a specific team member by ID
     * GET /api/team-members/{id}
     * Reads current sprint ID from cookie 'currentSprintId' to determine sprint assignment status
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TeamMemberDto>> getTeamMemberById(
            @Parameter(description = "Team member ID", required = true)
            @PathVariable String id,
            HttpServletRequest request) {
        try {
            // Get sprintId from cookie
            String currentSprint = getSprintIdFromCookie(request);
            
            TeamMemberDto member = teamService.getTeamMemberById(id, currentSprint);
            return ResponseEntity.ok(ApiResponse.success(member));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.failure(e.getMessage()));
            }
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("Failed to retrieve team member: " + e.getMessage()));
        }
    }
    
    /**
     * Get team members by role
     * GET /api/team-members/role/{role}
     */
    @GetMapping("/role/{role}")
    public ResponseEntity<ApiResponse<List<TeamMemberDto>>> getTeamMembersByRole(
            @PathVariable String role) {
        try {
            List<TeamMemberDto> members = teamService.getTeamMembersByRole(role.toUpperCase());
            return ResponseEntity.ok(ApiResponse.success(members));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.failure("Invalid role: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("Failed to retrieve team members: " + e.getMessage()));
        }
    }
    
    /**
     * Get available roles
     * GET /api/team-members/roles
     */
    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<String>>> getAvailableRoles() {
        try {
            List<String> roles = List.of(
                TeamMember.Role.BACKEND.name(),
                TeamMember.Role.FRONTEND.name(),
                TeamMember.Role.QA.name(),
                TeamMember.Role.DEVOPS.name(),
                TeamMember.Role.MANAGER.name(),
                TeamMember.Role.DESIGNER.name()
            );
            return ResponseEntity.ok(ApiResponse.success(roles));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("Failed to retrieve roles: " + e.getMessage()));
        }
    }
    
    /**
     * Assign team members to a sprint
     * POST /api/team-members/assign-to-sprint
     */
    @PostMapping("/assign-to-sprint")
    public ResponseEntity<ApiResponse<String>> assignMembersToSprint(
            @Parameter(description = "Sprint assignment details", required = true)
            @RequestBody SprintAssignmentRequest request) {
        try {
            teamService.assignMembersToSprint(request);
            return ResponseEntity.ok(
                    ApiResponse.success(
                            String.format("%d member(s) assigned to sprint successfully", 
                                    request.memberIds().size()), 
                            null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.failure("Validation failed: " + e.getMessage()));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.failure(e.getMessage()));
            }
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("Failed to assign members to sprint: " + e.getMessage()));
        }
    }
    
    /**
     * Assign a single team member to a sprint (without affecting other members)
     * POST /api/team-members/{memberId}/sprint/{sprintId}
     */
    @PostMapping("/{memberId}/sprint/{sprintId}")
    public ResponseEntity<ApiResponse<String>> assignSingleMemberToSprint(
            @Parameter(description = "Team member ID", required = true)
            @PathVariable String memberId,
            @Parameter(description = "Sprint ID", required = true)
            @PathVariable String sprintId) {
        try {
            teamService.assignSingleMemberToSprint(memberId, sprintId);
            return ResponseEntity.ok(
                    ApiResponse.success("Member assigned to sprint successfully", null));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.failure(e.getMessage()));
            }
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("Failed to assign member to sprint: " + e.getMessage()));
        }
    }
    
    /**
     * Unassign a team member from a sprint
     * DELETE /api/team-members/{memberId}/sprint/{sprintId}
     */
    @DeleteMapping("/{memberId}/sprint/{sprintId}")
    public ResponseEntity<ApiResponse<String>> unassignMemberFromSprint(
            @Parameter(description = "Team member ID", required = true)
            @PathVariable String memberId,
            @Parameter(description = "Sprint ID", required = true)
            @PathVariable String sprintId) {
        try {
            teamService.unassignMemberFromSprint(memberId, sprintId);
            return ResponseEntity.ok(
                    ApiResponse.success("Member unassigned from sprint successfully", null));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.failure(e.getMessage()));
            }
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("Failed to unassign member from sprint: " + e.getMessage()));
        }
    }
    
    /**
     * Get team members assigned to a sprint
     * GET /api/team-members/sprint/{sprintId}
     */
    @GetMapping("/sprint/{sprintId}")
    public ResponseEntity<ApiResponse<List<TeamMemberDto>>> getTeamMembersForSprint(
            @Parameter(description = "Sprint ID", required = true)
            @PathVariable String sprintId) {
        try {
            List<TeamMemberDto> members = teamService.getTeamMembersForSprint(sprintId);
            return ResponseEntity.ok(ApiResponse.success(members));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("Failed to retrieve team members for sprint: " + e.getMessage()));
        }
    }
    
    /**
     * Get member IDs assigned to a sprint
     * GET /api/team-members/sprint/{sprintId}/ids
     */
    @GetMapping("/sprint/{sprintId}/ids")
    public ResponseEntity<ApiResponse<List<String>>> getMemberIdsForSprint(
            @Parameter(description = "Sprint ID", required = true)
            @PathVariable String sprintId) {
        try {
            List<String> memberIds = teamService.getMemberIdsForSprint(sprintId);
            return ResponseEntity.ok(ApiResponse.success(memberIds));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("Failed to retrieve member IDs for sprint: " + e.getMessage()));
        }
    }
    
    /**
     * DTO for leave day request
     */
    public record LeaveDayRequest(String leaveDate) {
        public LeaveDayRequest {
            if (leaveDate == null || leaveDate.isBlank()) {
                throw new IllegalArgumentException("Leave date cannot be null or blank");
            }
        }
    }
    
    /**
     * Helper method to extract sprint ID from cookie
     */
    private String getSprintIdFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("currentSprintId".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}

