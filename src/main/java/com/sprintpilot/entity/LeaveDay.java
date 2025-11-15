package com.sprintpilot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_day")
@Data
public class LeaveDay {
    
    @Id
    @Column(name = "id")
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private TeamMember member;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private Sprint sprint;
    
    @Column(name = "leave_date", nullable = false)
    private LocalDate leaveDate;
    
    @Column(name = "leave_type")
    @Enumerated(EnumType.STRING)
    private LeaveType leaveType = LeaveType.PERSONAL;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (id == null) id = "leave-" + System.currentTimeMillis();
    }
    
    public enum LeaveType {
        PERSONAL,
        SICK,
        VACATION,
        PUBLIC_HOLIDAY,
        OTHER
    }
}
