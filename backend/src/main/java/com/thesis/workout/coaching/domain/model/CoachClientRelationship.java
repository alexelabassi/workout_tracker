package com.thesis.workout.coaching.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A coach↔client relationship and its lifecycle. The DB enforces one live (PENDING/ACTIVE) pairing
 * per (coach, client) via a partial unique index. Only an {@code ACTIVE} relationship authorizes a
 * coach to read that client's data (see {@code CoachAccess}); the relationship is the unit of
 * relationship-based access control.
 */
@Entity
@Table(name = "coach_client_relationships")
public class CoachClientRelationship {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "coach_user_id", nullable = false)
    private UUID coachUserId;

    @Column(name = "client_user_id", nullable = false)
    private UUID clientUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RelationshipStatus status;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CoachClientRelationship() {
    }

    /** Coach invites a client: a PENDING relationship created by the coach. */
    public static CoachClientRelationship invite(UUID coachUserId, UUID clientUserId, UUID createdByUserId) {
        CoachClientRelationship relationship = new CoachClientRelationship();
        relationship.id = UUID.randomUUID();
        relationship.coachUserId = coachUserId;
        relationship.clientUserId = clientUserId;
        relationship.status = RelationshipStatus.PENDING;
        relationship.createdByUserId = createdByUserId;
        return relationship;
    }

    public void accept(Instant when) {
        this.status = RelationshipStatus.ACTIVE;
        this.acceptedAt = when;
    }

    public void reject() {
        this.status = RelationshipStatus.REJECTED;
    }

    public void revoke(Instant when) {
        this.status = RelationshipStatus.REVOKED;
        this.revokedAt = when;
    }

    public boolean isPending() {
        return status == RelationshipStatus.PENDING;
    }

    public boolean isActive() {
        return status == RelationshipStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCoachUserId() {
        return coachUserId;
    }

    public UUID getClientUserId() {
        return clientUserId;
    }

    public RelationshipStatus getStatus() {
        return status;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
