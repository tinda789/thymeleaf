package com.project.userauthservice.service;

import com.project.userauthservice.entity.subscription.PaymentTransaction;
import com.project.userauthservice.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {
    
    private final PaymentTransactionRepository transactionRepository;
    private final SubscriptionService subscriptionService;
    
    public String processVNPayPayment(PaymentTransaction transaction) {
        // Giả lập thanh toán VNPAY
        String vnpUrl = simulateVNPayPaymentURL(transaction);
        return vnpUrl;
    }
    
    public String processMoMoPayment(PaymentTransaction transaction) {
        // Giả lập thanh toán MoMo
        String momoUrl = simulateMoMoPaymentURL(transaction);
        return momoUrl;
    }
    
    public String processBankTransfer(PaymentTransaction transaction) {
        // Giả lập thanh toán chuyển khoản ngân hàng
        String bankInfo = simulateBankTransferInfo(transaction);
        return bankInfo;
    }
    
    public String processCreditCard(PaymentTransaction transaction) {
        // Giả lập thanh toán thẻ tín dụng
        String redirectUrl = simulateCreditCardPayment(transaction);
        return redirectUrl;
    }
    
    private String simulateVNPayPaymentURL(PaymentTransaction transaction) {
        // Trong thực tế: Tạo URL để chuyển hướng đến cổng thanh toán VNPAY
        // Ở đây: Giả lập URL callback với kết quả luôn thành công
        
        String baseUrl = "http://localhost:8081/subscription/vnpay-callback";
        String params = "?vnp_TxnRef=" + transaction.getTransactionId() + 
                       "&vnp_Amount=" + (int)(transaction.getAmount() * 100) +
                       "&vnp_ResponseCode=00"; // 00 = thành công
        
        // Lưu thêm thông tin vào transaction
        transaction.setPaymentDetails("VNPay transaction initiated");
        transaction.setStatus(PaymentTransaction.TransactionStatus.PENDING);
        transactionRepository.save(transaction);
        
        return baseUrl + params;
    }
    
    private String simulateMoMoPaymentURL(PaymentTransaction transaction) {
        // Tương tự như VNPAY
        String baseUrl = "http://localhost:8081/subscription/momo-callback";
        String params = "?orderId=" + transaction.getTransactionId() + 
                       "&amount=" + transaction.getAmount() +
                       "&resultCode=0"; // 0 = thành công
        
        transaction.setPaymentDetails("MoMo transaction initiated");
        transaction.setStatus(PaymentTransaction.TransactionStatus.PENDING);
        transactionRepository.save(transaction);
        
        return baseUrl + params;
    }
    
    private String simulateBankTransferInfo(PaymentTransaction transaction) {
        // Thông tin chuyển khoản
        String bankInfo = "Ngân hàng: BIDV\n" +
                         "Số tài khoản: 123456789\n" +
                         "Chủ tài khoản: CÔNG TY TNHH PROJECT\n" +
                         "Số tiền: " + transaction.getAmount() + " VND\n" +
                         "Nội dung chuyển khoản: " + transaction.getTransactionId();
        
        transaction.setPaymentDetails(bankInfo);
        transaction.setStatus(PaymentTransaction.TransactionStatus.PENDING);
        transactionRepository.save(transaction);
        
        return bankInfo;
    }
    
    private String simulateCreditCardPayment(PaymentTransaction transaction) {
        // Giả lập trang thanh toán thẻ tín dụng
        String baseUrl = "http://localhost:8081/subscription/card-payment";
        String params = "?transactionId=" + transaction.getTransactionId();
        
        transaction.setPaymentDetails("Credit card payment initiated");
        transaction.setStatus(PaymentTransaction.TransactionStatus.PENDING);
        transactionRepository.save(transaction);
        
        return baseUrl + params;
    }
    
    // Xử lý callback từ các cổng thanh toán
    public void handlePaymentCallback(String transactionId, boolean success) {
        subscriptionService.completePayment(transactionId, success);
    }
}