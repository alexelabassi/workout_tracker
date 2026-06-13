package com.thesis.workout.auth.application;

import com.thesis.workout.auth.domain.model.AppUser;
import java.time.Duration;
import java.time.Instant;

/**
 * Outcome of a successful register/login/refresh: the authenticated user, a freshly minted
 * access token (with its lifetime) and the raw refresh token plus its expiry. The web layer
 * turns the raw refresh token into an HttpOnly cookie and never exposes it in the body.
 */
public record AuthResult(
        AppUser user,
        String accessToken,
        Duration accessTtl,
        String refreshToken,
        Instant refreshExpiresAt) {
}
