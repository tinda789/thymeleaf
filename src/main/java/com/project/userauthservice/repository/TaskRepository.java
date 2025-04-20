// repository/TaskRepository.java
package com.project.userauthservice.repository;

import com.project.userauthservice.entity.task.Task;
import com.project.userauthservice.entity.project.Project;
import com.project.userauthservice.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProject(Project project);
    List<Task> findByAssignee(User assignee);
    List<Task> findByReporter(User reporter);
    Optional<Task> findByTaskKey(String taskKey);
    boolean existsByTaskKey(String taskKey);
}