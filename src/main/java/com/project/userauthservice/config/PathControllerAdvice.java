package com.project.userauthservice.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@Component
@ControllerAdvice
public class PathControllerAdvice {

    @ModelAttribute
    public void addCurrentPathToModel(Model model, HttpServletRequest request) {
        try {
            String currentPath = request.getRequestURI();
            String contextPath = request.getContextPath();
            
            // Chuẩn hóa path bằng cách loại bỏ contextPath nếu có
            String normalizedPath = currentPath.startsWith(contextPath) 
                ? currentPath.substring(contextPath.length())
                : currentPath;
                
            model.addAttribute("currentPath", normalizedPath);
            
            // Có thể thêm các thuộc tính path khác nếu cần
            model.addAttribute("fullPath", currentPath);
            model.addAttribute("contextPath", contextPath);
            
        } catch (Exception e) {
            // Log lỗi nếu cần
            System.err.println("Error adding path to model: " + e.getMessage());
            model.addAttribute("currentPath", "");
        }
    }
}