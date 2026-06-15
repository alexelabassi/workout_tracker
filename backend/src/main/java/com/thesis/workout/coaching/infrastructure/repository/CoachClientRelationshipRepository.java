package com.thesis.workout.coaching.infrastructure.repository;

import com.thesis.workout.coaching.domain.model.CoachClientRelationship;
import com.thesis.workout.coaching.domain.model.RelationshipStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoachClientRelationshipRepository extends JpaRepository<CoachClientRelationship, UUID> {

    /** The authorization gate: an ACTIVE relationship between this coach and client. */
    Optional<CoachClientRelationship> findByCoachUserIdAndClientUserIdAndStatus(
            UUID coachUserId, UUID clientUserId, RelationshipStatus status);

    /** A coach's relationships in a given status (e.g. ACTIVE clients). */
    List<CoachClientRelationship> findByCoachUserIdAndStatusOrderByAcceptedAtDesc(
            UUID coachUserId, RelationshipStatus status);

    /** A client's relationships in a given status (e.g. PENDING invites, ACTIVE coaches). */
    List<CoachClientRelationship> findByClientUserIdAndStatusOrderByCreatedAtDesc(
            UUID clientUserId, RelationshipStatus status);

    /** A specific relationship owned (as client) by the caller — for accept/reject/revoke. */
    Optional<CoachClientRelationship> findByIdAndClientUserId(UUID id, UUID clientUserId);

    /** A specific relationship owned (as coach) by the caller — for coach-side revoke. */
    Optional<CoachClientRelationship> findByIdAndCoachUserId(UUID id, UUID coachUserId);

    /** Duplicate-invite guard (the partial unique index is the hard guarantee). */
    boolean existsByCoachUserIdAndClientUserIdAndStatusIn(
            UUID coachUserId, UUID clientUserId, Collection<RelationshipStatus> statuses);
}
