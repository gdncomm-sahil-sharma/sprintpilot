package com.sprintpilot.dto;

import com.sprintpilot.entity.Holiday;
import java.time.LocalDate;

public record HolidayDto(
        String id,
        String name,
        LocalDate holidayDate,
        Holiday.HolidayType holidayType,
        Boolean recurring
) {
    public HolidayDto {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Holiday name cannot be null or blank");
        }
        if (holidayDate == null) {
            throw new IllegalArgumentException("Holiday date cannot be null");
        }
        if (holidayType == null) {
            holidayType = Holiday.HolidayType.PUBLIC;
        }
        if (recurring == null) {
            recurring = false;
        }
    }
}
