// service/ProjectService.java
package com.project.userauthservice.service;

import com.project.userauthservice.dto.ProjectCreateDto;
import com.project.userauthservice.entity.user.User;
import com.project.userauthservice.entity.task.Task;

import com.project.userauthservice.entity.project.Project;
import com.project.userauthservice.entity.workspace.Workspace;
import com.project.userauthservice.entity.workspace.WorkspaceMember;
import com.project.userauthservice.repository.ProjectRepository;
import com.project.userauthservice.repository.TaskRepository;
import com.project.userauthservice.repository.UserRepository;
import com.project.userauthservice.repository.WorkspaceMemberRepository;
import com.project.userauthservice.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {
    
    private final ProjectRepository projectRepository;
    private final WorkspaceRepository workspaceRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    
    @Transactional
    public Project createProject(Long workspaceId, ProjectCreateDto dto) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new RuntimeException("Workspace not found"));
        
        // Validate dữ liệu
        if (dto.getStartDate() != null && dto.getEndDate() != null &&
            dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new RuntimeException("Ngày kết thúc phải sau ngày bắt đầu");
        }
        
        // Tạo project key duy nhất
        String projectKey = generateProjectKey(workspace.getWorkspaceKey());
        
        Project project = Project.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .projectKey(projectKey)
                .workspace(workspace)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .status(Project.ProjectStatus.ACTIVE)
                .build();
        
        return projectRepository.save(project);
    }
    
    private String generateProjectKey(String workspaceKey) {
        int count = projectRepository.countByWorkspace(workspaceRepository.findByWorkspaceKey(workspaceKey).get());
        String baseKey = workspaceKey;
        String projectKey;
        
        do {
            count++;
            projectKey = baseKey + "-" + count;
        } while (projectRepository.existsByProjectKey(projectKey));
        
        return projectKey;
    }
    
    public List<Project> getProjectsByWorkspace(Long workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new RuntimeException("Workspace not found"));
        return projectRepository.findByWorkspace(workspace);
    }
    
    public Project getProjectById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
    }

    @Transactional
    public void deleteProject(Long projectId, String username) {
    Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found"));
    
    // Kiểm tra quyền xóa (chỉ owner và admin của workspace mới được phép xóa)
    User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
    
    WorkspaceMember member = workspaceMemberRepository.findByWorkspaceAndUser(project.getWorkspace(), currentUser)
            .orElseThrow(() -> new RuntimeException("Bạn không phải là thành viên của workspace này"));
    
    if (member.getRole() != WorkspaceMember.WorkspaceRole.OWNER && 
        member.getRole() != WorkspaceMember.WorkspaceRole.ADMIN) {
        throw new RuntimeException("Bạn không có quyền xóa dự án này");
    }
    
    // Xóa tất cả tasks liên quan trước
    List<Task> tasks = taskRepository.findByProject(project);
    taskRepository.deleteAll(tasks);
    
    // Xóa project
    projectRepository.delete(project);
}
public void validateProjectCreation(Workspace workspace, User currentUser) {
    int currentProjectCount = projectRepository.countByWorkspace(workspace);
    
    switch (currentUser.getAccountLevel()) {
        case FREE:
            if (currentProjectCount >= 1) {
                throw new RuntimeException("Tài khoản miễn phí chỉ được tạo tối đa 1 dự án");
            }
            break;
        case STANDARD:
            if (currentProjectCount >= 5) {
                throw new RuntimeException("Tài khoản Standard chỉ được tạo tối đa 5 dự án");
            }
            break;
        case PREMIUM:
            if (currentProjectCount >= 20) {
                throw new RuntimeException("Tài khoản Premium chỉ được tạo tối đa 20 dự án");
            }
            break;
        case VIP:
            // Không giới hạn
            break;
    }
}

}