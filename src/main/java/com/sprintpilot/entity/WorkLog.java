package com.sprintpilot.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a work log entry from Jira
 * Used specifically for burndown chart calculations
 */
@Entity
@Table(name = "work_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkLog {
    
    @Id
    @Column(name = "id", length = 100)
    private String id; // Jira worklog ID
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;
    
    @Column(name = "time_spent_hours", precision = 10, scale = 2)
    private BigDecimal timeSpentHours;
    
    @Column(name = "logged_date")
    private LocalDate loggedDate; // The date when work was logged
    
    @Column(name = "author", length = 255)
    private String author; // Who logged the work
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
