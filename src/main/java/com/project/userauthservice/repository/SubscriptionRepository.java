package com.project.userauthservice.repository;

import com.project.userauthservice.entity.payment.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByType(Subscription.SubscriptionType type);
}