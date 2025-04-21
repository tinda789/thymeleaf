package com.project.userauthservice.repository;

import com.project.userauthservice.entity.task.Task;
import com.project.userauthservice.entity.project.Project;
import com.project.userauthservice.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProject(Project project);
    
    @Query("SELECT t FROM Task t WHERE t.project.id = :projectId")
    List<Task> findTasksByProjectId(@Param("projectId") Long projectId);
    
    List<Task> findByAssignee(User assignee);
    List<Task> findByReporter(User reporter);
    
    Optional<Task> findByTaskKey(String taskKey);
    boolean existsByTaskKey(String taskKey);
    
    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.id = :projectId")
    int countByProjectId(@Param("projectId") Long projectId);

    List<Task> findByProjectAndAssignee(Project project, User assignee);
}