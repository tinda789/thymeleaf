package com.project.userauthservice.controller;

import com.project.userauthservice.dto.ProjectCreateDto;
import com.project.userauthservice.entity.project.Project;
import com.project.userauthservice.entity.task.Task;
import com.project.userauthservice.entity.workspace.Workspace;
import com.project.userauthservice.service.ProjectService;
import com.project.userauthservice.service.TaskService;
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

@Controller
@RequestMapping("/workspaces/{workspaceId}/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final WorkspaceService workspaceService;
    private final TaskService taskService;

    @GetMapping
    public String listProjects(@PathVariable Long workspaceId, Model model) {
        Workspace workspace = workspaceService.getWorkspaceById(workspaceId);
        List<Project> projects = projectService.getProjectsByWorkspace(workspaceId);
        
        model.addAttribute("workspace", workspace);
        model.addAttribute("projects", projects);
        return "project/list";
    }

    @GetMapping("/create")
    public String createProjectForm(@PathVariable Long workspaceId, Model model) {
        Workspace workspace = workspaceService.getWorkspaceById(workspaceId);
        model.addAttribute("workspace", workspace);
        model.addAttribute("project", new ProjectCreateDto());
        return "project/create";
    }

    @PostMapping("/create")
    public String createProject(@PathVariable Long workspaceId,
                              @Valid @ModelAttribute("project") ProjectCreateDto dto,
                              BindingResult result,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        
        // Kiểm tra lỗi validation
        if (result.hasErrors()) {
            Workspace workspace = workspaceService.getWorkspaceById(workspaceId);
            model.addAttribute("workspace", workspace);
            return "project/create";
        }

        try {
            Project project = projectService.createProject(workspaceId, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Dự án đã được tạo thành công!");
            return "redirect:/workspaces/" + workspaceId + "/projects/" + project.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/workspaces/" + workspaceId + "/projects/create";
        }
    }

    @GetMapping("/{id}")
    public String viewProject(@PathVariable Long workspaceId, 
                            @PathVariable Long id, 
                            Model model) {
        Project project = projectService.getProjectById(id);
        
        // Kiểm tra project có thuộc workspace này không
        if (!project.getWorkspace().getId().equals(workspaceId)) {
            return "redirect:/workspaces/" + workspaceId + "/projects";
        }
        
        // Lấy danh sách task theo status
        Map<Task.TaskStatus, List<Task>> tasksByStatus = taskService.getTasksByProjectGroupedByStatus(id);
        
        System.out.println("Total tasks by status map size: " + tasksByStatus.size());
        for (Map.Entry<Task.TaskStatus, List<Task>> entry : tasksByStatus.entrySet()) {
            System.out.println("Status " + entry.getKey() + ": " + entry.getValue().size() + " tasks");
        }

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
        
        System.out.println("Total tasks: " + totalTasks);
        System.out.println("Done tasks: " + doneTasks);
        System.out.println("Progress: " + progressPercentage);

        model.addAttribute("project", project);
        model.addAttribute("workspace", project.getWorkspace());
        model.addAttribute("tasksByStatus", tasksByStatus);
        model.addAttribute("totalTasks", totalTasks);
        model.addAttribute("doneTasks", doneTasks);
        model.addAttribute("progressPercentage", progressPercentage);
        
        return "project/view";
    }

    @PostMapping("/{id}/delete")
public String deleteProject(@PathVariable Long workspaceId,
                          @PathVariable Long id,
                          @AuthenticationPrincipal UserDetails userDetails,
                          RedirectAttributes redirectAttributes) {
    try {
        projectService.deleteProject(id, userDetails.getUsername());
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa dự án thành công!");
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    }
    return "redirect:/workspaces/" + workspaceId + "/projects";
}
}