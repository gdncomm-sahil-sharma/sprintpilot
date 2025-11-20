package com.sprintpilot.repository;

import com.sprintpilot.entity.Task;
import com.sprintpilot.dto.TaskDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, String> {
    
    @EntityGraph(attributePaths = {"assignees", "sprint"})
    @Query("SELECT t FROM Task t WHERE t.id = :id")
    Optional<Task> findByIdWithDetails(@Param("id") String id);
    
    @EntityGraph(attributePaths = {"assignees", "sprint"})
    @Query("SELECT t FROM Task t WHERE t.sprint.id = :sprintId ORDER BY t.priority DESC, t.createdAt ASC")
    List<Task> findBySprintId(@Param("sprintId") String sprintId);
    
    @Query("SELECT t FROM Task t JOIN t.assignees a WHERE a.id = :memberId")
    List<Task> findByAssigneeId(@Param("memberId") String memberId);
    
    @Query("SELECT t FROM Task t WHERE t.sprint.id = :sprintId AND t.category = :category")
    List<Task> findBySprintIdAndCategory(@Param("sprintId") String sprintId, @Param("category") Task.TaskCategory category);
    
    @Query("SELECT t FROM Task t WHERE t.status = :status ORDER BY t.dueDate ASC")
    List<Task> findByStatus(@Param("status") Task.TaskStatus status);
    
    @Query("SELECT new com.sprintpilot.dto.TaskDto(t.id, t.sprint.id, t.taskKey, t.summary, t.description, " +
           "t.storyPoints, t.category, t.priority, t.status, t.startDate, t.dueDate, t.timeSpent, " +
           "(SELECT MIN(tm.id) FROM TeamMember tm JOIN tm.assignedTasks at WHERE at.id = t.id)) " +
           "FROM Task t WHERE t.sprint.id = :sprintId")
    List<TaskDto> findTaskDtosBySprintId(@Param("sprintId") String sprintId);
    
    @Query("SELECT COUNT(t) FROM Task t WHERE t.sprint.id = :sprintId AND t.status = 'DONE'")
    long countCompletedTasksBySprintId(@Param("sprintId") String sprintId);
    
    @Query("SELECT SUM(t.storyPoints) FROM Task t WHERE t.sprint.id = :sprintId")
    Double sumStoryPointsBySprintId(@Param("sprintId") String sprintId);
    
    @Query("SELECT t FROM Task t WHERE t.taskKey = :taskKey AND t.sprint.id = :sprintId")
    Optional<Task> findByTaskKeyAndSprintId(@Param("taskKey") String taskKey, @Param("sprintId") String sprintId);
    
    @Query("SELECT t FROM Task t WHERE t.sprint.id = :sprintId AND t.taskKey IN :taskKeys")
    List<Task> findBySprintIdAndTaskKeyIn(@Param("sprintId") String sprintId, @Param("taskKeys") List<String> taskKeys);
    
    /**
     * Find tasks by sprint ID and risk factor with pagination
     * Treats null riskFactor in DB as ON_TRACK
     * Sorts by assignee name (tasks with no assignees come last)
     * Excludes tasks with status DONE
     */
    @EntityGraph(attributePaths = {"assignees", "sprint"})
    @Query("SELECT t FROM Task t " +
           "WHERE t.sprint.id = :sprintId " +
           "AND t.status != 'DONE' " +
           "AND (:riskFactor IS NULL OR " +
           "     (t.riskFactor IS NULL AND :riskFactor = 'ON_TRACK') OR " +
           "     (STR(t.riskFactor) = :riskFactor)) " +
           "ORDER BY t.priority DESC, t.createdAt ASC")
    Page<Task> findBySprintIdAndRiskFactorPaginated(
            @Param("sprintId") String sprintId,
            @Param("riskFactor") String riskFactor,
            Pageable pageable
    );
    
    /**
     * Count tasks by sprint ID and risk factor
     * Treats null riskFactor in DB as ON_TRACK
     * Excludes tasks with status DONE
     */
    @Query("SELECT COUNT(t) FROM Task t " +
           "WHERE t.sprint.id = :sprintId " +
           "AND t.status != 'DONE' " +
           "AND (:riskFactor IS NULL OR " +
           "     (t.riskFactor IS NULL AND :riskFactor = 'ON_TRACK') OR " +
           "     (STR(t.riskFactor) = :riskFactor))")
    long countBySprintIdAndRiskFactor(
            @Param("sprintId") String sprintId,
            @Param("riskFactor") String riskFactor
    );
}
