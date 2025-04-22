package com.project.userauthservice.config;

import com.project.userauthservice.entity.subscription.SubscriptionPackage;
import com.project.userauthservice.entity.user.Role;
import com.project.userauthservice.entity.user.User;
import com.project.userauthservice.entity.user.UserRole;
import com.project.userauthservice.repository.RoleRepository;
import com.project.userauthservice.repository.SubscriptionPackageRepository;
import com.project.userauthservice.repository.UserRepository;
import com.project.userauthservice.repository.UserRoleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class DataInitializer {
    
    private final RoleRepository roleRepository;
    private final SubscriptionPackageRepository packageRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    
    @PostConstruct
    public void init() {
        initRoles();
        initAdminUser();
        initSubscriptionPackages();
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
    
    private void initAdminUser() {
        if (userRepository.findByUsername("admin").isEmpty()) {
            // Tạo tài khoản admin
            User adminUser = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .email("admin@example.com")
                    .fullName("Administrator")
                    .active(true)
                    .emailVerified(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .accountLevel(SubscriptionPackage.AccountLevel.VIP)
                    .build();
            
            userRepository.save(adminUser);
            
            // Gán vai trò ADMIN cho tài khoản admin
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new RuntimeException("Admin role not found"));
            
            UserRole userRole = UserRole.builder()
                    .user(adminUser)
                    .role(adminRole)
                    .build();
            
            userRoleRepository.save(userRole);
            
            System.out.println("Created admin user with username: admin and password: admin123");
        }
    }
    
    private void initSubscriptionPackages() {
        if (packageRepository.count() == 0) {
            // Gói Standard
            SubscriptionPackage standard = SubscriptionPackage.builder()
                    .name("Standard")
                    .description("Gói tiêu chuẩn phù hợp với cá nhân và nhóm nhỏ")
                    .price(100000.0)  // 100,000 VNĐ
                    .durationDays(30)
                    .level(SubscriptionPackage.AccountLevel.STANDARD)
                    .active(true)
                    .build();
            
            // Gói Premium
            SubscriptionPackage premium = SubscriptionPackage.builder()
                    .name("Premium")
                    .description("Gói cao cấp với nhiều tính năng dành cho team chuyên nghiệp")
                    .price(300000.0)  // 300,000 VNĐ
                    .durationDays(30)
                    .level(SubscriptionPackage.AccountLevel.PREMIUM)
                    .active(true)
                    .build();
            
            // Gói VIP
            SubscriptionPackage vip = SubscriptionPackage.builder()
                    .name("VIP")
                    .description("Gói VIP không giới hạn với đầy đủ quyền và hỗ trợ 24/7")
                    .price(500000.0)  // 500,000 VNĐ
                    .durationDays(30)
                    .level(SubscriptionPackage.AccountLevel.VIP)
                    .active(true)
                    .build();
            
            packageRepository.saveAll(Arrays.asList(standard, premium, vip));
        }
    }
}