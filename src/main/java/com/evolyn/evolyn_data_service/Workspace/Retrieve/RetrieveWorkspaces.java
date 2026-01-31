package com.evolyn.evolyn_data_service.Workspace.Retrieve;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import com.evolyn.evolyn_data_service.Workspace.Creation.WorkspaceDAO;

@RestController
@RequestMapping("/evolyn/api/internal")
public class RetrieveWorkspaces {
    private final RetrieveWorkspaceRepository retrieveWorkspaceRepository;

    public RetrieveWorkspaces(RetrieveWorkspaceRepository retrieveWorkspaceRepository) {
        this.retrieveWorkspaceRepository = retrieveWorkspaceRepository;
    }

    @GetMapping(value = "/retrieve/workspaces", produces = "application/json")
    public ResponseEntity<List<WorkspaceDAO>> retrieveWorkspacesForUser(@RequestParam("userId") String userId) {
        try {
            UUID userUuid = UUID.fromString(userId);
            List<WorkspaceDAO> workspaces = retrieveWorkspaceRepository.findByUserId(userUuid);
            return ResponseEntity.ok(workspaces);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
