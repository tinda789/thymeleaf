// service/ProjectService.java
package com.project.userauthservice.service;

import com.project.userauthservice.dto.ProjectCreateDto;
import com.project.userauthservice.entity.project.Project;
import com.project.userauthservice.entity.workspace.Workspace;
import com.project.userauthservice.repository.ProjectRepository;
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
                .active(true)  // Đảm bảo set giá trị active
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
}