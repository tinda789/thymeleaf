package com.project.userauthservice.repository;

import com.project.userauthservice.entity.subscription.PaymentTransaction;
import com.project.userauthservice.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    List<PaymentTransaction> findByUserOrderByTransactionDateDesc(User user);
    Optional<PaymentTransaction> findByTransactionId(String transactionId);
    List<PaymentTransaction> findByStatusAndTransactionDateBefore(
            PaymentTransaction.TransactionStatus status, LocalDateTime date);
}