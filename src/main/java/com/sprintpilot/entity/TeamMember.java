package com.sprintpilot.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @Column(name = "location")
    private String location;
    
    @Column(name = "active")
    private Boolean active = true;
    
    @Column(name = "deleted")
    private Boolean deleted = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToMany(mappedBy = "teamMembers", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Sprint> sprints = new ArrayList<>();
    
    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<LeaveDay> leaveDays = new ArrayList<>();
    
    @ManyToMany(mappedBy = "assignees", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Task> assignedTasks = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (id == null) id = "member-" + UUID.randomUUID().toString();
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
