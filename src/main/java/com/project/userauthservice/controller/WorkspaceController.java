package com.project.userauthservice.controller;

import com.project.userauthservice.dto.WorkspaceCreateDto;
import com.project.userauthservice.dto.WorkspaceMemberAddDto;
import com.project.userauthservice.entity.workspace.Workspace;
import com.project.userauthservice.entity.workspace.WorkspaceMember;
import com.project.userauthservice.entity.project.Project;
import com.project.userauthservice.entity.task.Task;
import com.project.userauthservice.entity.user.User;
import com.project.userauthservice.repository.UserRepository;
import com.project.userauthservice.repository.WorkspaceMemberRepository;
import com.project.userauthservice.service.WorkspaceService;
import com.project.userauthservice.service.ProjectService;
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

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final ProjectService projectService;
    private final TaskService taskService;

    private final UserRepository userRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    @GetMapping
    public String listWorkspaces(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        List<Workspace> workspaces = workspaceService.getUserWorkspaces(userDetails.getUsername());
        model.addAttribute("workspaces", workspaces);
        return "workspace/list";
    }

    @GetMapping("/create")
    public String createWorkspaceForm(Model model) {
        model.addAttribute("workspace", new WorkspaceCreateDto());
        return "workspace/create";
    }

    @PostMapping("/create")
    public String createWorkspace(@Valid @ModelAttribute("workspace") WorkspaceCreateDto dto,
                                BindingResult result,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "workspace/create";
        }

        try {
            Workspace workspace = workspaceService.createWorkspace(dto, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Workspace đã được tạo thành công!");
            return "redirect:/workspaces/" + workspace.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/workspaces/create";
        }
    }

    @GetMapping("/{id}")
    public String viewWorkspace(@PathVariable Long id, 
                                @AuthenticationPrincipal UserDetails userDetails,
                                Model model) {
        try {
            // Lấy user hiện tại
            User currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

            Workspace workspace = workspaceService.getWorkspaceById(id);
            List<Project> projects = projectService.getProjectsByWorkspace(id);
            List<WorkspaceMember> members = workspaceService.getWorkspaceMembers(id);
            
            // Tìm member hiện tại của workspace
            WorkspaceMember currentMember = workspaceMemberRepository.findByWorkspaceAndUser(workspace, currentUser)
                    .orElseThrow(() -> new RuntimeException("Bạn không phải là thành viên của workspace này"));
            
            // Tính toán số lượng tasks
            int activeTasks = 0;
            int completedTasks = 0;
            
            for (Project project : projects) {
                List<Task> tasks = taskService.getTasksByProject(project.getId());
                for (Task task : tasks) {
                    if (task.getStatus() == Task.TaskStatus.IN_PROGRESS || 
                        task.getStatus() == Task.TaskStatus.IN_REVIEW) {
                        activeTasks++;
                    } else if (task.getStatus() == Task.TaskStatus.DONE) {
                        completedTasks++;
                    }
                }
            }
            
            model.addAttribute("workspace", workspace);
            model.addAttribute("projects", projects);
            model.addAttribute("members", members);
            model.addAttribute("activeTasks", activeTasks);
            model.addAttribute("completedTasks", completedTasks);
            model.addAttribute("currentMember", currentMember);
            
            return "workspace/view";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "error";
        }
    }

    
    @GetMapping("/{id}/members")
public String viewMembers(@PathVariable Long id, 
                           @AuthenticationPrincipal UserDetails userDetails,
                           Model model) {
    try {
        Workspace workspace = workspaceService.getWorkspaceById(id);
        List<WorkspaceMember> members = workspaceService.getWorkspaceMembers(id);
        
        // Lấy thông tin người dùng hiện tại
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        
        // Tìm thông tin thành viên hiện tại trong workspace
        WorkspaceMember currentMember = workspaceMemberRepository.findByWorkspaceAndUser(workspace, currentUser)
                .orElse(null);
        
        model.addAttribute("workspace", workspace);
        model.addAttribute("members", members);
        model.addAttribute("memberForm", new WorkspaceMemberAddDto());
        model.addAttribute("roles", Arrays.asList(WorkspaceMember.WorkspaceRole.values()));
        model.addAttribute("currentMember", currentMember);  // Truyền currentMember vào model
        
        return "workspace/members";
    } catch (Exception e) {
        e.printStackTrace();
        model.addAttribute("errorMessage", "Không thể tải danh sách thành viên: " + e.getMessage());
        return "error";
    }
}
    
    @PostMapping("/{id}/members/add")
    public String addMember(@PathVariable Long id,
                           @Valid @ModelAttribute("memberForm") WorkspaceMemberAddDto dto,
                           BindingResult result,
                           @AuthenticationPrincipal UserDetails userDetails,
                           RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "workspace/members";
        }
        
        try {
            workspaceService.addMemberByEmail(id, dto.getEmail(), dto.getRole(), userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm thành viên thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        
        return "redirect:/workspaces/" + id + "/members";
    }
    
    @PostMapping("/{workspaceId}/members/{memberId}/remove")
    public String removeMember(@PathVariable Long workspaceId,
                              @PathVariable Long memberId,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        try {
            workspaceService.removeMember(workspaceId, memberId, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa thành viên thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        
        return "redirect:/workspaces/" + workspaceId + "/members";
    }

    @PostMapping("/{id}/delete")
    public String deleteWorkspace(@PathVariable Long id,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        try {
            workspaceService.deleteWorkspace(id, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Workspace đã được xóa thành công!");
            return "redirect:/workspaces";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/workspaces/" + id;
        }
    }
}