package com.project.userauthservice.controller;

import com.project.userauthservice.dto.TaskCreateDto;
import com.project.userauthservice.dto.TaskUpdateDto;
import com.project.userauthservice.entity.task.Task;
import com.project.userauthservice.entity.project.Project;
import com.project.userauthservice.entity.workspace.WorkspaceMember;
import com.project.userauthservice.repository.TaskRepository;
import com.project.userauthservice.repository.UserRepository;
import com.project.userauthservice.repository.WorkspaceMemberRepository;
import com.project.userauthservice.service.TaskService;
import com.project.userauthservice.service.ProjectService;
import com.project.userauthservice.service.WorkspaceService;
import com.project.userauthservice.entity.user.User;


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


@Controller
@RequestMapping("/workspaces/{workspaceId}/projects/{projectId}/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final ProjectService projectService;
    private final WorkspaceService workspaceService;

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    @GetMapping
public String listTasks(@PathVariable Long workspaceId,
                        @PathVariable Long projectId, 
                        @AuthenticationPrincipal UserDetails userDetails,
                        Model model) {
    Project project = projectService.getProjectById(projectId);
    
    // Lấy user hiện tại
    User currentUser = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
    
    // Kiểm tra quyền của user trong workspace
    WorkspaceMember member = workspaceMemberRepository.findByWorkspaceAndUser(project.getWorkspace(), currentUser)
            .orElseThrow(() -> new RuntimeException("Bạn không phải là thành viên của workspace này"));
    
    List<Task> tasks;
    
    // Chỉ OWNER và ADMIN mới được xem toàn bộ task
    if (member.getRole() == WorkspaceMember.WorkspaceRole.OWNER || 
        member.getRole() == WorkspaceMember.WorkspaceRole.ADMIN) {
        tasks = taskService.getTasksByProject(projectId);
    } else {
        // Member chỉ xem task được giao cho mình
        tasks = taskRepository.findByProjectAndAssignee(project, currentUser);
    }
    
    // Tính toán thống kê
    int totalTasks = tasks.size();
    int doneTasks = (int) tasks.stream()
            .filter(task -> task.getStatus() == Task.TaskStatus.DONE)
            .count();
    
    double progressPercentage = totalTasks > 0 ? (doneTasks * 100.0 / totalTasks) : 0;
    
    model.addAttribute("workspace", project.getWorkspace());
    model.addAttribute("project", project);
    model.addAttribute("tasks", tasks);
    model.addAttribute("totalTasks", totalTasks);
    model.addAttribute("doneTasks", doneTasks);
    model.addAttribute("progressPercentage", progressPercentage);
    
    return "task/list";
}

    @GetMapping("/create")
    public String createTaskForm(@PathVariable Long workspaceId,
                                @PathVariable Long projectId, 
                                Model model) {
        Project project = projectService.getProjectById(projectId);
        List<WorkspaceMember> members = workspaceService.getWorkspaceMembers(workspaceId);
        
        model.addAttribute("workspace", project.getWorkspace());
        model.addAttribute("project", project);
        model.addAttribute("task", new TaskCreateDto());
        model.addAttribute("members", members);
        model.addAttribute("taskTypes", Task.TaskType.values());
        model.addAttribute("taskPriorities", Task.TaskPriority.values());
        return "task/create";
    }

    @PostMapping("/create")
    public String createTask(@PathVariable Long workspaceId,
                           @PathVariable Long projectId,
                           @Valid @ModelAttribute("task") TaskCreateDto dto,
                           BindingResult result,
                           @AuthenticationPrincipal UserDetails userDetails,
                           RedirectAttributes redirectAttributes,
                           Model model) {
        if (result.hasErrors()) {
            Project project = projectService.getProjectById(projectId);
            List<WorkspaceMember> members = workspaceService.getWorkspaceMembers(workspaceId);
            
            model.addAttribute("workspace", project.getWorkspace());
            model.addAttribute("project", project);
            model.addAttribute("members", members);
            model.addAttribute("taskTypes", Task.TaskType.values());
            model.addAttribute("taskPriorities", Task.TaskPriority.values());
            return "task/create";
        }

        try {
            Task task = taskService.createTask(projectId, dto, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Task đã được tạo thành công!");
            return "redirect:/workspaces/" + workspaceId + "/projects/" + projectId + "/tasks/" + task.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/workspaces/" + workspaceId + "/projects/" + projectId + "/tasks/create";
        }
    }

    @GetMapping("/{id}")
    public String viewTask(@PathVariable Long workspaceId,
                         @PathVariable Long projectId, 
                         @PathVariable Long id, 
                         Model model) {
        Task task = taskService.getTaskById(id);
        
        // Kiểm tra task có thuộc project này không
        if (!task.getProject().getId().equals(projectId)) {
            return "redirect:/workspaces/" + workspaceId + "/projects/" + projectId + "/tasks";
        }
        
        model.addAttribute("task", task);
        model.addAttribute("project", task.getProject());
        model.addAttribute("workspace", task.getProject().getWorkspace());
        return "task/view";
    }
    
    @GetMapping("/{id}/edit")
    public String editTaskForm(@PathVariable Long workspaceId,
                              @PathVariable Long projectId, 
                              @PathVariable Long id, 
                              Model model) {
        Task task = taskService.getTaskById(id);
        
        // Kiểm tra task có thuộc project này không
        if (!task.getProject().getId().equals(projectId)) {
            return "redirect:/workspaces/" + workspaceId + "/projects/" + projectId + "/tasks";
        }
        
        Project project = projectService.getProjectById(projectId);
        List<WorkspaceMember> members = workspaceService.getWorkspaceMembers(workspaceId);
        
        TaskUpdateDto updateDto = new TaskUpdateDto();
        updateDto.setId(task.getId());  // Set ID cho form action
        updateDto.setTaskKey(task.getTaskKey());  // Set taskKey để hiển thị
        updateDto.setTitle(task.getTitle());
        updateDto.setDescription(task.getDescription());
        updateDto.setType(task.getType());
        updateDto.setPriority(task.getPriority());
        updateDto.setStatus(task.getStatus());
        updateDto.setDueDate(task.getDueDate());
        updateDto.setAssigneeId(task.getAssignee() != null ? task.getAssignee().getId() : null);
        updateDto.setEstimatedHours(task.getEstimatedHours());
        updateDto.setActualHours(task.getActualHours());
        
        model.addAttribute("workspace", project.getWorkspace());
        model.addAttribute("project", project);
        model.addAttribute("task", updateDto);  // Sử dụng updateDto thay vì task entity
        model.addAttribute("members", members);
        model.addAttribute("taskTypes", Task.TaskType.values());
        model.addAttribute("taskPriorities", Task.TaskPriority.values());
        model.addAttribute("taskStatuses", Task.TaskStatus.values());
        
        return "task/edit";
    }
    @PostMapping("/{id}/edit")
    public String updateTask(@PathVariable Long workspaceId,
                            @PathVariable Long projectId,
                            @PathVariable Long id,
                            @Valid @ModelAttribute("task") TaskUpdateDto dto,
                            BindingResult result,
                            @AuthenticationPrincipal UserDetails userDetails,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        if (result.hasErrors()) {
            Project project = projectService.getProjectById(projectId);
            List<WorkspaceMember> members = workspaceService.getWorkspaceMembers(workspaceId);
            
            model.addAttribute("workspace", project.getWorkspace());
            model.addAttribute("project", project);
            model.addAttribute("members", members);
            model.addAttribute("taskTypes", Task.TaskType.values());
            model.addAttribute("taskPriorities", Task.TaskPriority.values());
            model.addAttribute("taskStatuses", Task.TaskStatus.values());
            return "task/edit";
        }

        try {
            Task task = taskService.updateTask(id, dto, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Task đã được cập nhật thành công!");
            return "redirect:/workspaces/" + workspaceId + "/projects/" + projectId + "/tasks/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/workspaces/" + workspaceId + "/projects/" + projectId + "/tasks/" + id + "/edit";
        }
    }
    
    @PostMapping("/{id}/delete")
public String deleteTask(@PathVariable Long workspaceId,
                        @PathVariable Long projectId,
                        @PathVariable Long id,
                        @AuthenticationPrincipal UserDetails userDetails,
                        RedirectAttributes redirectAttributes) {
    try {
        // Gọi service với username để kiểm tra quyền
        taskService.deleteTask(id, userDetails.getUsername());
        redirectAttributes.addFlashAttribute("successMessage", "Task đã được xóa thành công!");
    } catch (RuntimeException e) {
        // Nếu không có quyền hoặc có lỗi khác
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    }
    return "redirect:/workspaces/" + workspaceId + "/projects/" + projectId + "/tasks";
}
    @PostMapping("/{id}/update-status")
    public String updateTaskStatus(@PathVariable Long workspaceId,
                                   @PathVariable Long projectId,
                                   @PathVariable Long id,
                                   @RequestParam Task.TaskStatus newStatus,
                                   RedirectAttributes redirectAttributes) {
        try {
            taskService.updateTaskStatus(id, newStatus);
            redirectAttributes.addFlashAttribute("successMessage", "Trạng thái task đã được cập nhật thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi cập nhật trạng thái task: " + e.getMessage());
        }
        return "redirect:/workspaces/" + workspaceId + "/projects/" + projectId + "/tasks/" + id;
    }
    
}