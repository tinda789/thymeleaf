package com.project.userauthservice.config;

import com.project.userauthservice.entity.payment.Subscription;
import com.project.userauthservice.entity.user.Role;
import com.project.userauthservice.repository.RoleRepository;
import com.project.userauthservice.repository.SubscriptionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class DataInitializer {
    
    private final RoleRepository roleRepository;
    private final SubscriptionRepository subscriptionRepository;
    
    @PostConstruct
    public void init() {
        initRoles();
        initSubscriptions();
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
    
    private void initSubscriptions() {
        if (subscriptionRepository.count() == 0) {
            Subscription freeSubscription = Subscription.builder()
                .type(Subscription.SubscriptionType.FREE)
                .price(BigDecimal.ZERO)
                .maxWorkspaces(1)
                .maxMembers(5)
                .description("Gói miễn phí cho người dùng mới")
                .build();

            Subscription basicSubscription = Subscription.builder()
                .type(Subscription.SubscriptionType.BASIC)
                .price(new BigDecimal("99000")) // 99,000 VND/tháng
                .maxWorkspaces(5)
                .maxMembers(20)
                .description("Gói cơ bản cho nhóm nhỏ - Tối đa 5 workspace, 20 thành viên")
                .build();

            Subscription premiumSubscription = Subscription.builder()
                .type(Subscription.SubscriptionType.PREMIUM)
                .price(new BigDecimal("199000")) // 199,000 VND/tháng
                .maxWorkspaces(10)
                .maxMembers(50)
                .description("Gói nâng cao cho doanh nghiệp - Tối đa 10 workspace, 50 thành viên")
                .build();

            Subscription enterpriseSubscription = Subscription.builder()
                .type(Subscription.SubscriptionType.ENTERPRISE)
                .price(new BigDecimal("499000")) // 499,000 VND/tháng
                .maxWorkspaces(50)
                .maxMembers(200)
                .description("Gói doanh nghiệp - Tối đa 50 workspace, 200 thành viên")
                .build();

            subscriptionRepository.saveAll(Arrays.asList(
                freeSubscription, 
                basicSubscription, 
                premiumSubscription,
                enterpriseSubscription
            ));
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