package com.evolyn.evolyn_data_service.Tribes;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One person's membership of one tribe. status: ACTIVE, LEFT or REMOVED. */
@Entity
@Table(name = "tribe_members")
public class TribeMemberEntity {

    public static final String ACTIVE = "ACTIVE";
    public static final String LEFT = "LEFT";
    public static final String REMOVED = "REMOVED";

    @Id
    @Column(name = "member_id")
    private UUID memberId;

    @Column(name = "tribe_id", nullable = false)
    private UUID tribeId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    @Column(name = "removed_by")
    private UUID removedBy;

    public UUID getMemberId() { return memberId; }
    public void setMemberId(UUID memberId) { this.memberId = memberId; }

    public UUID getTribeId() { return tribeId; }
    public void setTribeId(UUID tribeId) { this.tribeId = tribeId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getJoinedAt() { return joinedAt; }
    public void setJoinedAt(Instant joinedAt) { this.joinedAt = joinedAt; }

    public Instant getLeftAt() { return leftAt; }
    public void setLeftAt(Instant leftAt) { this.leftAt = leftAt; }

    public UUID getRemovedBy() { return removedBy; }
    public void setRemovedBy(UUID removedBy) { this.removedBy = removedBy; }
}
