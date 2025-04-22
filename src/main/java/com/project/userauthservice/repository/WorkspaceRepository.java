package com.project.userauthservice.repository;

import com.project.userauthservice.entity.workspace.Workspace;
import com.project.userauthservice.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
    Optional<Workspace> findByWorkspaceKey(String workspaceKey);
    List<Workspace> findByOwner(User owner);
    boolean existsByWorkspaceKey(String workspaceKey);
    int countByOwner(User owner);  // Thêm method này
    
}