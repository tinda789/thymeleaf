package com.project.userauthservice.service;

import com.project.userauthservice.dto.UserProfileUpdateDto;
import com.project.userauthservice.dto.UserRegistrationDto;
import com.project.userauthservice.entity.user.Role;
import com.project.userauthservice.entity.user.User;
import com.project.userauthservice.entity.user.UserRole;
import com.project.userauthservice.entity.user.VerificationToken;
import com.project.userauthservice.repository.RoleRepository;
import com.project.userauthservice.repository.UserRepository;
import com.project.userauthservice.repository.UserRoleRepository;
import com.project.userauthservice.repository.VerificationTokenRepository;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenRepository verificationTokenRepository;
    private final EmailService emailService;
    
    @Value("${application.base-url:http://localhost:8081}")
    private String baseUrl;
    
    private static final String AVATAR_UPLOAD_DIR = "uploads/avatars/";
    
    @Transactional
    public User registerNewUser(UserRegistrationDto registrationDto) {
        // Check if username already exists
        if (userRepository.existsByUsername(registrationDto.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        
        // Check if email already exists
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        
        // Create new user
        User user = new User();
        user.setUsername(registrationDto.getUsername());
        user.setEmail(registrationDto.getEmail());
        user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        user.setFullName(registrationDto.getFullName());
        user.setActive(true);
        user.setEmailVerified(false); // Will be set to true after email verification
        user.setCreatedAt(LocalDateTime.now());
        
        // Save user
        User savedUser = userRepository.save(user);
        
        // Assign default user role
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Default role not found"));
        
        UserRole userUserRole = new UserRole();
        userUserRole.setUser(savedUser);
        userUserRole.setRole(userRole);
        userRoleRepository.save(userUserRole);
        
        // Tạo và gửi token xác thực
        createVerificationTokenAndSendEmail(savedUser);
        
        return savedUser;
    }
    
    private void createVerificationTokenAndSendEmail(User user) {
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);
        verificationToken.setExpiryDate(LocalDateTime.now().plusHours(24));
        
        verificationTokenRepository.save(verificationToken);
        
        // Gửi email xác thực
        String verificationLink = baseUrl + "/verify-email?token=" + token;
        try {
            emailService.sendVerificationEmail(user.getEmail(), user.getUsername(), verificationLink);
        } catch (MessagingException e) {
            // Log lỗi
            e.printStackTrace();
        }
    }
    
    @Transactional
    public boolean verifyEmail(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token không hợp lệ"));
        
        if (verificationToken.isExpired()) {
            verificationTokenRepository.delete(verificationToken);
            throw new RuntimeException("Token đã hết hạn");
        }
        
        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        
        verificationTokenRepository.delete(verificationToken);
        return true;
    }
    
    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        
        if (user.isEmailVerified()) {
            throw new RuntimeException("Email đã được xác thực");
        }
        
        // Xóa token cũ nếu có
        verificationTokenRepository.findByUser(user).ifPresent(verificationTokenRepository::delete);
        
        // Tạo và gửi token mới
        createVerificationTokenAndSendEmail(user);
    }
    
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    @Transactional
    public User updateProfile(String username, UserProfileUpdateDto profileDto) {
        User user = findByUsername(username);
        
        // Kiểm tra email trùng lặp
        if (!user.getEmail().equals(profileDto.getEmail()) && 
            userRepository.existsByEmail(profileDto.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng");
        }
        
        user.setFullName(profileDto.getFullName());
        user.setEmail(profileDto.getEmail());
        user.setPhoneNumber(profileDto.getPhoneNumber());
        user.setDateOfBirth(profileDto.getDateOfBirth());
        user.setGender(profileDto.getGender());
        
        return userRepository.save(user);
    }
    
    @Transactional
    public void updateAvatar(String username, MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new RuntimeException("File ảnh không được để trống");
        }
        
        // Kiểm tra loại file
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Chỉ chấp nhận file ảnh");
        }
        
        // Kiểm tra kích thước file
        if (file.getSize() > 5 * 1024 * 1024) { // 5MB
            throw new RuntimeException("Kích thước file không được vượt quá 5MB");
        }
        
        // Tạo tên file unique
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        
        // Tạo thư mục nếu chưa tồn tại
        Path uploadPath = Paths.get(AVATAR_UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Lưu file
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);
        
        // Cập nhật database
        User user = findByUsername(username);
        
        // Xóa avatar cũ nếu có
        if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
            try {
                Path oldAvatar = Paths.get(user.getAvatar());
                Files.deleteIfExists(oldAvatar);
            } catch (Exception e) {
                // Log lỗi nếu không xóa được file cũ
                e.printStackTrace();
            }
        }
        
        user.setAvatar(AVATAR_UPLOAD_DIR + fileName);
        userRepository.save(user);
    }
    
    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = findByUsername(username);
        
        // Kiểm tra mật khẩu hiện tại
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Mật khẩu hiện tại không chính xác");
        }
        
        // Kiểm tra mật khẩu mới
        if (newPassword.length() < 6) {
            throw new RuntimeException("Mật khẩu mới phải có ít nhất 6 ký tự");
        }
        
        // Không cho phép mật khẩu mới giống mật khẩu cũ
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new RuntimeException("Mật khẩu mới phải khác mật khẩu hiện tại");
        }
        
        // Cập nhật mật khẩu mới
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
    
    @Transactional
    public void updateLastLogin(String username) {
        User user = findByUsername(username);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }
    
    @Transactional
    public void activateUser(String username) {
        User user = findByUsername(username);
        user.setActive(true);
        userRepository.save(user);
    }
    
    @Transactional
    public void deactivateUser(String username) {
        User user = findByUsername(username);
        user.setActive(false);
        userRepository.save(user);
    }
    

    
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    @Transactional
    public void assignRoleToUser(String username, String roleName) {
        User user = findByUsername(username);
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
        
        // Kiểm tra xem user đã có role này chưa
        boolean roleExists = user.getUserRoles().stream()
                .anyMatch(userRole -> userRole.getRole().getName().equals(roleName));
        
        if (!roleExists) {
            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(role);
            userRoleRepository.save(userRole);
        }
    }
    
    @Transactional
    public void removeRoleFromUser(String username, String roleName) {
        User user = findByUsername(username);
        
        user.getUserRoles().removeIf(userRole -> 
            userRole.getRole().getName().equals(roleName)
        );
        
        userRepository.save(user);
    }
}