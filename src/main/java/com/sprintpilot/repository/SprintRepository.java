package com.sprintpilot.repository;

import com.sprintpilot.entity.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, String> {
    
    @EntityGraph(attributePaths = {"teamMembers", "events", "tasks"})
    @Query("SELECT s FROM Sprint s WHERE s.id = :id")
    Optional<Sprint> findByIdWithDetails(@Param("id") String id);
    
    @Query("SELECT s FROM Sprint s WHERE s.status = :status ORDER BY s.startDate DESC")
    List<Sprint> findByStatus(@Param("status") Sprint.SprintStatus status);
    
    @Query("SELECT s FROM Sprint s WHERE s.startDate <= :date AND s.endDate >= :date")
    Optional<Sprint> findActiveSprintByDate(@Param("date") LocalDate date);
    
    @Query("SELECT s FROM Sprint s WHERE s.status IN ('ACTIVE', 'PLANNING') ORDER BY s.startDate ASC")
    List<Sprint> findActiveSprints();
    
    @Query("SELECT s FROM Sprint s WHERE s.status = 'COMPLETED' ORDER BY s.endDate DESC")
    List<Sprint> findCompletedSprints();
    
    @Query("SELECT COUNT(s) FROM Sprint s WHERE s.status = :status")
    long countByStatus(@Param("status") Sprint.SprintStatus status);
    
    @EntityGraph(attributePaths = {"teamMembers", "events", "tasks", "tasks.assignees"})
    @Query("SELECT s FROM Sprint s WHERE s.id = :id")
    Optional<Sprint> findByIdWithFullDetails(@Param("id") String id);

    @EntityGraph(attributePaths = {"teamMembers"})
    @Query("SELECT s FROM Sprint s WHERE s.id = :id")
    Optional<Sprint> findByIdWithTeamMembers(@Param("id") String id);
    
    /**
     * Finds sprint IDs and start dates for sprint number calculation
     * Only fetches id, startDate, and createdAt to avoid lazy loading issues
     */
    @Query("SELECT s.id, s.startDate, s.createdAt FROM Sprint s WHERE s.startDate IS NOT NULL")
    List<Object[]> findAllSprintDates();
}
