package com.sprintpilot.repository;

import com.sprintpilot.entity.SprintTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Sprint Team operations
 * Manages the relationship between sprints and team members
 */
@Repository
public interface SprintTeamRepository extends JpaRepository<SprintTeam, String> {
    
    /**
     * Find all team member mappings for a specific sprint
     * @param sprintId The sprint ID
     * @return List of sprint-member mappings
     */
    @Query("SELECT s FROM SprintTeam s WHERE s.sprintId = :sprintId")
    List<SprintTeam> findBySprintId(@Param("sprintId") String sprintId);
    
    /**
     * Find all sprint mappings for a specific team member
     * @param memberId The member ID
     * @return List of sprint-member mappings
     */
    @Query("SELECT s FROM SprintTeam s WHERE s.memberId = :memberId")
    List<SprintTeam> findByMemberId(@Param("memberId") String memberId);
    
    /**
     * Find a specific sprint-member mapping
     * @param sprintId The sprint ID
     * @param memberId The member ID
     * @return The sprint-member mapping if found, null otherwise
     */
    @Query("SELECT s FROM SprintTeam s WHERE s.sprintId = :sprintId AND s.memberId = :memberId")
    SprintTeam findBySprintIdAndMemberId(@Param("sprintId") String sprintId, @Param("memberId") String memberId);
    
    /**
     * Delete all team member mappings for a specific sprint
     * @param sprintId The sprint ID
     */
    @Modifying
    @Query(value = "DELETE FROM sprint_team WHERE sprint_id = :sprintId", nativeQuery = true)
    void deleteBySprintId(@Param("sprintId") String sprintId);
    
    /**
     * Delete all sprint mappings for a specific team member
     * @param memberId The member ID
     */
    @Modifying
    @Query(value = "DELETE FROM sprint_team WHERE member_id = :memberId", nativeQuery = true)
    void deleteByMemberId(@Param("memberId") String memberId);
    
    /**
     * Get only the member IDs for a specific sprint
     * @param sprintId The sprint ID
     * @return List of member IDs
     */
    @Query("SELECT s.memberId FROM SprintTeam s WHERE s.sprintId = :sprintId")
    List<String> findMemberIdsBySprintId(@Param("sprintId") String sprintId);
    
    /**
     * Get only the sprint IDs for a specific team member
     * @param memberId The member ID
     * @return List of sprint IDs
     */
    @Query("SELECT s.sprintId FROM SprintTeam s WHERE s.memberId = :memberId")
    List<String> findSprintIdsByMemberId(@Param("memberId") String memberId);
    
    /**
     * Check if a team member is assigned to a specific sprint
     * @param sprintId The sprint ID
     * @param memberId The member ID
     * @return true if the member is assigned to the sprint, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM SprintTeam s WHERE s.sprintId = :sprintId AND s.memberId = :memberId")
    boolean existsBySprintIdAndMemberId(@Param("sprintId") String sprintId, @Param("memberId") String memberId);
    
    /**
     * Count the number of team members in a specific sprint
     * @param sprintId The sprint ID
     * @return The count of team members
     */
    @Query("SELECT COUNT(s) FROM SprintTeam s WHERE s.sprintId = :sprintId")
    long countBySprintId(@Param("sprintId") String sprintId);
    
    /**
     * Count the number of sprints a team member is assigned to
     * @param memberId The member ID
     * @return The count of sprints
     */
    @Query("SELECT COUNT(s) FROM SprintTeam s WHERE s.memberId = :memberId")
    long countByMemberId(@Param("memberId") String memberId);
}

