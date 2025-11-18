package com.sprintpilot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sprint")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sprint {
    
    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "name", nullable = false)
    private String sprintName;
    
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    
    @Column(name = "duration", nullable = false)
    private Integer duration;
    
    @Column(name = "freeze_date")
    private LocalDate freezeDate;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private SprintStatus status = SprintStatus.ACTIVE;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "sprint", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<SprintEvent> events = new ArrayList<>();
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "sprint_team",
        joinColumns = @JoinColumn(name = "sprint_id"),
        inverseJoinColumns = @JoinColumn(name = "member_id")
    )
    @ToString.Exclude
    private List<TeamMember> teamMembers = new ArrayList<>();
    
    @OneToMany(mappedBy = "sprint", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Task> tasks = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (id == null) id = "sprint-" + UUID.randomUUID().toString();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Sprint Status Lifecycle:
     * - ACTIVE: Current sprint in progress (default)
     * - ARCHIVED: Historical sprint for reference and analytics
     */
    public enum SprintStatus {
        ACTIVE,
        ARCHIVED
    }
}
