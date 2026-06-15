package com.thesis.workout.coaching.application;

import com.thesis.workout.auth.domain.model.AppUser;
import com.thesis.workout.auth.infrastructure.repository.AppUserRepository;
import com.thesis.workout.coaching.application.exception.ClientNotFoundException;
import com.thesis.workout.coaching.application.exception.DuplicateRelationshipException;
import com.thesis.workout.coaching.application.exception.InviteTargetNotFoundException;
import com.thesis.workout.coaching.application.exception.SelfCoachingNotAllowedException;
import com.thesis.workout.coaching.domain.model.CoachClientRelationship;
import com.thesis.workout.coaching.domain.model.RelationshipStatus;
import com.thesis.workout.coaching.infrastructure.repository.CoachClientRelationshipRepository;
import com.thesis.workout.coaching.web.dto.ClientSummaryResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coach-side relationship operations: invite a client by email, list active clients, revoke a client. */
@Service
public class CoachClientService {

    private final CoachClientRelationshipRepository relationshipRepository;
    private final AppUserRepository appUserRepository;

    public CoachClientService(CoachClientRelationshipRepository relationshipRepository,
            AppUserRepository appUserRepository) {
        this.relationshipRepository = relationshipRepository;
        this.appUserRepository = appUserRepository;
    }

    /** In-app invite by email. No email is sent and no account is created; an unknown email is a clean 404. */
    @Transactional
    public ClientSummaryResponse invite(UUID coachUserId, String clientEmail) {
        AppUser client = appUserRepository.findByEmailIgnoreCase(clientEmail.trim())
                .orElseThrow(InviteTargetNotFoundException::new);
        if (client.getId().equals(coachUserId)) {
            throw new SelfCoachingNotAllowedException();
        }
        if (relationshipRepository.existsByCoachUserIdAndClientUserIdAndStatusIn(
                coachUserId, client.getId(), List.of(RelationshipStatus.PENDING, RelationshipStatus.ACTIVE))) {
            throw new DuplicateRelationshipException();
        }
        try {
            // The partial unique index ux on (coach,client) WHERE status IN (PENDING,ACTIVE) is the
            // real guarantee against a concurrent duplicate invite.
            relationshipRepository.saveAndFlush(
                    CoachClientRelationship.invite(coachUserId, client.getId(), coachUserId));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateRelationshipException();
        }
        return new ClientSummaryResponse(client.getId(), client.getDisplayName(), client.getEmail(), null);
    }

    @Transactional(readOnly = true)
    public List<ClientSummaryResponse> listActiveClients(UUID coachUserId) {
        List<CoachClientRelationship> relationships = relationshipRepository
                .findByCoachUserIdAndStatusOrderByAcceptedAtDesc(coachUserId, RelationshipStatus.ACTIVE);
        if (relationships.isEmpty()) {
            return List.of();
        }
        Map<UUID, AppUser> clients = appUserRepository
                .findAllById(relationships.stream().map(CoachClientRelationship::getClientUserId).toList())
                .stream().collect(Collectors.toMap(AppUser::getId, Function.identity()));
        return relationships.stream().map(relationship -> {
            AppUser client = clients.get(relationship.getClientUserId());
            return new ClientSummaryResponse(relationship.getClientUserId(),
                    client != null ? client.getDisplayName() : null,
                    client != null ? client.getEmail() : null,
                    relationship.getAcceptedAt());
        }).toList();
    }

    /** Coach ends an active relationship with a client. */
    @Transactional
    public void revokeClient(UUID coachUserId, UUID clientUserId) {
        CoachClientRelationship relationship = relationshipRepository
                .findByCoachUserIdAndClientUserIdAndStatus(coachUserId, clientUserId, RelationshipStatus.ACTIVE)
                .orElseThrow(ClientNotFoundException::new);
        relationship.revoke(Instant.now());
    }
}
