package com.project.userauthservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.project.userauthservice.entity.workspace.WorkspaceMember.WorkspaceRole;

@Data
public class WorkspaceMemberAddDto {
    
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;
    
    @NotNull(message = "Vui lòng chọn vai trò")
    private WorkspaceRole role;
}