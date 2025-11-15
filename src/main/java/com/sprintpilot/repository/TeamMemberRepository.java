package com.sprintpilot.repository;

import com.sprintpilot.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, String> {
    
    @Query("SELECT t FROM TeamMember t WHERE t.active = true ORDER BY t.name")
    List<TeamMember> findActiveMembers();
    
    @Query("SELECT t FROM TeamMember t WHERE t.role = :role AND t.active = true")
    List<TeamMember> findByRole(@Param("role") TeamMember.Role role);
    
    Optional<TeamMember> findByEmail(String email);
    
    @EntityGraph(attributePaths = {"leaveDays", "assignedTasks"})
    @Query("SELECT t FROM TeamMember t WHERE t.id = :id")
    Optional<TeamMember> findByIdWithDetails(@Param("id") String id);
    
    @Query("SELECT t FROM TeamMember t WHERE t.name LIKE %:name%")
    List<TeamMember> searchByName(@Param("name") String name);
    
    @Query("SELECT DISTINCT t FROM TeamMember t JOIN t.sprints s WHERE s.id = :sprintId")
    List<TeamMember> findBySprintId(@Param("sprintId") String sprintId);
    
    @Query("SELECT COUNT(t) FROM TeamMember t WHERE t.active = true")
    long countActiveMembers();
}
