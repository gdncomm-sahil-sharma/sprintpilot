package com.sprintpilot.repository;

import com.sprintpilot.entity.SprintEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface SprintEventRepository extends JpaRepository<SprintEvent, String> {
    
    @Query("SELECT e FROM SprintEvent e WHERE e.sprint.id = :sprintId ORDER BY e.eventDate, e.eventTime")
    List<SprintEvent> findBySprintId(@Param("sprintId") String sprintId);
    
    @Query("SELECT e FROM SprintEvent e WHERE e.eventType = :type AND e.sprint.id = :sprintId")
    List<SprintEvent> findBySprintIdAndType(@Param("sprintId") String sprintId, @Param("type") SprintEvent.EventType type);
    
    @Query("SELECT e FROM SprintEvent e WHERE e.eventDate BETWEEN :startDate AND :endDate ORDER BY e.eventDate")
    List<SprintEvent> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT e FROM SprintEvent e WHERE e.eventType = 'MEETING' AND e.eventSubtype = :meetingType")
    List<SprintEvent> findByMeetingType(@Param("meetingType") SprintEvent.MeetingType meetingType);
}
