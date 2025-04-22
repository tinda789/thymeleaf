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
            
            // Nếu là cùng một gói
            if (currentSub.getSubscriptionPackage().getId().equals(packageId)) {
                // Cộng thêm thời gian của gói mới vào ngày hết hạn hiện tại
                expiryDate = currentSub.getExpiryDate().plusDays(pkg.getDurationDays());
                
                // Tổng số tiền là tổng của hai gói
                amountPaid += currentSub.getAmountPaid();
            } 
            // Nếu là gói khác
            else {
                // Lấy thứ tự các gói
                List<SubscriptionPackage.AccountLevel> levels = Arrays.asList(
                    SubscriptionPackage.AccountLevel.FREE,
                    SubscriptionPackage.AccountLevel.STANDARD,
                    SubscriptionPackage.AccountLevel.PREMIUM,
                    SubscriptionPackage.AccountLevel.VIP
                );
                
                int currentIndex = levels.indexOf(currentSub.getSubscriptionPackage().getLevel());
                int newIndex = levels.indexOf(pkg.getLevel());
                
                // Nếu gói mới cao hơn gói hiện tại
                if (newIndex > currentIndex) {
                    // Tính số ngày còn lại của gói hiện tại
                    long remainingDays = ChronoUnit.DAYS.between(LocalDateTime.now(), currentSub.getExpiryDate());
                    
                    // Tính giá trị còn lại của gói hiện tại
                    double remainingValue = (currentSub.getAmountPaid() / currentSub.getSubscriptionPackage().getDurationDays()) * remainingDays;
                    
                    // Tổng số tiền sẽ là phần chênh lệch của gói mới
                    amountPaid = pkg.getPrice() - remainingValue;
                    
                    // Đặt ngày hết hạn mới
                    expiryDate = LocalDateTime.now().plusDays(pkg.getDurationDays());
                } 
                // Nếu không được phép nâng cấp
                else {
                    throw new RuntimeException("Không thể đăng ký gói này. Vui lòng nâng cấp theo trình tự.");
                }
            }
        } 
        // Nếu chưa có gói đăng ký
        else {
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