package com.project.userauthservice.service;

import com.project.userauthservice.dto.TaskCreateDto;
import com.project.userauthservice.dto.TaskUpdateDto;
import com.project.userauthservice.entity.task.Task;
import com.project.userauthservice.entity.project.Project;
import com.project.userauthservice.entity.user.User;
import com.project.userauthservice.repository.TaskRepository;
import com.project.userauthservice.repository.ProjectRepository;
import com.project.userauthservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {
    
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    
    @Transactional
    public Task createTask(Long projectId, TaskCreateDto dto, String reporterUsername) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        
        User reporter = userRepository.findByUsername(reporterUsername)
                .orElseThrow(() -> new RuntimeException("Reporter not found"));
        
        User assignee = null;
        if (dto.getAssigneeId() != null) {
            assignee = userRepository.findById(dto.getAssigneeId())
                    .orElseThrow(() -> new RuntimeException("Assignee not found"));
        }
        
        String taskKey = generateTaskKey(project);
        
        Task task = Task.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .taskKey(taskKey)
                .project(project)
                .reporter(reporter)
                .assignee(assignee)
                .type(dto.getType() != null ? dto.getType() : Task.TaskType.TASK)
                .priority(dto.getPriority() != null ? dto.getPriority() : Task.TaskPriority.MEDIUM)
                .dueDate(dto.getDueDate())
                .estimatedHours(dto.getEstimatedHours())
                .status(Task.TaskStatus.TODO)
                .build();
        
        return taskRepository.save(task);
    }
    
    private String generateTaskKey(Project project) {
        String projectKey = project.getProjectKey();
        List<Task> projectTasks = taskRepository.findByProject(project);
        int nextNumber = projectTasks.size() + 1;
        String taskKey = projectKey + "-" + nextNumber;
        
        // Đảm bảo không bị trùng key
        while (taskRepository.existsByTaskKey(taskKey)) {
            nextNumber++;
            taskKey = projectKey + "-" + nextNumber;
        }
        
        return taskKey;
    }
    
    @Transactional
    public Task updateTask(Long taskId, TaskUpdateDto dto, String updaterUsername) {
        Task task = getTaskById(taskId);
        
        // Cập nhật thông tin chính của task
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setType(dto.getType());
        task.setPriority(dto.getPriority());
        task.setStatus(dto.getStatus());
        task.setDueDate(dto.getDueDate());
        
        // Cập nhật người thực hiện (assignee)
        if (dto.getAssigneeId() != null) {
            User assignee = userRepository.findById(dto.getAssigneeId())
                    .orElseThrow(() -> new RuntimeException("Assignee not found"));
            task.setAssignee(assignee);
        } else {
            task.setAssignee(null);
        }
        
        // Cập nhật thời gian ước tính và thực tế
        task.setEstimatedHours(dto.getEstimatedHours());
        task.setActualHours(dto.getActualHours());
        
        return taskRepository.save(task);
    }
    
    public List<Task> getTasksByProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        return taskRepository.findByProject(project);
    }
    
    public Map<Task.TaskStatus, List<Task>> getTasksByProjectGroupedByStatus(Long projectId) {
        List<Task> tasks = getTasksByProject(projectId);
        Map<Task.TaskStatus, List<Task>> tasksByStatus = new HashMap<>();
        
        // Khởi tạo map với tất cả các status để tránh null pointer
        for (Task.TaskStatus status : Task.TaskStatus.values()) {
            tasksByStatus.put(status, new ArrayList<>());
        }
        
        // Nhóm tasks theo status
        if (!tasks.isEmpty()) {
            Map<Task.TaskStatus, List<Task>> groupedTasks = tasks.stream()
                    .collect(Collectors.groupingBy(Task::getStatus));
            
            // Cập nhật vào map đã khởi tạo
            tasksByStatus.putAll(groupedTasks);
        }
        
        return tasksByStatus;
    }
    
    public Task getTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));
    }
    
    @Transactional
    public Task updateTaskStatus(Long taskId, Task.TaskStatus newStatus) {
        Task task = getTaskById(taskId);
        task.setStatus(newStatus);
        return taskRepository.save(task);
    }
    
    @Transactional
    public void deleteTask(Long taskId) {
        Task task = getTaskById(taskId);
        taskRepository.delete(task);
    }
}