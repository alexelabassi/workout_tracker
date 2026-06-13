package com.thesis.workout.auth.infrastructure.security;

import com.thesis.workout.auth.domain.model.Role;
import java.util.UUID;

/**
 * Authenticated caller derived from a verified access token. Stored as the principal of
 * the Spring Security {@code Authentication} so downstream controllers can resolve the
 * acting user without another database lookup.
 */
public record UserPrincipal(UUID id, String email, Role role) {
}
