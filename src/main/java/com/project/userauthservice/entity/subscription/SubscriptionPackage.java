package com.project.userauthservice.entity.subscription;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_packages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPackage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false, length = 500)
    private String description;
    
    @Column(nullable = false)
    private Double price;
    
    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountLevel level;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(nullable = false)
    private boolean active = true;
    
    public enum AccountLevel {
        FREE, STANDARD, PREMIUM, VIP
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}