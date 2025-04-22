package com.project.userauthservice.repository;

import com.project.userauthservice.entity.workspace.WorkspaceMember;
import com.project.userauthservice.entity.workspace.Workspace;
import com.project.userauthservice.entity.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {
    int countByWorkspace(Workspace workspace);
    
    @EntityGraph(attributePaths = {"user", "workspace"})
    List<WorkspaceMember> findByWorkspace(Workspace workspace);

    List<WorkspaceMember> findByUser(User user);
    Optional<WorkspaceMember> findByWorkspaceAndUser(Workspace workspace, User user);
    boolean existsByWorkspaceAndUser(Workspace workspace, User user);

    void deleteByWorkspace(Workspace workspace);
}