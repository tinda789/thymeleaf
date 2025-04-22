package com.project.userauthservice.service;

import com.project.userauthservice.dto.WorkspaceCreateDto;
import com.project.userauthservice.entity.project.Project;
import com.project.userauthservice.entity.task.Task;
import com.project.userauthservice.entity.user.User;
import com.project.userauthservice.entity.workspace.Workspace;
import com.project.userauthservice.entity.workspace.WorkspaceMember;
import com.project.userauthservice.repository.ProjectRepository;
import com.project.userauthservice.repository.TaskRepository;
import com.project.userauthservice.repository.UserRepository;
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

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

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
    
    @Transactional(readOnly = true)
    public Workspace getWorkspaceById(Long id) {
        Workspace workspace = workspaceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workspace not found"));
        // Force load members collection
        workspace.getMembers().size();
        return workspace;
    }
    
    @Transactional(readOnly = true)
    public List<WorkspaceMember> getWorkspaceMembers(Long workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new RuntimeException("Workspace not found"));
        
        List<WorkspaceMember> members = workspaceMemberRepository.findByWorkspace(workspace);
        
        // Force load user details to avoid lazy loading issues
        members.forEach(member -> {
            if (member.getUser() != null) {
                member.getUser().getUsername();
                member.getUser().getEmail();
                member.getUser().getFullName();
            }
        });
        
        return members;
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

     @Transactional
    public void deleteWorkspace(Long workspaceId, String username) {
        // Tìm workspace
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new RuntimeException("Workspace không tồn tại"));
        
        // Tìm người dùng hiện tại
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        
        // Kiểm tra quyền
        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceAndUser(workspace, currentUser)
                .orElseThrow(() -> new RuntimeException("Bạn không phải là thành viên của workspace này"));
        
        // Chỉ OWNER và ADMIN mới được xóa
        if (member.getRole() != WorkspaceMember.WorkspaceRole.OWNER && 
            member.getRole() != WorkspaceMember.WorkspaceRole.ADMIN) {
            throw new RuntimeException("Bạn không có quyền xóa workspace này");
        }
        
        // Xóa các project trong workspace trước
        List<Project> projects = projectRepository.findByWorkspace(workspace);
        for (Project project : projects) {
            // Xóa tasks của từng project
            List<Task> tasks = taskRepository.findByProject(project);
            taskRepository.deleteAll(tasks);
            
            // Xóa project
            projectRepository.delete(project);
        }
        
        // Xóa workspace
        workspaceMemberRepository.deleteByWorkspace(workspace);
        workspaceRepository.delete(workspace);
    }

    public void validateWorkspaceCreation(User user) {
        int currentWorkspaceCount = workspaceRepository.countByOwner(user);
        
        switch (user.getAccountLevel()) {
            case FREE:
                if (currentWorkspaceCount >= 1) {
                    throw new RuntimeException("Tài khoản miễn phí chỉ được tạo tối đa 1 workspace");
                }
                break;
            case STANDARD:
                if (currentWorkspaceCount >= 3) {
                    throw new RuntimeException("Tài khoản Standard chỉ được tạo tối đa 3 workspace");
                }
                break;
            case PREMIUM:
                if (currentWorkspaceCount >= 10) {
                    throw new RuntimeException("Tài khoản Premium chỉ được tạo tối đa 10 workspace");
                }
                break;
            case VIP:
                // Không giới hạn
                break;
        }
    }
    
    public void validateAddMember(Workspace workspace, User currentUser) {
        int currentMemberCount = workspaceMemberRepository.countByWorkspace(workspace);
        
        switch (currentUser.getAccountLevel()) {
            case FREE:
                if (currentMemberCount >= 2) {
                    throw new RuntimeException("Tài khoản miễn phí chỉ được thêm tối đa 2 thành viên");
                }
                break;
            case STANDARD:
                if (currentMemberCount >= 5) {
                    throw new RuntimeException("Tài khoản Standard chỉ được thêm tối đa 5 thành viên");
                }
                break;
            case PREMIUM:
                if (currentMemberCount >= 20) {
                    throw new RuntimeException("Tài khoản Premium chỉ được thêm tối đa 20 thành viên");
                }
                break;
            case VIP:
                // Không giới hạn
                break;
        }
    }
}