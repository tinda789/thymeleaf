package com.project.userauthservice.controller;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordChangeForm {
    @NotEmpty(message = "Vui lòng nhập mật khẩu hiện tại")
    private String currentPassword;
    
    @NotEmpty(message = "Vui lòng nhập mật khẩu mới")
    @Size(min = 6, message = "Mật khẩu mới phải có ít nhất 6 ký tự")
    private String newPassword;
    
    @NotEmpty(message = "Vui lòng xác nhận mật khẩu mới")
    private String confirmPassword;
}