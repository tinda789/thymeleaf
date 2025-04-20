package com.project.userauthservice.controller;

import com.project.userauthservice.dto.PaymentRegistrationDto;
import com.project.userauthservice.entity.payment.Payment;
import com.project.userauthservice.entity.payment.Subscription;
import com.project.userauthservice.repository.SubscriptionRepository;
import com.project.userauthservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@RequestMapping("/subscription")
@RequiredArgsConstructor
public class SubscriptionController {
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentService paymentService;

    @GetMapping
    public String subscriptionPage(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        List<Subscription> subscriptions = subscriptionRepository.findAll();
        model.addAttribute("subscriptions", subscriptions);
        model.addAttribute("paymentForm", new PaymentRegistrationDto());
        return "subscription/index";
    }

    @PostMapping("/register")
    public String registerPayment(
        @Valid @ModelAttribute("paymentForm") PaymentRegistrationDto paymentDto,
        BindingResult result,
        @AuthenticationPrincipal UserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        if (result.hasErrors()) {
            return "subscription/index";
        }

        try {
            Payment payment = paymentService.registerPayment(userDetails.getUsername(), paymentDto);
            redirectAttributes.addFlashAttribute("payment", payment);
            return "redirect:/subscription/confirm";
        } catch (Exception e) {
            result.rejectValue("cardNumber", "error.payment", e.getMessage());
            return "subscription/index";
        }
    }

    @GetMapping("/confirm")
    public String confirmPaymentPage() {
        return "subscription/confirm";
    }

    @PostMapping("/confirm")
    public String confirmPayment(
        @RequestParam String transactionCode,
        RedirectAttributes redirectAttributes
    ) {
        try {
            Payment payment = paymentService.confirmPayment(transactionCode);
            redirectAttributes.addFlashAttribute("successMessage", "Nâng cấp tài khoản thành công!");
            return "redirect:/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/subscription/confirm";
        }
    }
}