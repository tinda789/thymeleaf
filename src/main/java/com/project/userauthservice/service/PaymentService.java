package com.project.userauthservice.service;

import com.project.userauthservice.dto.PaymentRegistrationDto;
import com.project.userauthservice.entity.payment.Payment;
import com.project.userauthservice.entity.payment.Subscription;
import com.project.userauthservice.entity.user.User;
import com.project.userauthservice.repository.PaymentRepository;
import com.project.userauthservice.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final UserService userService;

    @Transactional
    public Payment registerPayment(String username, PaymentRegistrationDto paymentDto) {
        validateCardDetails(paymentDto);

        Subscription subscription = subscriptionRepository.findByType(paymentDto.getSubscriptionType())
            .orElseThrow(() -> new RuntimeException("Gói đăng ký không tồn tại"));

        User user = userService.findByUsername(username);

        Payment payment = Payment.builder()
            .user(user)
            .subscription(subscription)
            .amount(subscription.getPrice())
            .paymentDate(LocalDateTime.now())
            .status(Payment.PaymentStatus.PENDING)
            .transactionCode(generateTransactionCode())
            .build();

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment confirmPayment(String transactionCode) {
        Payment payment = paymentRepository.findByTransactionCode(transactionCode)
            .orElseThrow(() -> new RuntimeException("Giao dịch không tồn tại"));

        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        upgradeUserSubscription(payment.getUser(), payment.getSubscription());

        return paymentRepository.save(payment);
    }

    private void validateCardDetails(PaymentRegistrationDto paymentDto) {
        if (!isValidCreditCard(paymentDto.getCardNumber())) {
            throw new RuntimeException("Số thẻ không hợp lệ");
        }

        if (isExpiredCard(paymentDto.getExpiryDate())) {
            throw new RuntimeException("Thẻ đã hết hạn");
        }
    }

    private void upgradeUserSubscription(User user, Subscription subscription) {
        user.setSubscriptionType(subscription.getType());
        user.setMaxWorkspaces(subscription.getMaxWorkspaces());
        userService.saveUser(user);
    }

    private String generateTransactionCode() {
        return "TRX" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private boolean isValidCreditCard(String cardNumber) {
        return cardNumber.matches("^\\d{16}$");
    }

    private boolean isExpiredCard(String expiryDate) {
        try {
            LocalDate expiry = LocalDate.parse("20" + expiryDate.split("/")[1] + "-" + 
                                                expiryDate.split("/")[0] + "-01");
            return expiry.isBefore(LocalDate.now());
        } catch (Exception e) {
            return true;
        }
    }
}