package com.sprintpilot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "task")
@Data
public class Task {
    
    @Id
    @Column(name = "id")
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id", nullable = false)
    private Sprint sprint;
    
    @Column(name = "task_key", nullable = false)
    private String taskKey;
    
    @Column(name = "summary", nullable = false, length = 500)
    private String summary;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "story_points", nullable = false)
    private BigDecimal storyPoints = BigDecimal.ZERO;
    
    @Column(name = "category", nullable = false)
    @Enumerated(EnumType.STRING)
    private TaskCategory category;
    
    @Column(name = "priority")
    @Enumerated(EnumType.STRING)
    private TaskPriority priority = TaskPriority.MEDIUM;
    
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private TaskStatus status = TaskStatus.TODO;
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "due_date")
    private LocalDate dueDate;
    
    @Column(name = "time_spent")
    private BigDecimal timeSpent = BigDecimal.ZERO;

    @Column(name = "original_estimate")
    private BigDecimal originalEstimate = BigDecimal.ZERO;
    
    @Column(name = "risk_factor")
    @Enumerated(EnumType.STRING)
    private RiskFactor riskFactor;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "task_assignment",
        joinColumns = @JoinColumn(name = "task_id"),
        inverseJoinColumns = @JoinColumn(name = "member_id")
    )
    private List<TeamMember> assignees = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (id == null) id = "task-" + UUID.randomUUID().toString();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum TaskCategory {
        FEATURE,
        TECH_DEBT,
        PROD_ISSUE,
        OTHER
    }
    
    public enum TaskPriority {
        LOWEST,
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
    
    public enum TaskStatus {
        TODO,
        IN_PROGRESS,
        IN_REVIEW,
        DONE
    }
    
    public enum RiskFactor {
        ON_TRACK,
        AT_RISK,
        OFF_TRACK
    }
}
