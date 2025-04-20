package com.project.userauthservice.controller;

import com.project.userauthservice.dto.UserProfileUpdateDto;
import com.project.userauthservice.entity.user.User;
import com.project.userauthservice.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
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
    public String viewProfile(@AuthenticationPrincipal UserDetails userDetails, 
                            Model model,
                            HttpServletRequest request) {
        // Thêm currentPath vào model trước khi xử lý
        model.addAttribute("currentPath", request.getRequestURI());
        
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
                              RedirectAttributes redirectAttributes,
                              HttpServletRequest request) {  // Thêm request nếu cần currentPath ở POST
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
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        
        return "redirect:/profile";
    }

    @GetMapping("/change-password")
    public String changePasswordForm(Model model, 
                                   HttpServletRequest request) {  // Thêm request
        model.addAttribute("currentPath", request.getRequestURI());  // Thêm currentPath
        model.addAttribute("passwordForm", new PasswordChangeForm());
        return "profile/change-password";
    }

    @PostMapping("/change-password")
public String changePassword(@Valid @ModelAttribute("passwordForm") PasswordChangeForm form,
                           BindingResult result,
                           @AuthenticationPrincipal UserDetails userDetails,
                           RedirectAttributes redirectAttributes,
                           HttpServletRequest request,
                           Model model) {  // Thêm Model vào tham số
    if (result.hasErrors()) {
        model.addAttribute("currentPath", request.getRequestURI());
        return "profile/change-password";
    }

    if (!form.getNewPassword().equals(form.getConfirmPassword())) {
        result.rejectValue("confirmPassword", "password.mismatch", "Mật khẩu xác nhận không khớp");
        model.addAttribute("currentPath", request.getRequestURI());
        return "profile/change-password";
    }

    try {
        userService.changePassword(userDetails.getUsername(), form.getCurrentPassword(), form.getNewPassword());
        redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công!");
        return "redirect:/profile";
    } catch (Exception e) {
        result.rejectValue("currentPassword", "password.incorrect", e.getMessage());
        model.addAttribute("currentPath", request.getRequestURI());
        return "profile/change-password";
    }
}
}