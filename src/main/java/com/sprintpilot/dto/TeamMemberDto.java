package com.sprintpilot.dto;

import com.sprintpilot.entity.TeamMember;
import java.math.BigDecimal;
import java.util.List;

public record TeamMemberDto(
        String id,
        String name,
        TeamMember.Role role,
        BigDecimal dailyCapacity,
        String email,
        Boolean active,
        List<String> leaveDays,
        Boolean assignedToCurrentSprint
) {
    public TeamMemberDto {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }
        if (dailyCapacity == null || dailyCapacity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Daily capacity must be positive");
        }
        if (active == null) {
            active = true;
        }
        if (leaveDays == null) {
            leaveDays = List.of();
        }
    }
}
