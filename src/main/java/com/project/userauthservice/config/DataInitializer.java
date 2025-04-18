package com.project.userauthservice.config;

import com.project.userauthservice.entity.user.Role;
import com.project.userauthservice.repository.RoleRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer {

    @Autowired
    private RoleRepository roleRepository;

    @PostConstruct
    public void init() {
        initRoles();
    }

    private void initRoles() {
        if (roleRepository.count() == 0) {
            // Tạo các vai trò toàn cầu
            createRoleIfNotExists("ROLE_ADMIN", "Quản trị viên", Role.ScopeType.GLOBAL);
            createRoleIfNotExists("ROLE_USER", "Người dùng thông thường", Role.ScopeType.GLOBAL);

            // Tạo các vai trò dự án
            createRoleIfNotExists("PROJECT_ADMIN", "Quản trị viên dự án", Role.ScopeType.PROJECT);
            createRoleIfNotExists("PROJECT_MANAGER", "Quản lý dự án", Role.ScopeType.PROJECT);
            createRoleIfNotExists("DEVELOPER", "Nhà phát triển", Role.ScopeType.PROJECT);
            createRoleIfNotExists("TESTER", "Người kiểm thử", Role.ScopeType.PROJECT);
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