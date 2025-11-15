package com.sprintpilot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sprint_history")
@Data
public class SprintHistory {
    
    @Id
    @Column(name = "id")
    private String id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id", nullable = false)
    private Sprint sprint;
    
    @Column(name = "completed_date", nullable = false)
    private LocalDateTime completedDate;
    
    @Column(name = "total_story_points")
    private BigDecimal totalStoryPoints;
    
    @Column(name = "completed_story_points")
    private BigDecimal completedStoryPoints;
    
    @Column(name = "team_size")
    private Integer teamSize;
    
    @Column(name = "velocity")
    private BigDecimal velocity;
    
    @Column(name = "feature_percentage")
    private BigDecimal featurePercentage;
    
    @Column(name = "tech_debt_percentage")
    private BigDecimal techDebtPercentage;
    
    @Column(name = "prod_issue_percentage")
    private BigDecimal prodIssuePercentage;
    
    @Column(name = "summary_json", columnDefinition = "TEXT")
    private String summaryJson;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (id == null) id = "history-" + System.currentTimeMillis();
    }
}
