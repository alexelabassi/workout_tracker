package com.thesis.workout.coaching.application;

import com.thesis.workout.auth.domain.model.AppUser;
import com.thesis.workout.auth.infrastructure.repository.AppUserRepository;
import com.thesis.workout.coaching.application.exception.InvalidRelationshipStateException;
import com.thesis.workout.coaching.application.exception.RelationshipNotFoundException;
import com.thesis.workout.coaching.domain.model.CoachClientRelationship;
import com.thesis.workout.coaching.domain.model.RelationshipStatus;
import com.thesis.workout.coaching.infrastructure.repository.CoachClientRelationshipRepository;
import com.thesis.workout.coaching.web.dto.CoachRelationshipResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Client-side relationship management: see pending invites, accept/reject them, list active coaches,
 * and revoke access. The client is always the owner of the relationship rows they act on
 * (looked up by id + client user id), so a client can never touch someone else's relationship.
 */
@Service
public class RelationshipService {

    private final CoachClientRelationshipRepository relationshipRepository;
    private final AppUserRepository appUserRepository;

    public RelationshipService(CoachClientRelationshipRepository relationshipRepository,
            AppUserRepository appUserRepository) {
        this.relationshipRepository = relationshipRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public List<CoachRelationshipResponse> pendingInvites(UUID clientUserId) {
        return toResponses(relationshipRepository
                .findByClientUserIdAndStatusOrderByCreatedAtDesc(clientUserId, RelationshipStatus.PENDING), true);
    }

    @Transactional(readOnly = true)
    public List<CoachRelationshipResponse> activeCoaches(UUID clientUserId) {
        return toResponses(relationshipRepository
                .findByClientUserIdAndStatusOrderByCreatedAtDesc(clientUserId, RelationshipStatus.ACTIVE), false);
    }

    @Transactional
    public void accept(UUID clientUserId, UUID relationshipId) {
        CoachClientRelationship relationship = requireOwned(relationshipId, clientUserId);
        if (!relationship.isPending()) {
            throw new InvalidRelationshipStateException("Only a pending invite can be accepted.");
        }
        relationship.accept(Instant.now());
    }

    @Transactional
    public void reject(UUID clientUserId, UUID relationshipId) {
        CoachClientRelationship relationship = requireOwned(relationshipId, clientUserId);
        if (!relationship.isPending()) {
            throw new InvalidRelationshipStateException("Only a pending invite can be rejected.");
        }
        relationship.reject();
    }

    @Transactional
    public void revoke(UUID clientUserId, UUID relationshipId) {
        CoachClientRelationship relationship = requireOwned(relationshipId, clientUserId);
        if (!relationship.isActive()) {
            throw new InvalidRelationshipStateException("Only an active relationship can be revoked.");
        }
        relationship.revoke(Instant.now());
    }

    private CoachClientRelationship requireOwned(UUID relationshipId, UUID clientUserId) {
        return relationshipRepository.findByIdAndClientUserId(relationshipId, clientUserId)
                .orElseThrow(RelationshipNotFoundException::new);
    }

    private List<CoachRelationshipResponse> toResponses(List<CoachClientRelationship> relationships, boolean pending) {
        if (relationships.isEmpty()) {
            return List.of();
        }
        Map<UUID, AppUser> coaches = appUserRepository
                .findAllById(relationships.stream().map(CoachClientRelationship::getCoachUserId).toList())
                .stream().collect(Collectors.toMap(AppUser::getId, Function.identity()));
        return relationships.stream().map(relationship -> {
            AppUser coach = coaches.get(relationship.getCoachUserId());
            Instant since = pending ? relationship.getCreatedAt() : relationship.getAcceptedAt();
            return new CoachRelationshipResponse(relationship.getId(), relationship.getCoachUserId(),
                    coach != null ? coach.getDisplayName() : null,
                    coach != null ? coach.getEmail() : null,
                    relationship.getStatus(), since);
        }).toList();
    }
}
