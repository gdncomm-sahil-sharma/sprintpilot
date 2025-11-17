package com.sprintpilot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "sprint_team")
@Data
public class SprintTeam {
    
    @Id
    @Column(name = "id")
    private String id;
    
    @Column(name = "sprint_id", nullable = false)
    private String sprintId;
    
    @Column(name = "member_id", nullable = false)
    private String memberId;
    
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;
    
    @Column(name = "assigned_by")
    private String assignedBy;
    
    @PrePersist
    protected void onCreate() {
        if (assignedAt == null) assignedAt = LocalDateTime.now();
        if (id == null) id = "spm-" + System.currentTimeMillis() + "-" + Math.random();
    }
}

