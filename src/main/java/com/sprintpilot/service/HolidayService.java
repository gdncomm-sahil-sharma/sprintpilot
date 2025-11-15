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
    
    List<HolidayDto> getHolidaysByYear(int year);
    
    List<HolidayDto> getRecurringHolidays();
    
    void deleteHoliday(String id);
    
    List<String> getHolidayDatesForSprint(LocalDate startDate, LocalDate endDate);
    
    boolean isHoliday(LocalDate date);
}
