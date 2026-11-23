package com.evolyn.evolyn_data_service.Tribes;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * An invitation to a tribe for one person (found by their Profile ID).
 * status: PENDING, ACCEPTED, DECLINED or REVOKED. A PENDING one past
 * expires_at counts as expired.
 */
@Entity
@Table(name = "tribe_invitations")
public class TribeInvitationEntity {

    public static final String PENDING = "PENDING";
    public static final String ACCEPTED = "ACCEPTED";
    public static final String DECLINED = "DECLINED";
    public static final String REVOKED = "REVOKED";

    @Id
    @Column(name = "invitation_id")
    private UUID invitationId;

    @Column(name = "tribe_id", nullable = false)
    private UUID tribeId;

    @Column(name = "invited_user_id", nullable = false)
    private UUID invitedUserId;

    @Column(name = "invited_by", nullable = false)
    private UUID invitedBy;

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    public UUID getInvitationId() { return invitationId; }
    public void setInvitationId(UUID invitationId) { this.invitationId = invitationId; }

    public UUID getTribeId() { return tribeId; }
    public void setTribeId(UUID tribeId) { this.tribeId = tribeId; }

    public UUID getInvitedUserId() { return invitedUserId; }
    public void setInvitedUserId(UUID invitedUserId) { this.invitedUserId = invitedUserId; }

    public UUID getInvitedBy() { return invitedBy; }
    public void setInvitedBy(UUID invitedBy) { this.invitedBy = invitedBy; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getRespondedAt() { return respondedAt; }
    public void setRespondedAt(Instant respondedAt) { this.respondedAt = respondedAt; }
}
