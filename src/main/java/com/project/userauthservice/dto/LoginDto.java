package com.project.userauthservice.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class LoginDto {
    @NotEmpty(message = "Username is required")
    private String username;

    @NotEmpty(message = "Password is required")
    private String password;

    private boolean rememberMe;
}