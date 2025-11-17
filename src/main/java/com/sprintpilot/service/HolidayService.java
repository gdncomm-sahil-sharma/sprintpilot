package com.sprintpilot.service;

import com.sprintpilot.dto.HolidayDto;
import java.time.LocalDate;
import java.util.List;

public interface HolidayService {
    
    HolidayDto createHoliday(HolidayDto holidayDto);
    
    HolidayDto updateHoliday(String id, HolidayDto holidayDto);
    
    HolidayDto getHolidayById(String id);
    
    List<HolidayDto> getAllHolidays();
    
    List<HolidayDto> getHolidaysByDateRange(LocalDate startDate, LocalDate endDate);
    
    List<HolidayDto> getHolidaysByDateRange(LocalDate startDate, LocalDate endDate, String location);
    
    List<HolidayDto> getHolidaysByYear(int year);
    
    List<HolidayDto> getHolidaysByYear(int year, String location);
    
    List<HolidayDto> getRecurringHolidays();
    
    List<HolidayDto> getRecurringHolidays(String location);
    
    List<HolidayDto> getHolidaysByLocation(String location);
    
    void deleteHoliday(String id);
    
    List<String> getHolidayDatesForSprint(LocalDate startDate, LocalDate endDate);
    
    List<String> getHolidayDatesForSprint(LocalDate startDate, LocalDate endDate, String location);
    
    boolean isHoliday(LocalDate date);
    
    boolean isHoliday(LocalDate date, String location);
}
