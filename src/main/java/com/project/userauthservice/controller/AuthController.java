package com.project.userauthservice.controller;

import com.project.userauthservice.dto.UserRegistrationDto;
import com.project.userauthservice.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {
    
    private final UserService userService;
    
    @GetMapping("/login")
    public String loginForm(@RequestParam(value = "error", required = false) String error,
                           @RequestParam(value = "message", required = false) String message,
                           @RequestParam(value = "logout", required = false) String logout,
                           @RequestParam(value = "expired", required = false) String expired,
                           Model model) {
        if (error != null) {
            if (message != null) {
                model.addAttribute("errorMessage", message);
            } else {
                model.addAttribute("errorMessage", "Đăng nhập thất bại");
            }
        }
        
        if (logout != null) {
            model.addAttribute("successMessage", "Bạn đã đăng xuất thành công");
        }
        
        if (expired != null) {
            model.addAttribute("errorMessage", "Phiên đăng nhập của bạn đã hết hạn. Vui lòng đăng nhập lại.");
        }
        
        return "auth/login";
    }
    
    @GetMapping("/register")
    public String registrationForm(Model model) {
        model.addAttribute("user", new UserRegistrationDto());
        return "auth/register";
    }
    
    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") UserRegistrationDto registrationDto,
                               BindingResult result, Model model) {
        // Check if passwords match
        if (!registrationDto.getPassword().equals(registrationDto.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.user", "Mật khẩu xác nhận không khớp");
        }
        
        if (result.hasErrors()) {
            return "auth/register";
        }
        
        try {
            userService.registerNewUser(registrationDto);
            return "redirect:/register?registered=true";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register";
        }
    }
    
    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam("token") String token, Model model) {
        try {
            userService.verifyEmail(token);
            model.addAttribute("successMessage", "Email đã được xác thực thành công!");
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
        }
        return "auth/email-verification-result";
    }
    
    @PostMapping("/resend-verification")
    public String resendVerification(@RequestParam("email") String email, 
                                   RedirectAttributes redirectAttributes) {
        try {
            userService.resendVerificationEmail(email);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Email xác thực đã được gửi lại. Vui lòng kiểm tra hộp thư của bạn.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/login";
    }
    
   @GetMapping("/dashboard")
public String dashboard(HttpServletRequest request, Model model) {
    model.addAttribute("currentPath", request.getRequestURI());
    return "dashboard";
}
}