package com.project.userauthservice.dto;

import com.project.userauthservice.entity.payment.Subscription;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class PaymentRegistrationDto {
    @NotNull(message = "Vui lòng chọn gói đăng ký")
    private Subscription.SubscriptionType subscriptionType;

    @NotEmpty(message = "Vui lòng nhập tên chủ thẻ")
    private String cardHolderName;

    @NotEmpty(message = "Vui lòng nhập số thẻ")
    @Pattern(regexp = "^\\d{16}$", message = "Số thẻ phải là 16 chữ số")
    private String cardNumber;

    @NotEmpty(message = "Vui lòng nhập ngày hết hạn")
    @Pattern(regexp = "^(0[1-9]|1[0-2])/\\d{2}$", message = "Định dạng ngày hết hạn không hợp lệ (MM/YY)")
    private String expiryDate;

    @NotEmpty(message = "Vui lòng nhập CVV")
    @Pattern(regexp = "^\\d{3}$", message = "CVV phải là 3 chữ số")
    private String cvv;
}