package com.sprintpilot.controller;

import com.sprintpilot.dto.ApiResponse;
import com.sprintpilot.dto.HolidayDto;
import com.sprintpilot.service.HolidayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST Controller for holiday management operations
 */
@RestController
@RequestMapping("/api/holidays")
public class HolidayController {
    
    @Autowired
    private HolidayService holidayService;
    
    /**
     * Create a new holiday
     */
    @PostMapping
    public ResponseEntity<ApiResponse<HolidayDto>> createHoliday(@RequestBody HolidayDto holidayDto) {
        try {
            HolidayDto created = holidayService.createHoliday(holidayDto);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Holiday created successfully", created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Invalid holiday data", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to create holiday", e.getMessage()));
        }
    }
    
    /**
     * Update an existing holiday
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HolidayDto>> updateHoliday(
            @PathVariable String id,
            @RequestBody HolidayDto holidayDto) {
        try {
            HolidayDto updated = holidayService.updateHoliday(id, holidayDto);
            return ResponseEntity.ok(ApiResponse.success("Holiday updated successfully", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Invalid holiday data", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Holiday not found", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to update holiday", e.getMessage()));
        }
    }
    
    /**
     * Get holiday by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HolidayDto>> getHolidayById(@PathVariable String id) {
        try {
            HolidayDto holiday = holidayService.getHolidayById(id);
            return ResponseEntity.ok(ApiResponse.success(holiday));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Holiday not found", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to fetch holiday", e.getMessage()));
        }
    }
    
    /**
     * Get all holidays
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<HolidayDto>>> getAllHolidays(
            @RequestParam(required = false) String location) {
        try {
            List<HolidayDto> holidays;
            if (location != null && !location.isEmpty()) {
                holidays = holidayService.getHolidaysByLocation(location);
            } else {
                holidays = holidayService.getAllHolidays();
            }
            return ResponseEntity.ok(ApiResponse.success(holidays));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to fetch holidays", e.getMessage()));
        }
    }
    
    /**
     * Get holidays by date range
     */
    @GetMapping("/range")
    public ResponseEntity<ApiResponse<List<HolidayDto>>> getHolidaysByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String location) {
        try {
            List<HolidayDto> holidays;
            if (location != null && !location.isEmpty()) {
                holidays = holidayService.getHolidaysByDateRange(startDate, endDate, location);
            } else {
                holidays = holidayService.getHolidaysByDateRange(startDate, endDate);
            }
            return ResponseEntity.ok(ApiResponse.success(holidays));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to fetch holidays by date range", e.getMessage()));
        }
    }
    
    /**
     * Get holidays by year
     */
    @GetMapping("/year/{year}")
    public ResponseEntity<ApiResponse<List<HolidayDto>>> getHolidaysByYear(
            @PathVariable int year,
            @RequestParam(required = false) String location) {
        try {
            List<HolidayDto> holidays;
            if (location != null && !location.isEmpty()) {
                holidays = holidayService.getHolidaysByYear(year, location);
            } else {
                holidays = holidayService.getHolidaysByYear(year);
            }
            return ResponseEntity.ok(ApiResponse.success(holidays));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to fetch holidays by year", e.getMessage()));
        }
    }
    
    /**
     * Get all recurring holidays
     */
    @GetMapping("/recurring")
    public ResponseEntity<ApiResponse<List<HolidayDto>>> getRecurringHolidays(
            @RequestParam(required = false) String location) {
        try {
            List<HolidayDto> holidays;
            if (location != null && !location.isEmpty()) {
                holidays = holidayService.getRecurringHolidays(location);
            } else {
                holidays = holidayService.getRecurringHolidays();
            }
            return ResponseEntity.ok(ApiResponse.success(holidays));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to fetch recurring holidays", e.getMessage()));
        }
    }
    
    /**
     * Get holidays by location
     */
    @GetMapping("/location/{location}")
    public ResponseEntity<ApiResponse<List<HolidayDto>>> getHolidaysByLocation(@PathVariable String location) {
        try {
            List<HolidayDto> holidays = holidayService.getHolidaysByLocation(location);
            return ResponseEntity.ok(ApiResponse.success(holidays));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to fetch holidays by location", e.getMessage()));
        }
    }
    
    /**
     * Delete a holiday
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteHoliday(@PathVariable String id) {
        try {
            holidayService.deleteHoliday(id);
            return ResponseEntity.ok(ApiResponse.success("Holiday deleted successfully", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Holiday not found", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to delete holiday", e.getMessage()));
        }
    }
    
    /**
     * Get holiday dates for a sprint (returns list of date strings)
     */
    @GetMapping("/sprint")
    public ResponseEntity<ApiResponse<List<String>>> getHolidayDatesForSprint(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String location) {
        try {
            List<String> holidayDates;
            if (location != null && !location.isEmpty()) {
                holidayDates = holidayService.getHolidayDatesForSprint(startDate, endDate, location);
            } else {
                holidayDates = holidayService.getHolidayDatesForSprint(startDate, endDate);
            }
            return ResponseEntity.ok(ApiResponse.success(holidayDates));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to fetch holiday dates for sprint", e.getMessage()));
        }
    }
    
    /**
     * Check if a date is a holiday
     */
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Boolean>> isHoliday(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String location) {
        try {
            boolean isHoliday;
            if (location != null && !location.isEmpty()) {
                isHoliday = holidayService.isHoliday(date, location);
            } else {
                isHoliday = holidayService.isHoliday(date);
            }
            return ResponseEntity.ok(ApiResponse.success(isHoliday));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to check if date is holiday", e.getMessage()));
        }
    }
}

