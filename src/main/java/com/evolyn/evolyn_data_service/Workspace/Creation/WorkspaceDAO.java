package com.evolyn.evolyn_data_service.Workspace.Creation;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "workspace",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "workspace_id"),
        @UniqueConstraint(columnNames = "workspace_name")
    }
)

public class WorkspaceDAO {
    @Column(name="user_id")
    private UUID userId;

    @Id
    @Column(name="workspace_id")
    private UUID workspaceId;

    @Column(name="workspace_name")
    private String workspaceName;

    @Column(name="workspace_description")
    private String workspaceDescription;

    @Column(name = "workspace_icon")
    private String workspaceIcon;

    public void setUserId(UUID userUUID){
        this.userId = userUUID;
    }

    public UUID getUserId(){
        return userId;
    }

    public void setWorkspaceId(UUID workspaceUUID){
        this.workspaceId = workspaceUUID;
    }

    public UUID getWorkspaceId(){
        return workspaceId;
    }

    public void setWorkspaceName(String workspaceName){
        this.workspaceName = workspaceName;
    }

    public String getWorkspaceName(){
        return workspaceName;
    }

    public void setWorkspaceDescription(String workspaceDescription){
        this.workspaceDescription = workspaceDescription;
    }

    public String getWorkspaceDescription(){
        return workspaceDescription;
    }

    public void setWorkspaceIcon(String workspaceIcon){
        this.workspaceIcon = workspaceIcon;
    }

    public String getWorkspaceIcon(){
        return workspaceIcon;
    }
}
