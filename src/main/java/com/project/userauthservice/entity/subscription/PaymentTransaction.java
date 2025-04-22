package com.project.userauthservice.entity.subscription;

import com.project.userauthservice.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "transaction_id", nullable = false, unique = true)
    private String transactionId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private UserSubscription userSubscription;
    
    @Column(nullable = false)
    private Double amount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private UserSubscription.PaymentMethod paymentMethod;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;
    
    @Column(name = "payment_details", columnDefinition = "TEXT")
    private String paymentDetails;
    
    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;
    
    public enum TransactionStatus {
        INITIATED, PENDING, COMPLETED, FAILED, REFUNDED
    }
    
    @PrePersist
    protected void onCreate() {
        if (transactionId == null) {
            transactionId = UUID.randomUUID().toString();
        }
        if (transactionDate == null) {
            transactionDate = LocalDateTime.now();
        }
    }
}