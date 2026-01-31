package com.evolyn.evolyn_data_service.Workspace.Creation;

import java.util.UUID;

public class WorkspaceDetailsDTO {

    private UUID workspaceId;
    private String workspaceName;
    private String workspaceDescription;
    private String workspaceIcon;

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(UUID workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    public String getWorkspaceDescription() {
        return workspaceDescription;
    }

    public void setWorkspaceDescription(String workspaceDescription) {
        this.workspaceDescription = workspaceDescription;
    }

    public String getWorkspaceIcon() {
        return workspaceIcon;
    }

    public void setWorkspaceIcon(String workspaceIcon) {
        this.workspaceIcon = workspaceIcon;
    }
}

