package com.thesis.workout.coaching.web.dto;

import com.thesis.workout.coaching.domain.model.RelationshipStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * A client's view of a relationship — used both for a pending invite (status PENDING, {@code since}
 * is the invite time) and for an active coach (status ACTIVE, {@code since} is the accepted time).
 */
public record CoachRelationshipResponse(
        UUID relationshipId,
        UUID coachUserId,
        String coachDisplayName,
        String coachEmail,
        RelationshipStatus status,
        Instant since) {
}
