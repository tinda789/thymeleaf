// dto/TaskCreateDto.java
package com.project.userauthservice.dto;

import com.project.userauthservice.entity.task.Task.TaskType;
import com.project.userauthservice.entity.task.Task.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskCreateDto {
    
    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(min = 3, max = 200, message = "Tiêu đề phải từ 3-200 ký tự")
    private String title;
    
    private String description;
    
    private Long assigneeId;
    private TaskType type = TaskType.TASK;
    private TaskPriority priority = TaskPriority.MEDIUM;
    private LocalDate dueDate;
    private Integer estimatedHours;
}