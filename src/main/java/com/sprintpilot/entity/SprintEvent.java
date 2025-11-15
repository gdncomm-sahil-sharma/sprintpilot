package com.sprintpilot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "sprint_event")
@Data
public class SprintEvent {
    
    @Id
    @Column(name = "id")
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id", nullable = false)
    private Sprint sprint;
    
    @Column(name = "event_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private EventType eventType;
    
    @Column(name = "event_subtype")
    @Enumerated(EnumType.STRING)
    private MeetingType eventSubtype;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;
    
    @Column(name = "event_time")
    private LocalTime eventTime;
    
    @Column(name = "duration_minutes")
    private Integer durationMinutes;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (id == null) id = "event-" + System.currentTimeMillis();
    }
    
    public enum EventType {
        DEPLOYMENT,
        MEETING,
        HOLIDAY
    }
    
    public enum MeetingType {
        PLANNING,
        GROOMING,
        RETROSPECTIVE
    }
}
