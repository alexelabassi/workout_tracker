package com.thesis.workout.coaching.application;

import com.thesis.workout.coaching.application.exception.ClientNotFoundException;
import com.thesis.workout.coaching.domain.model.CoachClientRelationship;
import com.thesis.workout.coaching.domain.model.RelationshipStatus;
import com.thesis.workout.coaching.infrastructure.repository.CoachClientRelationshipRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The relationship-based access control (ReBAC) gate for coach reads. RBAC (ROLE_COACH on
 * {@code /api/coach/**}) is necessary but not sufficient: every coach read must also pass through
 * here, which requires an ACTIVE coach↔client relationship. A missing relationship yields a 404
 * (not 403) so a coach cannot probe which users exist or have data.
 */
@Service
public class CoachAccess {

    private final CoachClientRelationshipRepository relationshipRepository;

    public CoachAccess(CoachClientRelationshipRepository relationshipRepository) {
        this.relationshipRepository = relationshipRepository;
    }

    @Transactional(readOnly = true)
    public CoachClientRelationship requireActiveClient(UUID coachUserId, UUID clientUserId) {
        return relationshipRepository
                .findByCoachUserIdAndClientUserIdAndStatus(coachUserId, clientUserId, RelationshipStatus.ACTIVE)
                .orElseThrow(ClientNotFoundException::new);
    }
}
