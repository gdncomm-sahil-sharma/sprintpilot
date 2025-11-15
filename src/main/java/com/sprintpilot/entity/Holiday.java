package com.sprintpilot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "holiday")
@Data
public class Holiday {
    
    @Id
    @Column(name = "id")
    private String id;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;
    
    @Column(name = "holiday_type")
    @Enumerated(EnumType.STRING)
    private HolidayType holidayType = HolidayType.PUBLIC;
    
    @Column(name = "recurring")
    private Boolean recurring = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (id == null) id = "holiday-" + System.currentTimeMillis();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum HolidayType {
        PUBLIC,
        COMPANY
    }
}
