package com.project.userauthservice.controller;

import com.project.userauthservice.entity.subscription.PaymentTransaction;
import com.project.userauthservice.entity.subscription.SubscriptionPackage;
import com.project.userauthservice.entity.subscription.UserSubscription;
import com.project.userauthservice.entity.user.User;
import com.project.userauthservice.service.PaymentService;
import com.project.userauthservice.service.SubscriptionService;
import com.project.userauthservice.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.text.DecimalFormat;
import java.util.List;

@Controller
@RequestMapping("/subscription")
@RequiredArgsConstructor
public class SubscriptionController {
    
    private final SubscriptionService subscriptionService;
    private final PaymentService paymentService;
    private final UserService userService;
    
    @GetMapping
    public String viewSubscriptions(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        
        List<SubscriptionPackage> packages = subscriptionService.getAllActivePackages();
        List<UserSubscription> subscriptions = subscriptionService.getUserSubscriptions(user);
        
        model.addAttribute("packages", packages);
        model.addAttribute("subscriptions", subscriptions);
        
        return "subscription/index";
    }
    
    @PostMapping("/{packageId}")
    public String subscribeToPackage(@PathVariable Long packageId,
                                   @RequestParam UserSubscription.PaymentMethod paymentMethod,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   RedirectAttributes redirectAttributes,
                                   Model model) {
        try {
            User user = userService.findByUsername(userDetails.getUsername());
            
            // Tạo đăng ký
            UserSubscription subscription = subscriptionService.createSubscription(user, packageId, paymentMethod);
            
            // Khởi tạo giao dịch thanh toán
            PaymentTransaction transaction = subscriptionService.initiatePayment(subscription);
            
            // Thêm thông báo nếu có chênh lệch giá
            if (Math.abs(subscription.getAmountPaid() - subscription.getSubscriptionPackage().getPrice()) > 0.01) {
                redirectAttributes.addFlashAttribute("infoMessage", 
                    "Bạn sẽ thanh toán " + 
                    new DecimalFormat("#,###").format(subscription.getAmountPaid()) + 
                    " VNĐ để " + 
                    (subscription.getAmountPaid() > subscription.getSubscriptionPackage().getPrice() ? 
                    "gia hạn" : "nâng cấp") + " gói.");
            }
            
            // Chuyển hướng đến trang thanh toán tương ứng
            String redirectUrl;
            switch (paymentMethod) {
                case VNPAY:
                    // Lấy URL thanh toán VNPay
                    redirectUrl = paymentService.processVNPayPayment(transaction);
                    
                    // Sử dụng trang chuyển hướng trung gian
                    model.addAttribute("paymentUrl", redirectUrl);
                    model.addAttribute("amount", transaction.getAmount());
                    model.addAttribute("packageName", subscription.getSubscriptionPackage().getName());
                    model.addAttribute("transactionId", transaction.getTransactionId());
                    return "subscription/direct-vnpay";
                    
                case MOMO:
                    redirectUrl = paymentService.processMoMoPayment(transaction);
                    return "redirect:" + redirectUrl;
                    
                case BANK_TRANSFER:
                    // Hiển thị thông tin chuyển khoản
                    String bankInfo = paymentService.processBankTransfer(transaction);
                    redirectAttributes.addFlashAttribute("bankInfo", bankInfo);
                    redirectAttributes.addFlashAttribute("transactionId", transaction.getTransactionId());
                    return "redirect:/subscription/bank-transfer";
                    
                case CREDIT_CARD:
                    // Hiển thị form nhập thông tin thẻ
                    redirectUrl = paymentService.processCreditCard(transaction);
                    return "redirect:" + redirectUrl;
                    
                default:
                    redirectAttributes.addFlashAttribute("errorMessage", "Phương thức thanh toán không hợp lệ");
                    return "redirect:/subscription";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/subscription";
        }
    }
    
    @GetMapping("/bank-transfer")
    public String showBankTransferInfo() {
        return "subscription/bank-transfer";
    }
    
    @GetMapping("/card-payment")
    public String showCardPaymentForm(@RequestParam String transactionId, Model model) {
        model.addAttribute("transactionId", transactionId);
        return "subscription/card-payment";
    }
    
    @PostMapping("/process-card-payment")
    public String processCardPayment(@RequestParam String transactionId, 
                                   @RequestParam String cardNumber,
                                   @RequestParam String cardName,
                                   @RequestParam String expiry,
                                   @RequestParam String cvv,
                                   RedirectAttributes redirectAttributes) {
        // Giả lập xử lý thanh toán thẻ luôn thành công
        paymentService.handlePaymentCallback(transactionId, true);
        
        redirectAttributes.addFlashAttribute("successMessage", "Thanh toán thành công!");
        return "redirect:/subscription";
    }
    
    @GetMapping("/vnpay-callback")
    public String handleVNPayCallback(
            @RequestParam(required = false) String vnp_TxnRef,
            @RequestParam(required = false) String vnp_ResponseCode,
            @RequestParam(required = false) String vnp_SecureHash,
            HttpServletRequest request,
            Model model) {
        
        // Kiểm tra có đủ tham số không
        if (vnp_TxnRef == null || vnp_ResponseCode == null) {
            model.addAttribute("success", false);
            model.addAttribute("errorMessage", "Thiếu tham số từ VNPAY");
            return "subscription/payment-result";
        }
        
        // Kiểm tra mã giao dịch hợp lệ
        boolean success = "00".equals(vnp_ResponseCode);
        
        // Xử lý kết quả thanh toán
        try {
            paymentService.handlePaymentCallback(vnp_TxnRef, success);
            
            model.addAttribute("success", success);
            model.addAttribute("transactionId", vnp_TxnRef);
            model.addAttribute("vnpResponseCode", vnp_ResponseCode);
            
            if (!success) {
                model.addAttribute("errorMessage", "Thanh toán thất bại! Mã lỗi: " + vnp_ResponseCode);
            }
        } catch (Exception e) {
            model.addAttribute("success", false);
            model.addAttribute("errorMessage", "Lỗi xử lý giao dịch: " + e.getMessage());
        }
        
        return "subscription/payment-result";
    }
    
    @GetMapping("/momo-callback")
    public String handleMoMoCallback(@RequestParam String orderId,
                                   @RequestParam String resultCode,
                                   RedirectAttributes redirectAttributes) {
        boolean success = "0".equals(resultCode);
        
        paymentService.handlePaymentCallback(orderId, success);
        
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Thanh toán thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Thanh toán thất bại!");
        }
        
        return "redirect:/subscription";
    }
    
    @PostMapping("/verify-bank-transfer")
    public String verifyBankTransfer(@RequestParam String transactionId,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        // Admin sẽ phải xác minh giao dịch chuyển khoản thủ công
        // Ở đây giả lập là tự động xác nhận thành công
        if (userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            paymentService.handlePaymentCallback(transactionId, true);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xác nhận giao dịch thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền xác nhận giao dịch!");
        }
        
        return "redirect:/subscription";
    }
}