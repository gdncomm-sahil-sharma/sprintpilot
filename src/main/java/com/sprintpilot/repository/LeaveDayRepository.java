package com.sprintpilot.repository;

import com.sprintpilot.entity.LeaveDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveDayRepository extends JpaRepository<LeaveDay, String> {
    
    @Query("SELECT l FROM LeaveDay l WHERE l.member.id = :memberId ORDER BY l.leaveDate")
    List<LeaveDay> findByMemberId(@Param("memberId") String memberId);
    
    @Query("SELECT l FROM LeaveDay l WHERE l.member.id = :memberId AND l.leaveDate = :date")
    Optional<LeaveDay> findByMemberIdAndDate(@Param("memberId") Long memberId, @Param("date") LocalDate date);
    
    @Query("SELECT l FROM LeaveDay l WHERE l.member.id = :memberId AND l.leaveDate BETWEEN :startDate AND :endDate")
    List<LeaveDay> findByMemberIdAndDateRange(
        @Param("memberId") String memberId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    @Query("SELECT l FROM LeaveDay l WHERE l.sprint.id = :sprintId")
    List<LeaveDay> findBySprintId(@Param("sprintId") String sprintId);
    
    @Query("SELECT l FROM LeaveDay l WHERE l.leaveDate = :date")
    List<LeaveDay> findByDate(@Param("date") LocalDate date);
    
    @Modifying
    @Query(value = "DELETE FROM leave_day WHERE member_id = :memberId", nativeQuery = true)
    void deleteByMemberId(@Param("memberId") String memberId);
}

