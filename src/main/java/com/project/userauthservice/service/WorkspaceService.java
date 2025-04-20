package com.project.userauthservice.service;

import com.project.userauthservice.dto.WorkspaceCreateDto;
import com.project.userauthservice.entity.user.User;
import com.project.userauthservice.entity.workspace.Workspace;
import com.project.userauthservice.entity.workspace.WorkspaceMember;
import com.project.userauthservice.repository.WorkspaceRepository;
import com.project.userauthservice.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserService userService;

    @Transactional
    public Workspace createWorkspace(WorkspaceCreateDto dto, String username) {
        User owner = userService.findByUsername(username);
        
        // Tạo workspace key duy nhất
        String workspaceKey = generateUniqueWorkspaceKey(dto.getName());
        
        Workspace workspace = Workspace.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .workspaceKey(workspaceKey)
                .owner(owner)
                .active(true)
                .build();
        
        workspace = workspaceRepository.save(workspace);
        
        // Tự động thêm owner làm thành viên với role OWNER
        WorkspaceMember ownerMember = WorkspaceMember.builder()
                .workspace(workspace)
                .user(owner)
                .role(WorkspaceMember.WorkspaceRole.OWNER)
                .build();
        
        workspaceMemberRepository.save(ownerMember);
        
        return workspace;
    }
    
    private String generateUniqueWorkspaceKey(String name) {
        String baseKey = name.replaceAll("[^A-Za-z0-9]", "")
                            .toUpperCase()
                            .substring(0, Math.min(4, name.length()));
        
        String key = baseKey;
        Random random = new Random();
        
        while (workspaceRepository.existsByWorkspaceKey(key)) {
            key = baseKey + random.nextInt(1000);
        }
        
        return key;
    }
    
    public List<Workspace> getUserWorkspaces(String username) {
        User user = userService.findByUsername(username);
        List<WorkspaceMember> memberships = workspaceMemberRepository.findByUser(user);
        
        return memberships.stream()
                .map(WorkspaceMember::getWorkspace)
                .toList();
    }
    
    public Workspace getWorkspaceById(Long id) {
        return workspaceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workspace not found"));
    }
    
    @Transactional
    public void addMemberByEmail(Long workspaceId, String memberEmail, WorkspaceMember.WorkspaceRole role, String currentUsername) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new RuntimeException("Workspace not found"));
        
        // Kiểm tra quyền thêm thành viên
        validateMemberPermission(workspace, currentUsername);
        
        User newMember = userService.findByEmail(memberEmail);
        
        // Kiểm tra xem người dùng đã là thành viên chưa
        if (workspaceMemberRepository.existsByWorkspaceAndUser(workspace, newMember)) {
            throw new RuntimeException("Người dùng đã là thành viên của workspace này");
        }
        
        WorkspaceMember member = WorkspaceMember.builder()
                .workspace(workspace)
                .user(newMember)
                .role(role)
                .build();
        
        workspaceMemberRepository.save(member);
    }
    
    private void validateMemberPermission(Workspace workspace, String username) {
        User currentUser = userService.findByUsername(username);
        
        // Kiểm tra xem người dùng hiện tại có phải là owner hoặc admin không
        WorkspaceMember currentMember = workspaceMemberRepository.findByWorkspaceAndUser(workspace, currentUser)
                .orElseThrow(() -> new RuntimeException("Bạn không phải là thành viên của workspace này"));
        
        if (currentMember.getRole() != WorkspaceMember.WorkspaceRole.OWNER && 
            currentMember.getRole() != WorkspaceMember.WorkspaceRole.ADMIN) {
            throw new RuntimeException("Bạn không có quyền thêm thành viên");
        }
    }
    
    public List<WorkspaceMember> getWorkspaceMembers(Long workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new RuntimeException("Workspace not found"));
        return workspaceMemberRepository.findByWorkspace(workspace);
    }
    
    @Transactional
    public void removeMember(Long workspaceId, Long memberId, String currentUsername) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new RuntimeException("Workspace not found"));
        
        validateMemberPermission(workspace, currentUsername);
        
        WorkspaceMember memberToRemove = workspaceMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        
        if (memberToRemove.getRole() == WorkspaceMember.WorkspaceRole.OWNER) {
            throw new RuntimeException("Không thể xóa chủ sở hữu workspace");
        }
        
        workspaceMemberRepository.delete(memberToRemove);
    }
}