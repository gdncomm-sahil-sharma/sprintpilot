package com.sprintpilot.repository;

import com.sprintpilot.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, String> {
    
    @Query("SELECT h FROM Holiday h WHERE h.holidayDate BETWEEN :startDate AND :endDate ORDER BY h.holidayDate")
    List<Holiday> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT h FROM Holiday h WHERE h.holidayType = :type ORDER BY h.holidayDate")
    List<Holiday> findByType(@Param("type") Holiday.HolidayType type);
    
    @Query("SELECT h FROM Holiday h WHERE EXTRACT(YEAR FROM h.holidayDate) = :year ORDER BY h.holidayDate")
    List<Holiday> findByYear(@Param("year") int year);
    
    @Query("SELECT h FROM Holiday h WHERE h.recurring = true")
    List<Holiday> findRecurringHolidays();
    
    @Query("SELECT COUNT(h) FROM Holiday h WHERE h.holidayDate BETWEEN :startDate AND :endDate")
    long countHolidaysInRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
