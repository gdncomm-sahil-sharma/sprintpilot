package com.sprintpilot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "team_member")
@Data
public class TeamMember {
    
    @Id
    @Column(name = "id")
    private String id;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;
    
    @Column(name = "daily_capacity", nullable = false)
    private BigDecimal dailyCapacity = new BigDecimal("6.0");
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "active")
    private Boolean active = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToMany(mappedBy = "teamMembers", fetch = FetchType.LAZY)
    private List<Sprint> sprints = new ArrayList<>();
    
    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    private List<LeaveDay> leaveDays = new ArrayList<>();
    
    @ManyToMany(mappedBy = "assignees", fetch = FetchType.LAZY)
    private List<Task> assignedTasks = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (id == null) id = "member-" + System.currentTimeMillis();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum Role {
        BACKEND,
        FRONTEND,
        QA,
        DEVOPS,
        MANAGER,
        DESIGNER
    }
}
