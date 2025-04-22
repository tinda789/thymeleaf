package com.project.userauthservice.service;

import com.project.userauthservice.config.VNPayConfig;
import com.project.userauthservice.entity.subscription.PaymentTransaction;
import com.project.userauthservice.repository.PaymentTransactionRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PaymentService {
    
    private final PaymentTransactionRepository transactionRepository;
    private final SubscriptionService subscriptionService;
    
    @Autowired
    private VNPayConfig vnPayConfig;
    
    public String processVNPayPayment(PaymentTransaction transaction) {
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnPayConfig.getVersion());
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnp_Params.put("vnp_Amount", String.valueOf(Math.round(transaction.getAmount() * 100)));
        vnp_Params.put("vnp_CurrCode", "VND");
        
        // Mã giao dịch
        vnp_Params.put("vnp_TxnRef", transaction.getTransactionId());
        
        // Thông tin đơn hàng
        String orderInfo = "Thanh toan goi " + transaction.getUserSubscription().getSubscriptionPackage().getName();
        vnp_Params.put("vnp_OrderInfo", orderInfo);
        vnp_Params.put("vnp_OrderType", "billpayment");
        
        // Ngôn ngữ
        vnp_Params.put("vnp_Locale", "vn");
        
        // URL trả về
        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        
        // IP của khách hàng
        vnp_Params.put("vnp_IpAddr", "127.0.0.1");
        
        // Thời gian tạo giao dịch
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(calendar.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
        
        // Sắp xếp các tham số theo thứ tự a-z
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        
        // Tạo chuỗi hashData và query
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                try {
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    
                    // Build query
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        
        // Tạo chữ ký hmacSHA512
        String vnp_SecureHash = hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        query.append("&vnp_SecureHash=").append(vnp_SecureHash);
        
        // Tạo URL thanh toán
        String paymentUrl = vnPayConfig.getPaymentUrl() + "?" + query.toString();
        
        // Lưu thêm thông tin vào transaction
        transaction.setPaymentDetails("VNPAY transaction initiated: " + paymentUrl);
        transaction.setStatus(PaymentTransaction.TransactionStatus.PENDING);
        transactionRepository.save(transaction);
        
        return paymentUrl;
    }
    
    public String processMoMoPayment(PaymentTransaction transaction) {
        // Giả lập như hiện tại
        String baseUrl = "http://localhost:8081/subscription/momo-callback";
        String params = "?orderId=" + transaction.getTransactionId() + 
                       "&amount=" + transaction.getAmount() +
                       "&resultCode=0"; // 0 = thành công
        
        transaction.setPaymentDetails("MoMo transaction initiated");
        transaction.setStatus(PaymentTransaction.TransactionStatus.PENDING);
        transactionRepository.save(transaction);
        
        return baseUrl + params;
    }
    
    public String processBankTransfer(PaymentTransaction transaction) {
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
    
    public String processCreditCard(PaymentTransaction transaction) {
        // Giả lập trang thanh toán thẻ tín dụng
        String baseUrl = "http://localhost:8081/subscription/card-payment";
        String params = "?transactionId=" + transaction.getTransactionId();
        
        transaction.setPaymentDetails("Credit card payment initiated");
        transaction.setStatus(PaymentTransaction.TransactionStatus.PENDING);
        transactionRepository.save(transaction);
        
        return baseUrl + params;
    }
    
    // Xử lý callback từ các cổng thanh toán
    @Transactional
    public void handlePaymentCallback(String transactionId, boolean success) {
        PaymentTransaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch với mã: " + transactionId));
    
        // Cập nhật trạng thái giao dịch
        transaction.setStatus(success ? PaymentTransaction.TransactionStatus.COMPLETED : 
                                      PaymentTransaction.TransactionStatus.FAILED);
        transactionRepository.save(transaction);
    
        // Hoàn tất quá trình đăng ký
        subscriptionService.completePayment(transactionId, success);
    }
    
    // Phương thức tạo HMAC SHA512
    private String hmacSHA512(String key, String data) {
        try {
            Mac sha512_HMAC = Mac.getInstance("HmacSHA512");
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            sha512_HMAC.init(secret_key);
            byte[] hash = sha512_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    
    // Chuyển đổi byte array sang hex string
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}