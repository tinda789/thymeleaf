package com.project.userauthservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VNPayConfig {
    
    @Value("${vnpay.version}")
    private String version;
    
    @Value("${vnpay.tmn-code}")
    private String tmnCode;
    
    @Value("${vnpay.hash-secret}")
    private String hashSecret;
    
    @Value("${vnpay.url}")
    private String paymentUrl;
    
    @Value("${vnpay.return-url}")
    private String returnUrl;
    
    // Getters
    public String getVersion() {
        return version;
    }

    public String getTmnCode() {
        return tmnCode;
    }

    public String getHashSecret() {
        return hashSecret;
    }

    public String getPaymentUrl() {
        return paymentUrl;
    }

    public String getReturnUrl() {
        return returnUrl;
    }
}