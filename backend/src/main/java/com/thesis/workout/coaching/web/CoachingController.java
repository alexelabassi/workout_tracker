package com.thesis.workout.coaching.web;

import com.thesis.workout.auth.infrastructure.security.UserPrincipal;
import com.thesis.workout.coaching.application.RelationshipService;
import com.thesis.workout.coaching.web.dto.CoachRelationshipResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Client-facing relationship management. Any authenticated user can see/accept/reject invites sent
 * to them, list their active coaches, and revoke a coach's access at any time. The acting user is
 * always taken from the JWT, and relationships are resolved by id + the caller's user id.
 */
@RestController
@RequestMapping("/api/coaching")
public class CoachingController {

    private final RelationshipService relationshipService;

    public CoachingController(RelationshipService relationshipService) {
        this.relationshipService = relationshipService;
    }

    @GetMapping("/invites")
    public List<CoachRelationshipResponse> pendingInvites(@AuthenticationPrincipal UserPrincipal principal) {
        return relationshipService.pendingInvites(principal.id());
    }

    @PostMapping("/invites/{relationshipId}/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void accept(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID relationshipId) {
        relationshipService.accept(principal.id(), relationshipId);
    }

    @PostMapping("/invites/{relationshipId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID relationshipId) {
        relationshipService.reject(principal.id(), relationshipId);
    }

    @GetMapping("/coaches")
    public List<CoachRelationshipResponse> activeCoaches(@AuthenticationPrincipal UserPrincipal principal) {
        return relationshipService.activeCoaches(principal.id());
    }

    @DeleteMapping("/coaches/{relationshipId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID relationshipId) {
        relationshipService.revoke(principal.id(), relationshipId);
    }
}
