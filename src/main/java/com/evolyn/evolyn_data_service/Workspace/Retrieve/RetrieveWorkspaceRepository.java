package com.evolyn.evolyn_data_service.Workspace.Retrieve;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.evolyn.evolyn_data_service.Workspace.Creation.WorkspaceDAO;

public interface RetrieveWorkspaceRepository extends JpaRepository<WorkspaceDAO, UUID>{
    List<WorkspaceDAO> findByUserId(UUID userId);
}
