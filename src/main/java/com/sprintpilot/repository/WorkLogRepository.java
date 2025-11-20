package com.sprintpilot.repository;

import com.sprintpilot.entity.WorkLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WorkLogRepository extends JpaRepository<WorkLog, String> {
    
    /**
     * Find all work logs for a specific task
     */
    List<WorkLog> findByTaskId(String taskId);
    
    /**
     * Find all work logs for tasks in a sprint, within a date range
     */
    @Query("SELECT wl FROM WorkLog wl WHERE wl.task.sprint.id = :sprintId " +
           "AND wl.loggedDate >= :startDate AND wl.loggedDate <= :endDate")
    List<WorkLog> findBySprintIdAndDateRange(
            @Param("sprintId") String sprintId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
    
    /**
     * Delete all work logs for a specific task
     */
    void deleteByTaskId(String taskId);
    
    /**
     * Sum time spent by date for a sprint (optimized aggregation query)
     * Returns pairs of [loggedDate, sumOfTimeSpentHours]
     */
    @Query("SELECT wl.loggedDate, SUM(wl.timeSpentHours) " +
           "FROM WorkLog wl " +
           "WHERE wl.task.sprint.id = :sprintId " +
           "AND wl.loggedDate >= :startDate AND wl.loggedDate <= :endDate " +
           "GROUP BY wl.loggedDate " +
           "ORDER BY wl.loggedDate")
    List<Object[]> sumTimeSpentByDateForSprint(
            @Param("sprintId") String sprintId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
