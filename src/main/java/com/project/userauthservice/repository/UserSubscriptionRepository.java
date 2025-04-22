package com.project.userauthservice.repository;

import com.project.userauthservice.entity.subscription.UserSubscription;
import com.project.userauthservice.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {
    List<UserSubscription> findByUserOrderByCreatedAtDesc(User user);
    Optional<UserSubscription> findByUserAndExpiryDateAfterAndPaymentStatus(
            User user, 
            LocalDateTime now, 
            UserSubscription.PaymentStatus status);
}