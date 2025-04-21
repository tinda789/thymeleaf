package com.project.userauthservice.dto;

import com.project.userauthservice.entity.task.Task.TaskType;
import com.project.userauthservice.entity.task.Task.TaskPriority;
import com.project.userauthservice.entity.task.Task.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskUpdateDto {
    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(min = 3, max = 200, message = "Tiêu đề phải từ 3-200 ký tự")
    private String title;
    
    private String description;
    private Long assigneeId;  // ID của người được gán task
    private TaskType type;
    private TaskPriority priority;
    private TaskStatus status;
    private LocalDate dueDate;
    private Integer estimatedHours;
    private Integer actualHours;
}