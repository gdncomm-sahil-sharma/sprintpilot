package com.sprintpilot.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DateUtils {
    
    /**
     * Calculate the number of working days between two dates, excluding weekends and holidays
     */
    public static int getWorkingDays(LocalDate start, LocalDate end, List<String> holidays) {
        int count = 0;
        LocalDate current = start;
        Set<LocalDate> holidaySet = parseHolidayDates(holidays);
        
        while (!current.isAfter(end)) {
            if (isWorkingDay(current, holidaySet)) {
                count++;
            }
            current = current.plusDays(1);
        }
        
        return count;
    }
    
    /**
     * Add a specified number of working days to a date
     */
    public static LocalDate addWorkingDays(LocalDate startDate, int days, List<String> holidays) {
        if (days <= 0) {
            return startDate;
        }
        
        LocalDate result = startDate;
        Set<LocalDate> holidaySet = parseHolidayDates(holidays);
        
        // First, find the next working day to start counting from
        while (!isWorkingDay(result, holidaySet)) {
            result = result.plusDays(1);
        }
        
        int addedDays = 0;
        while (addedDays < days - 1) { // -1 because we start from a working day
            result = result.plusDays(1);
            if (isWorkingDay(result, holidaySet)) {
                addedDays++;
            }
        }
        
        return result;
    }
    
    /**
     * Check if a date is a working day (not weekend and not holiday)
     */
    public static boolean isWorkingDay(LocalDate date, Set<LocalDate> holidays) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && 
               dayOfWeek != DayOfWeek.SUNDAY && 
               !holidays.contains(date);
    }
    
    /**
     * Check if a date is a weekend
     */
    public static boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }
    
    /**
     * Parse holiday dates from ISO date strings
     */
    private static Set<LocalDate> parseHolidayDates(List<String> holidays) {
        Set<LocalDate> holidaySet = new HashSet<>();
        if (holidays != null) {
            for (String holiday : holidays) {
                try {
                    holidaySet.add(LocalDate.parse(holiday));
                } catch (Exception e) {
                    // Skip invalid dates
                }
            }
        }
        return holidaySet;
    }
    
    /**
     * Get the next working day from a given date
     */
    public static LocalDate getNextWorkingDay(LocalDate date, List<String> holidays) {
        Set<LocalDate> holidaySet = parseHolidayDates(holidays);
        LocalDate nextDay = date.plusDays(1);
        
        while (!isWorkingDay(nextDay, holidaySet)) {
            nextDay = nextDay.plusDays(1);
        }
        
        return nextDay;
    }
    
    /**
     * Get the previous working day from a given date
     */
    public static LocalDate getPreviousWorkingDay(LocalDate date, List<String> holidays) {
        Set<LocalDate> holidaySet = parseHolidayDates(holidays);
        LocalDate prevDay = date.minusDays(1);
        
        while (!isWorkingDay(prevDay, holidaySet)) {
            prevDay = prevDay.minusDays(1);
        }
        
        return prevDay;
    }
    
    /**
     * Calculate sprint end date based on start date and duration in working days
     */
    public static LocalDate calculateSprintEndDate(LocalDate startDate, int durationInWorkingDays, 
                                                   List<String> holidays) {
        return addWorkingDays(startDate, durationInWorkingDays, holidays);
    }
    
    /**
     * Calculate code freeze date (typically 2 days before sprint end)
     */
    public static LocalDate calculateCodeFreezeDate(LocalDate endDate, int daysBefore, 
                                                    List<String> holidays) {
        Set<LocalDate> holidaySet = parseHolidayDates(holidays);
        LocalDate freezeDate = endDate;
        int daysCount = 0;
        
        while (daysCount < daysBefore) {
            freezeDate = freezeDate.minusDays(1);
            if (isWorkingDay(freezeDate, holidaySet)) {
                daysCount++;
            }
        }
        
        return freezeDate;
    }
}
