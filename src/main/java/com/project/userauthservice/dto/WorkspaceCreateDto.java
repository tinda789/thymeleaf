package com.project.userauthservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WorkspaceCreateDto {
    
    @NotBlank(message = "Tên workspace không được để trống")
    @Size(min = 3, max = 100, message = "Tên workspace phải từ 3-100 ký tự")
    private String name;
    
    @Size(max = 500, message = "Mô tả không được quá 500 ký tự")
    private String description;
}