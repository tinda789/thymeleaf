package com.project.userauthservice.service;

import com.project.userauthservice.entity.subscription.PaymentTransaction;
import com.project.userauthservice.entity.subscription.SubscriptionPackage;
import com.project.userauthservice.entity.subscription.UserSubscription;
import com.project.userauthservice.entity.user.User;
import com.project.userauthservice.repository.PaymentTransactionRepository;
import com.project.userauthservice.repository.SubscriptionPackageRepository;
import com.project.userauthservice.repository.UserRepository;
import com.project.userauthservice.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubscriptionService {
    
    private final SubscriptionPackageRepository packageRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    
    public List<SubscriptionPackage> getAllActivePackages() {
        return packageRepository.findByActiveTrue();
    }
    
    @Transactional
    public UserSubscription createSubscription(User user, Long packageId, 
                                              UserSubscription.PaymentMethod paymentMethod) {
        SubscriptionPackage pkg = packageRepository.findByIdAndActiveTrue(packageId)
                .orElseThrow(() -> new RuntimeException("Gói dịch vụ không tồn tại hoặc không khả dụng"));
        
        // Kiểm tra gói đăng ký hiện tại
        Optional<UserSubscription> existingSubscription = 
            subscriptionRepository.findByUserAndExpiryDateAfterAndPaymentStatus(
                user, 
                LocalDateTime.now(), 
                UserSubscription.PaymentStatus.COMPLETED
            );
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryDate;
        double amountPaid = pkg.getPrice();
        
        if (existingSubscription.isPresent()) {
            UserSubscription currentSub = existingSubscription.get();
            
            // Lấy thứ tự các gói
            List<SubscriptionPackage.AccountLevel> levels = Arrays.asList(
                SubscriptionPackage.AccountLevel.FREE,
                SubscriptionPackage.AccountLevel.STANDARD,
                SubscriptionPackage.AccountLevel.PREMIUM,
                SubscriptionPackage.AccountLevel.VIP
            );
            
            int currentIndex = levels.indexOf(currentSub.getSubscriptionPackage().getLevel());
            int newIndex = levels.indexOf(pkg.getLevel());
            
            // Nếu gói mới cao hơn hoặc bằng gói hiện tại
            if (newIndex >= currentIndex) {
                // Cộng thêm thời gian của gói mới
                expiryDate = currentSub.getExpiryDate().plusDays(pkg.getDurationDays());
                
                // Tổng số tiền của cả hai gói
                amountPaid += currentSub.getAmountPaid();
                
                // Xóa gói cũ để tránh trùng lặp
                subscriptionRepository.delete(currentSub);
            } else {
                // Không cho phép hạ cấp gói
                throw new RuntimeException("Không thể đăng ký gói thấp hơn. Vui lòng chờ hết hạn gói hiện tại.");
            }
        } else {
            // Nếu chưa có gói đăng ký
            expiryDate = now.plusDays(pkg.getDurationDays());
        }
        
        UserSubscription subscription = UserSubscription.builder()
                .user(user)
                .subscriptionPackage(pkg)
                .purchaseDate(now)
                .expiryDate(expiryDate)
                .amountPaid(amountPaid)
                .paymentStatus(UserSubscription.PaymentStatus.PENDING)
                .paymentMethod(paymentMethod)
                .build();
        
        return subscriptionRepository.save(subscription);
    }
    
    @Transactional
    public PaymentTransaction initiatePayment(UserSubscription subscription) {
        PaymentTransaction transaction = PaymentTransaction.builder()
                .user(subscription.getUser())
                .userSubscription(subscription)
                .amount(subscription.getAmountPaid())
                .paymentMethod(subscription.getPaymentMethod())
                .status(PaymentTransaction.TransactionStatus.INITIATED)
                .build();
        
        return transactionRepository.save(transaction);
    }
    
    @Transactional
    public void completePayment(String transactionId, boolean success) {
        PaymentTransaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Giao dịch không tồn tại"));
        
        UserSubscription subscription = transaction.getUserSubscription();
        User user = subscription.getUser();
        
        if (success) {
            // Cập nhật trạng thái giao dịch
            transaction.setStatus(PaymentTransaction.TransactionStatus.COMPLETED);
            
            // Cập nhật trạng thái đăng ký
            subscription.setPaymentStatus(UserSubscription.PaymentStatus.COMPLETED);
            
            // Cập nhật level người dùng
            user.setAccountLevel(subscription.getSubscriptionPackage().getLevel());
            user.setAccountLevelExpiry(subscription.getExpiryDate());
            
            userRepository.save(user);
        } else {
            transaction.setStatus(PaymentTransaction.TransactionStatus.FAILED);
            subscription.setPaymentStatus(UserSubscription.PaymentStatus.FAILED);
        }
        
        transactionRepository.save(transaction);
        subscriptionRepository.save(subscription);
    }
    
    public List<UserSubscription> getUserSubscriptions(User user) {
        return subscriptionRepository.findByUserOrderByCreatedAtDesc(user);
    }
    
    public UserSubscription getCurrentSubscription(User user) {
        return subscriptionRepository.findByUserAndExpiryDateAfterAndPaymentStatus(
                user, LocalDateTime.now(), UserSubscription.PaymentStatus.COMPLETED)
                .orElse(null);
    }
    
    @Scheduled(cron = "0 0 0 * * ?") // Chạy hàng ngày lúc 00:00:00
    public void checkExpiredSubscriptions() {
        List<User> users = userRepository.findByAccountLevelExpiryBefore(LocalDateTime.now());
        
        for (User user : users) {
            user.setAccountLevel(SubscriptionPackage.AccountLevel.FREE);
            user.setAccountLevelExpiry(null);
        }
        
        userRepository.saveAll(users);
    }
}