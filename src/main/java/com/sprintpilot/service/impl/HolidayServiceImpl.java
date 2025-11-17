package com.sprintpilot.service.impl;

import com.sprintpilot.dto.HolidayDto;
import com.sprintpilot.entity.Holiday;
import com.sprintpilot.repository.HolidayRepository;
import com.sprintpilot.service.HolidayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HolidayServiceImpl implements HolidayService {
    
    @Autowired
    private HolidayRepository holidayRepository;
    
    @Override
    @Transactional
    public HolidayDto createHoliday(HolidayDto holidayDto) {
        Holiday holiday = dtoToEntity(holidayDto);
        Holiday savedHoliday = holidayRepository.save(holiday);
        return entityToDto(savedHoliday);
    }
    
    @Override
    @Transactional
    public HolidayDto updateHoliday(String id, HolidayDto holidayDto) {
        Holiday existingHoliday = holidayRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Holiday not found with id: " + id));
        
        existingHoliday.setName(holidayDto.name());
        existingHoliday.setHolidayDate(holidayDto.holidayDate());
        existingHoliday.setHolidayType(holidayDto.holidayType());
        existingHoliday.setRecurring(holidayDto.recurring());
        existingHoliday.setLocation(holidayDto.location());
        
        Holiday updatedHoliday = holidayRepository.save(existingHoliday);
        return entityToDto(updatedHoliday);
    }
    
    @Override
    public HolidayDto getHolidayById(String id) {
        Holiday holiday = holidayRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Holiday not found with id: " + id));
        return entityToDto(holiday);
    }
    
    @Override
    public List<HolidayDto> getAllHolidays() {
        return holidayRepository.findAll().stream()
            .map(this::entityToDto)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<HolidayDto> getHolidaysByDateRange(LocalDate startDate, LocalDate endDate) {
        return holidayRepository.findByDateRange(startDate, endDate).stream()
            .map(this::entityToDto)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<HolidayDto> getHolidaysByDateRange(LocalDate startDate, LocalDate endDate, String location) {
        return filterByLocation(
            holidayRepository.findByDateRange(startDate, endDate),
            location
        ).stream()
            .map(this::entityToDto)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<HolidayDto> getHolidaysByYear(int year) {
        return holidayRepository.findByYear(year).stream()
            .map(this::entityToDto)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<HolidayDto> getHolidaysByYear(int year, String location) {
        return filterByLocation(
            holidayRepository.findByYear(year),
            location
        ).stream()
            .map(this::entityToDto)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<HolidayDto> getRecurringHolidays() {
        return holidayRepository.findRecurringHolidays().stream()
            .map(this::entityToDto)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<HolidayDto> getRecurringHolidays(String location) {
        return filterByLocation(
            holidayRepository.findRecurringHolidays(),
            location
        ).stream()
            .map(this::entityToDto)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<HolidayDto> getHolidaysByLocation(String location) {
        return holidayRepository.findAll().stream()
            .filter(h -> h.getLocation() != null && h.getLocation().contains(location))
            .map(this::entityToDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Helper method to filter holidays by location
     * Returns holidays where location is null (global) or contains the specified location
     */
    private List<Holiday> filterByLocation(List<Holiday> holidays, String location) {
        if (location == null || location.isEmpty()) {
            return holidays;
        }
        return holidays.stream()
            .filter(h -> {
                List<String> locations = h.getLocation();
                // Include global holidays (location is null) or holidays that contain the specified location
                return locations == null || locations.contains(location);
            })
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public void deleteHoliday(String id) {
        if (!holidayRepository.existsById(id)) {
            throw new RuntimeException("Holiday not found with id: " + id);
        }
        holidayRepository.deleteById(id);
    }
    
    @Override
    public List<String> getHolidayDatesForSprint(LocalDate startDate, LocalDate endDate) {
        return getHolidayDatesForSprint(startDate, endDate, null);
    }
    
    @Override
    public List<String> getHolidayDatesForSprint(LocalDate startDate, LocalDate endDate, String location) {
        // Get all holidays in the date range (including location-specific and global)
        List<LocalDate> holidayDates = filterByLocation(
            holidayRepository.findByDateRange(startDate, endDate),
            location
        ).stream()
            .map(Holiday::getHolidayDate)
            .collect(Collectors.toList());
        
        // Also check recurring holidays - if they fall within the date range for any year
        List<Holiday> recurringHolidays = filterByLocation(
            holidayRepository.findRecurringHolidays(),
            location
        );
        for (Holiday recurringHoliday : recurringHolidays) {
            LocalDate holidayDate = recurringHoliday.getHolidayDate();
            // Check if the month/day of the recurring holiday falls within the date range
            // by checking each year in the range
            int startYear = startDate.getYear();
            int endYear = endDate.getYear();
            
            for (int year = startYear; year <= endYear; year++) {
                LocalDate recurringDate = LocalDate.of(year, holidayDate.getMonth(), holidayDate.getDayOfMonth());
                if (!recurringDate.isBefore(startDate) && !recurringDate.isAfter(endDate)) {
                    // Check if this date is not already in the list
                    if (!holidayDates.contains(recurringDate)) {
                        holidayDates.add(recurringDate);
                    }
                }
            }
        }
        
        return holidayDates.stream()
            .map(LocalDate::toString)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }
    
    @Override
    public boolean isHoliday(LocalDate date) {
        return isHoliday(date, null);
    }
    
    @Override
    public boolean isHoliday(LocalDate date, String location) {
        // Check for exact date match (including location-specific and global)
        List<Holiday> exactMatches = filterByLocation(
            holidayRepository.findByDateRange(date, date),
            location
        );
        if (!exactMatches.isEmpty()) {
            return true;
        }
        
        // Check recurring holidays - if month and day match
        List<Holiday> recurringHolidays = filterByLocation(
            holidayRepository.findRecurringHolidays(),
            location
        );
        return recurringHolidays.stream()
            .anyMatch(holiday -> {
                LocalDate holidayDate = holiday.getHolidayDate();
                return holidayDate.getMonth() == date.getMonth() && 
                       holidayDate.getDayOfMonth() == date.getDayOfMonth();
            });
    }
    
    private HolidayDto entityToDto(Holiday holiday) {
        return new HolidayDto(
            holiday.getId(),
            holiday.getName(),
            holiday.getHolidayDate(),
            holiday.getHolidayType(),
            holiday.getRecurring(),
            holiday.getLocation()
        );
    }
    
    private Holiday dtoToEntity(HolidayDto dto) {
        Holiday holiday = new Holiday();
        holiday.setId(dto.id());
        holiday.setName(dto.name());
        holiday.setHolidayDate(dto.holidayDate());
        holiday.setHolidayType(dto.holidayType());
        holiday.setRecurring(dto.recurring());
        holiday.setLocation(dto.location());
        return holiday;
    }
}

