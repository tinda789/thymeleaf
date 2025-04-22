// entity/project/Project.java
package com.project.userauthservice.entity.project;

import com.project.userauthservice.entity.workspace.Workspace;
import com.project.userauthservice.entity.task.Task;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "projects")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @Column(name = "project_key", unique = true, length = 10)
    private String projectKey;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProjectStatus status = ProjectStatus.ACTIVE;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;    // Đặt giá trị mặc định là true
    
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Task> tasks = new HashSet<>();
    
    public enum ProjectStatus {
        ACTIVE, COMPLETED, ON_HOLD, CANCELLED
    }

    
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (active == false) {
            active = true; // Đảm bảo active luôn là true khi tạo mới
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
}