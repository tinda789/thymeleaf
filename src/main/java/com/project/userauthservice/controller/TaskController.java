package com.project.userauthservice.controller;

import com.project.userauthservice.dto.TaskCreateDto;
import com.project.userauthservice.dto.TaskUpdateDto;
import com.project.userauthservice.entity.task.Task;
import com.project.userauthservice.entity.project.Project;
import com.project.userauthservice.entity.workspace.WorkspaceMember;
import com.project.userauthservice.service.TaskService;
import com.project.userauthservice.service.ProjectService;
import com.project.userauthservice.service.WorkspaceService;
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
import java.util.stream.Collectors;

@Controller
@RequestMapping("/workspaces/{workspaceId}/projects/{projectId}/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final ProjectService projectService;
    private final WorkspaceService workspaceService;

    @GetMapping
    public String listTasks(@PathVariable Long workspaceId,
                           @PathVariable Long projectId, 
                           Model model) {
        Project project = projectService.getProjectById(projectId);
        List<Task> tasks = taskService.getTasksByProject(projectId);
        
        // Nhóm tasks theo status
        Map<String, List<Task>> tasksByStatus = tasks.stream()
            .collect(Collectors.groupingBy(task -> task.getStatus().name()));
        
        // Tính toán thống kê
        int totalTasks = tasks.size();
        int doneTasks = tasksByStatus.containsKey("DONE") ? tasksByStatus.get("DONE").size() : 0;
        double progressPercentage = totalTasks > 0 ? (doneTasks * 100.0 / totalTasks) : 0;
        
        model.addAttribute("workspace", project.getWorkspace());
        model.addAttribute("project", project);
        model.addAttribute("tasks", tasks);
        model.addAttribute("tasksByStatus", tasksByStatus);
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
        model.addAttribute("task", task);
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
                            RedirectAttributes redirectAttributes) {
        try {
            taskService.deleteTask(id);
            redirectAttributes.addFlashAttribute("successMessage", "Task đã được xóa thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/workspaces/" + workspaceId + "/projects/" + projectId + "/tasks";
    }
}