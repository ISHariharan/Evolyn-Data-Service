package com.evolyn.evolyn_data_service.Workspace.Creation;


import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/evolyn/api/internal")
public class WorkspaceCreation {
    
   private final WorkspaceCreationRepository workspaceCreationRepository;
   public WorkspaceCreation(WorkspaceCreationRepository workspaceCreationRepository){
    this.workspaceCreationRepository = workspaceCreationRepository;
   }

    @PostMapping("/store/workspace/creation")
    public ResponseEntity<Object> workSpaceCreation(@RequestBody WorkspaceCreationDTO Request) {
        WorkspaceDAO workspaceEntity = new WorkspaceDAO();
        workspaceEntity.setUserId(Request.getUserId());
        workspaceEntity.setWorkspaceId(Request.getWorkspaceUUID());
        workspaceEntity.setWorkspaceName(Request.getWorkspaceName());
        workspaceEntity.setWorkspaceDescription(Request.getWorkspaceDescription());
        workspaceEntity.setWorkspaceIcon(Request.getWorkspaceIcon());
        try {
            workspaceCreationRepository.save(workspaceEntity);
            return ResponseEntity
                    .status(HttpStatus.ACCEPTED)
                    .body(Map.of("message", "Workspace Created"));
        } catch (Exception e){
            System.out.println("Throwing Exception for Workspace Creation : " + e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Workspace Not Created"));
        }
    }
}
