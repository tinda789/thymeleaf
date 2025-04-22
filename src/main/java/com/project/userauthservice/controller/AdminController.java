package com.project.userauthservice.controller;

import com.project.userauthservice.entity.subscription.PaymentTransaction;
import com.project.userauthservice.entity.subscription.SubscriptionPackage;
import com.project.userauthservice.entity.user.User;
import com.project.userauthservice.service.AdminService;
import com.project.userauthservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ROLE_ADMIN')")
@RequiredArgsConstructor
public class AdminController {
    
    private final AdminService adminService;
    private final PaymentService paymentService;
    
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Thống kê doanh thu
        Map<String, Object> revenueStats = adminService.getRevenueStats();
        
        // Thống kê người dùng
        Map<String, Object> userStats = adminService.getUserStats();
        
        model.addAttribute("revenueStats", revenueStats);
        model.addAttribute("userStats", userStats);
        
        return "admin/dashboard";
    }
    
    // Quản lý người dùng
    @GetMapping("/users")
    public String listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Page<User> users = adminService.getAllUsers(PageRequest.of(page, size));
        
        model.addAttribute("users", users);
        model.addAttribute("currentPage", page);
        
        return "admin/users";
    }
    
    @GetMapping("/users/{id}")
    public String viewUser(@PathVariable Long id, Model model) {
        User user = adminService.getUserById(id);
        model.addAttribute("user", user);
        return "admin/user-detail";
    }
    
    @PostMapping("/users/{id}/toggle-active")
    public String toggleUserActive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminService.toggleUserActive(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái tài khoản");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }
    
    @PostMapping("/users/{id}/toggle-locked")
    public String toggleUserLocked(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        
        User user = adminService.getUserById(id);
        if (user.getUsername().equals("admin")) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể khóa tài khoản admin chính");
            return "redirect:/admin/users";
        }
       
        try {
            adminService.toggleUserLocked(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái khóa tài khoản");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }
    
    // Quản lý gói đăng ký
    @GetMapping("/packages")
    public String listPackages(Model model) {
        List<SubscriptionPackage> packages = adminService.getAllPackages();
        model.addAttribute("packages", packages);
        return "admin/packages";
    }
    
    @GetMapping("/packages/create")
    public String createPackageForm(Model model) {
        model.addAttribute("package", new SubscriptionPackage());
        model.addAttribute("accountLevels", SubscriptionPackage.AccountLevel.values());
        return "admin/package-form";
    }
    
    @PostMapping("/packages/create")
    public String createPackage(@ModelAttribute SubscriptionPackage pkg, RedirectAttributes redirectAttributes) {
        try {
            adminService.savePackage(pkg);
            redirectAttributes.addFlashAttribute("successMessage", "Gói đăng ký đã được tạo thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/packages";
    }
    
    @GetMapping("/packages/{id}/edit")
    public String editPackageForm(@PathVariable Long id, Model model) {
        SubscriptionPackage pkg = adminService.getPackageById(id);
        model.addAttribute("package", pkg);
        model.addAttribute("accountLevels", SubscriptionPackage.AccountLevel.values());
        return "admin/package-form";
    }
    
    @PostMapping("/packages/{id}/edit")
    public String updatePackage(@PathVariable Long id, @ModelAttribute SubscriptionPackage pkg, RedirectAttributes redirectAttributes) {
        try {
            pkg.setId(id);
            adminService.savePackage(pkg);
            redirectAttributes.addFlashAttribute("successMessage", "Gói đăng ký đã được cập nhật thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/packages";
    }
    
    @PostMapping("/packages/{id}/toggle-active")
    public String togglePackageActive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminService.togglePackageActive(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái gói đăng ký");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/packages";
    }
    
    // Quản lý giao dịch
    @GetMapping("/transactions")
    public String listTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        Page<PaymentTransaction> transactions = adminService.getAllTransactions(PageRequest.of(page, size));
        
        model.addAttribute("transactions", transactions);
        model.addAttribute("currentPage", page);
        
        return "admin/transactions";
    }
    
    @GetMapping("/transactions/{id}")
    public String viewTransaction(@PathVariable Long id, Model model) {
        PaymentTransaction transaction = adminService.getTransactionById(id);
        model.addAttribute("transaction", transaction);
        return "admin/transaction-detail";
    }
    
    @PostMapping("/transactions/{id}/verify-payment")
    public String verifyPayment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            PaymentTransaction transaction = adminService.getTransactionById(id);
            paymentService.handlePaymentCallback(transaction.getTransactionId(), true);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xác nhận thanh toán thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/transactions";
    }
    
    // Cài đặt hệ thống
    @GetMapping("/settings")
    public String showSettings(Model model) {
        String siteName = adminService.getSetting("site.name", "User Auth Service");
        String contactEmail = adminService.getSetting("contact.email", "support@example.com");
        String contactPhone = adminService.getSetting("contact.phone", "1900 xxxx");
        String paymentNote = adminService.getSetting("payment.note", "Lưu ý khi thanh toán");
        
        model.addAttribute("siteName", siteName);
        model.addAttribute("contactEmail", contactEmail);
        model.addAttribute("contactPhone", contactPhone);
        model.addAttribute("paymentNote", paymentNote);
        
        return "admin/settings";
    }
    
    @PostMapping("/settings")
    public String saveSettings(
            @RequestParam("siteName") String siteName,
            @RequestParam("contactEmail") String contactEmail,
            @RequestParam("contactPhone") String contactPhone,
            @RequestParam("paymentNote") String paymentNote,
            RedirectAttributes redirectAttributes) {
        try {
            adminService.saveSetting("site.name", siteName);
            adminService.saveSetting("contact.email", contactEmail);
            adminService.saveSetting("contact.phone", contactPhone);
            adminService.saveSetting("payment.note", paymentNote);
            
            redirectAttributes.addFlashAttribute("successMessage", "Cài đặt đã được lưu thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        
        return "redirect:/admin/settings";
    }
}