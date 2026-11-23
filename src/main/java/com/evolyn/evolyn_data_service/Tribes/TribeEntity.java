package com.evolyn.evolyn_data_service.Tribes;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A group of friends working out together. Table: V8__create_tribes.sql */
@Entity
@Table(name = "tribes")
public class TribeEntity {

    @Id
    @Column(name = "tribe_id")
    private UUID tribeId;

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @Column(name = "description", length = 300)
    private String description;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "join_code", length = 12, unique = true)
    private String joinCode;

    @Column(name = "join_code_expires_at")
    private Instant joinCodeExpiresAt;

    public UUID getTribeId() { return tribeId; }
    public void setTribeId(UUID tribeId) { this.tribeId = tribeId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getArchivedAt() { return archivedAt; }
    public void setArchivedAt(Instant archivedAt) { this.archivedAt = archivedAt; }

    public String getJoinCode() { return joinCode; }
    public void setJoinCode(String joinCode) { this.joinCode = joinCode; }

    public Instant getJoinCodeExpiresAt() { return joinCodeExpiresAt; }
    public void setJoinCodeExpiresAt(Instant joinCodeExpiresAt) { this.joinCodeExpiresAt = joinCodeExpiresAt; }
}
