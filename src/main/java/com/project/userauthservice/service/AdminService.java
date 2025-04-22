package com.project.userauthservice.service;

import com.project.userauthservice.entity.admin.AdminSettings;
import com.project.userauthservice.entity.subscription.PaymentTransaction;
import com.project.userauthservice.entity.subscription.SubscriptionPackage;
import com.project.userauthservice.entity.subscription.UserSubscription;
import com.project.userauthservice.entity.user.User;
import com.project.userauthservice.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {
    
    private final UserRepository userRepository;
    private final SubscriptionPackageRepository packageRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final AdminSettingsRepository adminSettingsRepository;
    
    // Quản lý người dùng
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
    
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
    }
    
    @Transactional
    public void toggleUserActive(Long userId) {
        User user = getUserById(userId);
        user.setActive(!user.isActive());
        userRepository.save(user);
    }
    
    @Transactional
    public void toggleUserLocked(Long userId) {
        User user = getUserById(userId);
        user.setLocked(!user.isLocked());
        userRepository.save(user);
    }
    
    // Quản lý gói đăng ký
    public List<SubscriptionPackage> getAllPackages() {
        return packageRepository.findAll();
    }
    
    public SubscriptionPackage getPackageById(Long id) {
        return packageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Gói đăng ký không tồn tại"));
    }
    
    @Transactional
    public SubscriptionPackage savePackage(SubscriptionPackage pkg) {
        return packageRepository.save(pkg);
    }
    
    @Transactional
    public void togglePackageActive(Long packageId) {
        SubscriptionPackage pkg = getPackageById(packageId);
        pkg.setActive(!pkg.isActive());
        packageRepository.save(pkg);
    }
    
    // Quản lý giao dịch
    public Page<PaymentTransaction> getAllTransactions(Pageable pageable) {
        return transactionRepository.findAll(pageable);
    }
    
    public PaymentTransaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Giao dịch không tồn tại"));
    }
    
    // Thống kê doanh thu
    public Map<String, Object> getRevenueStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // Doanh thu hôm nay
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime endOfToday = startOfToday.plusDays(1);
        double todayRevenue = transactionRepository.findAll().stream()
                .filter(t -> t.getTransactionDate().isAfter(startOfToday) && t.getTransactionDate().isBefore(endOfToday))
                .filter(t -> t.getStatus() == PaymentTransaction.TransactionStatus.COMPLETED)
                .mapToDouble(PaymentTransaction::getAmount)
                .sum();
        
        // Doanh thu tuần này
        LocalDateTime startOfWeek = LocalDate.now().atStartOfDay().minus(LocalDate.now().getDayOfWeek().getValue() - 1, ChronoUnit.DAYS);
        double weekRevenue = transactionRepository.findAll().stream()
                .filter(t -> t.getTransactionDate().isAfter(startOfWeek) && t.getTransactionDate().isBefore(endOfToday))
                .filter(t -> t.getStatus() == PaymentTransaction.TransactionStatus.COMPLETED)
                .mapToDouble(PaymentTransaction::getAmount)
                .sum();
        
        // Doanh thu tháng này
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        double monthRevenue = transactionRepository.findAll().stream()
                .filter(t -> t.getTransactionDate().isAfter(startOfMonth) && t.getTransactionDate().isBefore(endOfToday))
                .filter(t -> t.getStatus() == PaymentTransaction.TransactionStatus.COMPLETED)
                .mapToDouble(PaymentTransaction::getAmount)
                .sum();
        
        // Thống kê theo gói
        Map<String, Double> revenueByPackage = transactionRepository.findAll().stream()
                .filter(t -> t.getStatus() == PaymentTransaction.TransactionStatus.COMPLETED)
                .filter(t -> t.getUserSubscription() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getUserSubscription().getSubscriptionPackage().getName(),
                        Collectors.summingDouble(PaymentTransaction::getAmount)
                ));
        
        // Thống kê theo phương thức thanh toán
        Map<UserSubscription.PaymentMethod, Double> revenueByMethod = transactionRepository.findAll().stream()
                .filter(t -> t.getStatus() == PaymentTransaction.TransactionStatus.COMPLETED)
                .collect(Collectors.groupingBy(
                        PaymentTransaction::getPaymentMethod,
                        Collectors.summingDouble(PaymentTransaction::getAmount)
                ));
        
        stats.put("todayRevenue", todayRevenue);
        stats.put("weekRevenue", weekRevenue);
        stats.put("monthRevenue", monthRevenue);
        stats.put("revenueByPackage", revenueByPackage);
        stats.put("revenueByMethod", revenueByMethod);
        
        return stats;
    }
    
    // Quản lý cài đặt
    public String getSetting(String key, String defaultValue) {
        return adminSettingsRepository.findBySettingKey(key)
                .map(AdminSettings::getSettingValue)
                .orElse(defaultValue);
    }
    
    @Transactional
    public void saveSetting(String key, String value) {
        AdminSettings setting = adminSettingsRepository.findBySettingKey(key)
                .orElse(new AdminSettings());
        
        setting.setSettingKey(key);
        setting.setSettingValue(value);
        
        adminSettingsRepository.save(setting);
    }
    
    // Thống kê người dùng
    public Map<String, Object> getUserStats() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.findAll().stream()
                .filter(User::isActive)
                .count();
        
        Map<SubscriptionPackage.AccountLevel, Long> usersByLevel = userRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        User::getAccountLevel,
                        Collectors.counting()
                ));
        
        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("usersByLevel", usersByLevel);
        
        return stats;
    }
}