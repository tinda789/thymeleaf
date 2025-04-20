package com.project.userauthservice.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                      AuthenticationException exception) throws IOException, ServletException {
        String errorMessage;
        
        if (exception instanceof BadCredentialsException) {
            errorMessage = "Tên đăng nhập hoặc mật khẩu không chính xác";
        } else if (exception instanceof UsernameNotFoundException) {
            errorMessage = "Tài khoản không tồn tại";
        } else if (exception instanceof DisabledException) {
            errorMessage = "Tài khoản đã bị vô hiệu hóa";
        } else if (exception instanceof LockedException) {
            errorMessage = "Tài khoản đã bị khóa";
        } else {
            errorMessage = "Đăng nhập thất bại: " + exception.getMessage();
        }
        
        errorMessage = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
        setDefaultFailureUrl("/login?error=true&message=" + errorMessage);
        
        super.onAuthenticationFailure(request, response, exception);
    }
}