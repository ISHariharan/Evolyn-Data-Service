package com.evolyn.evolyn_data_service.Workspace.Creation;

import java.util.UUID;

public class WorkspaceCreationDTO {
    private UUID userId;
    private WorkspaceDetailsDTO workspaceCreationDetails;

    public void setUserId(UUID userUuid){
        this.userId = userUuid;
    }

    public UUID getUserId(){
        return userId;
    }

    public void setWorkspaceDetails(WorkspaceDetailsDTO workspaceDetails){
        this.workspaceCreationDetails = workspaceDetails;
    }

    public Object getWorkspaceDetails(){
        return workspaceCreationDetails;
    }

    public UUID getWorkspaceUUID(){
        return workspaceCreationDetails.getWorkspaceId();
    }

    public String getWorkspaceName(){
        return workspaceCreationDetails.getWorkspaceName();
    }

    public String getWorkspaceDescription(){
        return workspaceCreationDetails.getWorkspaceDescription();
    }

    public String getWorkspaceIcon(){
        return workspaceCreationDetails.getWorkspaceIcon();
    }
}
