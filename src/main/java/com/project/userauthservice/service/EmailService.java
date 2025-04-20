package com.project.userauthservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
@RequiredArgsConstructor
public class EmailService {
    
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    
    @Value("${application.mail.from}")
    private String mailFrom;
    
    @Value("${application.mail.sender-name}")
    private String senderName;
    
    public void sendVerificationEmail(String to, String username, String verificationLink) throws MessagingException {
        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("verificationLink", verificationLink);
        
        String htmlContent = templateEngine.process("email/verification-email", context);
        
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(mailFrom);
        helper.setTo(to);
        helper.setSubject("Xác thực tài khoản của bạn");
        helper.setText(htmlContent, true);
        
        mailSender.send(message);
    }
}