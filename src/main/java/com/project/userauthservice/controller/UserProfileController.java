package com.project.userauthservice.controller;

import com.project.userauthservice.dto.UserProfileUpdateDto;
import com.project.userauthservice.entity.user.User;
import com.project.userauthservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserService userService;

    @GetMapping
    public String viewProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        UserProfileUpdateDto profileDto = new UserProfileUpdateDto();
        
        // Map user entity to DTO
        profileDto.setFullName(user.getFullName());
        profileDto.setEmail(user.getEmail());
        profileDto.setPhoneNumber(user.getPhoneNumber());
        profileDto.setDateOfBirth(user.getDateOfBirth());
        profileDto.setGender(user.getGender());
        
        model.addAttribute("userProfile", profileDto);
        model.addAttribute("user", user);
        return "profile/index";
    }

    @PostMapping("/update")
    public String updateProfile(@Valid @ModelAttribute("userProfile") UserProfileUpdateDto profileDto,
                              BindingResult result,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "profile/index";
        }

        try {
            userService.updateProfile(userDetails.getUsername(), profileDto);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        
        return "redirect:/profile";
    }
    

    @PostMapping("/update-avatar")
    public String updateAvatar(@RequestParam("avatar") MultipartFile file,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        try {
            userService.updateAvatar(userDetails.getUsername(), file);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật ảnh đại diện thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật ảnh đại diện: " + e.getMessage());
        }
        
        return "redirect:/profile";
    }

    @GetMapping("/change-password")
    public String changePasswordForm(Model model) {
        model.addAttribute("passwordForm", new PasswordChangeForm());
        return "profile/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@Valid @ModelAttribute("passwordForm") PasswordChangeForm form,
                               BindingResult result,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        // Kiểm tra validation errors
        if (result.hasErrors()) {
            return "profile/change-password";
        }
    
        // Kiểm tra mật khẩu mới và xác nhận mật khẩu
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "password.mismatch", "Mật khẩu xác nhận không khớp");
            return "profile/change-password";
        }
    
        try {
            userService.changePassword(userDetails.getUsername(), form.getCurrentPassword(), form.getNewPassword());
            redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công!");
            return "redirect:/profile";
        } catch (Exception e) {
            result.rejectValue("currentPassword", "password.incorrect", e.getMessage());
            return "profile/change-password";
        }
    }
}