// repository/ProjectRepository.java
package com.project.userauthservice.repository;

import com.project.userauthservice.entity.project.Project;
import com.project.userauthservice.entity.workspace.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByWorkspace(Workspace workspace);
    Optional<Project> findByProjectKey(String projectKey);
    boolean existsByProjectKey(String projectKey);
    int countByWorkspace(Workspace workspace);
}