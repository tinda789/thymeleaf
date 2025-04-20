package com.project.userauthservice.entity.payment;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    @Enumerated(EnumType.STRING)
    private SubscriptionType type;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private int maxWorkspaces;

    @Column(nullable = false)
    private int maxMembers;

    private String description;

    public enum SubscriptionType {
        FREE,       // Mặc định
        BASIC,      // Trả phí cơ bản
        PREMIUM,    // Trả phí nâng cao
        ENTERPRISE  // Dành cho doanh nghiệp
    }
}