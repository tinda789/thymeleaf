package com.project.userauthservice.config;

import com.project.userauthservice.security.CustomAuthenticationFailureHandler;
import com.project.userauthservice.security.CustomAuthenticationSuccessHandler;
import com.project.userauthservice.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final CustomAuthenticationFailureHandler authenticationFailureHandler;
    private final CustomAuthenticationSuccessHandler authenticationSuccessHandler;
    private final PasswordEncoder passwordEncoder;

    public SecurityConfig(CustomUserDetailsService userDetailsService, 
                         CustomAuthenticationFailureHandler authenticationFailureHandler,
                         CustomAuthenticationSuccessHandler authenticationSuccessHandler,
                         PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.authenticationFailureHandler = authenticationFailureHandler;
        this.authenticationSuccessHandler = authenticationSuccessHandler;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())  // Tắt CSRF nếu ứng dụng không cần bảo vệ CSRF
            .authorizeRequests(authorizeRequests -> authorizeRequests
                .requestMatchers("/", "/register", "/login", "/verify-email", "/resend-verification", "/css/**", "/js/**", "/images/**", "/uploads/**")
                    .permitAll()  // Tất cả mọi người đều có thể truy cập các trang này
                .requestMatchers("/dashboard/**", "/profile/**", "/workspaces/**", "/subscription/**", "/projects/**", "/tasks/**")
                    .authenticated()  // Cần phải đăng nhập để truy cập các trang này
                .anyRequest().authenticated()  // Các yêu cầu còn lại cũng yêu cầu đăng nhập
            )
            .formLogin(formLogin -> formLogin
                .loginPage("/login")  // Chỉ định trang login
                .loginProcessingUrl("/perform-login")  // URL để xử lý đăng nhập
                .successHandler(authenticationSuccessHandler)  // Xử lý sau khi đăng nhập thành công
                .failureHandler(authenticationFailureHandler)  // Xử lý khi đăng nhập thất bại
                .permitAll()  // Cho phép tất cả người dùng truy cập trang login
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))  // Cấu hình URL logout
                .logoutSuccessUrl("/login?logout=true")  // URL khi đăng xuất thành công
                .invalidateHttpSession(true)  // Hủy session khi đăng xuất
                .deleteCookies("JSESSIONID")  // Xóa cookie phiên khi đăng xuất
                .permitAll()  // Cho phép tất cả người dùng thực hiện logout
            )
            .sessionManagement(sessionManagement -> sessionManagement
                .maximumSessions(1)  // Giới hạn số phiên đăng nhập đồng thời
                .expiredUrl("/login?expired=true")  // URL khi phiên hết hạn
            );

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);  // Cung cấp dịch vụ UserDetails tùy chỉnh
        authProvider.setPasswordEncoder(passwordEncoder);  // Mã hóa mật khẩu với PasswordEncoder
        authProvider.setHideUserNotFoundExceptions(false);  // Hiển thị thông báo khi không tìm thấy user
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();  // Cung cấp AuthenticationManager
    }
}
