// DataInitializer.java
package com.project.userauthservice.config;

import com.project.userauthservice.entity.user.Role;
import com.project.userauthservice.repository.RoleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer {
    
    private final RoleRepository roleRepository;
    
    @PostConstruct
    public void init() {
        initRoles();
    }
    
    private void initRoles() {
        if (roleRepository.count() == 0) {
            // Create global roles
            createRoleIfNotExists("ROLE_ADMIN", "Administrator", Role.ScopeType.GLOBAL);
            createRoleIfNotExists("ROLE_USER", "Regular User", Role.ScopeType.GLOBAL);
            
            // Create project roles
            createRoleIfNotExists("PROJECT_ADMIN", "Project Administrator", Role.ScopeType.PROJECT);
            createRoleIfNotExists("PROJECT_MANAGER", "Project Manager", Role.ScopeType.PROJECT);
            createRoleIfNotExists("DEVELOPER", "Developer", Role.ScopeType.PROJECT);
            createRoleIfNotExists("TESTER", "Tester", Role.ScopeType.PROJECT);
        }
    }
    
    private void createRoleIfNotExists(String name, String description, Role.ScopeType scopeType) {
        if (roleRepository.findByName(name).isEmpty()) {
            Role role = new Role();
            role.setName(name);
            role.setDescription(description);
            role.setScopeType(scopeType);
            roleRepository.save(role);
        }
    }
}