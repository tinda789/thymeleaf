package com.project.userauthservice.controller;

import com.project.userauthservice.dto.ProjectCreateDto;
import com.project.userauthservice.entity.project.Project;
import com.project.userauthservice.entity.task.Task;
import com.project.userauthservice.entity.workspace.Workspace;
import com.project.userauthservice.entity.workspace.WorkspaceMember;
import com.project.userauthservice.entity.user.User;
import com.project.userauthservice.repository.UserRepository;
import com.project.userauthservice.repository.WorkspaceMemberRepository;
import com.project.userauthservice.service.ProjectService;
import com.project.userauthservice.service.WorkspaceService;
import com.project.userauthservice.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/workspaces/{workspaceId}/projects")
@RequiredArgsConstructor
public class ProjectController {
    
    private final ProjectService projectService;
    private final WorkspaceService workspaceService;
    private final TaskService taskService;
    private final UserRepository userRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    @GetMapping
    public String listProjects(@PathVariable Long workspaceId, 
                               Model model,
                               @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
                
        Workspace workspace = workspaceService.getWorkspaceById(workspaceId);
        List<Project> projects = projectService.getProjectsByWorkspace(workspaceId);
        
        // Lấy thông tin member hiện tại để kiểm tra quyền
        WorkspaceMember currentMember = workspaceMemberRepository.findByWorkspaceAndUser(workspace, currentUser)
                .orElseThrow(() -> new RuntimeException("Bạn không phải là thành viên của workspace này"));
        
        model.addAttribute("workspace", workspace);
        model.addAttribute("projects", projects);
        model.addAttribute("currentMember", currentMember);
        return "project/list";
    }

    @GetMapping("/create")
    public String createProjectForm(
        @PathVariable Long workspaceId, 
        Model model,
        @AuthenticationPrincipal UserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        try {
            User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
            
            Workspace workspace = workspaceService.getWorkspaceById(workspaceId);
            
            // Kiểm tra quyền - chỉ OWNER và ADMIN mới được tạo project
            WorkspaceMember member = workspaceMemberRepository.findByWorkspaceAndUser(workspace, user)
                .orElseThrow(() -> new RuntimeException("Bạn không phải là thành viên của workspace này"));
                
            if (member.getRole() != WorkspaceMember.WorkspaceRole.OWNER && 
                member.getRole() != WorkspaceMember.WorkspaceRole.ADMIN) {
                throw new RuntimeException("Bạn không có quyền tạo dự án trong workspace này");
            }
            
            // Kiểm tra giới hạn dự án theo loại tài khoản
            projectService.validateProjectCreation(workspace, user);
            
            model.addAttribute("workspace", workspace);
            model.addAttribute("project", new ProjectCreateDto());
            return "project/create";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/workspaces/" + workspaceId;
        }
    }

    @PostMapping("/create")
    public String createProject(
        @PathVariable Long workspaceId,
        @Valid @ModelAttribute("project") ProjectCreateDto dto,
        BindingResult result,
        @AuthenticationPrincipal UserDetails userDetails,
        RedirectAttributes redirectAttributes,
        Model model
    ) {
        try {
            User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
            
            Workspace workspace = workspaceService.getWorkspaceById(workspaceId);
            
            // Kiểm tra quyền - chỉ OWNER và ADMIN mới được tạo project
            WorkspaceMember member = workspaceMemberRepository.findByWorkspaceAndUser(workspace, user)
                .orElseThrow(() -> new RuntimeException("Bạn không phải là thành viên của workspace này"));
                
            if (member.getRole() != WorkspaceMember.WorkspaceRole.OWNER && 
                member.getRole() != WorkspaceMember.WorkspaceRole.ADMIN) {
                throw new RuntimeException("Bạn không có quyền tạo dự án trong workspace này");
            }
            
            // Kiểm tra giới hạn dự án theo loại tài khoản
            projectService.validateProjectCreation(workspace, user);
            
            // Kiểm tra lỗi validation
            if (result.hasErrors()) {
                model.addAttribute("workspace", workspace);
                return "project/create";
            }

            Project project = projectService.createProject(workspaceId, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Dự án đã được tạo thành công!");
            return "redirect:/workspaces/" + workspaceId + "/projects/" + project.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/workspaces/" + workspaceId;
        }
    }

    @GetMapping("/{id}")
    public String viewProject(@PathVariable Long workspaceId, 
                            @PathVariable Long id, 
                            Model model,
                            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
                            
        Project project = projectService.getProjectById(id);
        
        // Kiểm tra project có thuộc workspace này không
        if (!project.getWorkspace().getId().equals(workspaceId)) {
            return "redirect:/workspaces/" + workspaceId + "/projects";
        }
        
        // Lấy danh sách task theo status
        Map<Task.TaskStatus, List<Task>> tasksByStatus = taskService.getTasksByProjectGroupedByStatus(id);

        // Tính toán thống kê
        int totalTasks = 0;
        int doneTasks = 0;
        
        for (List<Task> tasks : tasksByStatus.values()) {
            totalTasks += tasks.size();
        }
        
        if (tasksByStatus.containsKey(Task.TaskStatus.DONE)) {
            doneTasks = tasksByStatus.get(Task.TaskStatus.DONE).size();
        }
        
        double progressPercentage = totalTasks > 0 ? (doneTasks * 100.0 / totalTasks) : 0;

        // Lấy thông tin member hiện tại để kiểm tra quyền
        WorkspaceMember currentMember = workspaceMemberRepository.findByWorkspaceAndUser(project.getWorkspace(), currentUser)
                .orElseThrow(() -> new RuntimeException("Bạn không phải là thành viên của workspace này"));

        model.addAttribute("project", project);
        model.addAttribute("workspace", project.getWorkspace());
        model.addAttribute("tasksByStatus", tasksByStatus);
        model.addAttribute("totalTasks", totalTasks);
        model.addAttribute("doneTasks", doneTasks);
        model.addAttribute("progressPercentage", progressPercentage);
        model.addAttribute("currentMember", currentMember);
        
        return "project/view";
    }

    @PostMapping("/{id}/delete")
    public String deleteProject(
        @PathVariable Long workspaceId,
        @PathVariable Long id,
        @AuthenticationPrincipal UserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        try {
            projectService.deleteProject(id, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa dự án thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/workspaces/" + workspaceId + "/projects";
    }
}