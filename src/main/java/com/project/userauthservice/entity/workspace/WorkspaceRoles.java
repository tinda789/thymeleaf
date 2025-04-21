package com.project.userauthservice.entity.workspace;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "workspace_roles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceRoles {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @Column(name = "permission_level")
    private Integer permissionLevel;
    
    @Column(nullable = false)
    private boolean active = true;
}