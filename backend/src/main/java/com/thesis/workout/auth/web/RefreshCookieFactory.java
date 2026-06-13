package com.thesis.workout.auth.web;

import com.thesis.workout.auth.infrastructure.security.AuthSecurityProperties;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Builds the refresh-token cookie. It is HttpOnly and SameSite=Strict, scoped to
 * {@code /api/auth} so it is only ever sent to the refresh/logout endpoints. The Secure
 * flag is profile-driven: false on local HTTP, true on production HTTPS.
 */
@Component
public class RefreshCookieFactory {

    private static final String PATH = "/api/auth";
    private static final String SAME_SITE = "Strict";

    private final String cookieName;
    private final boolean secure;

    public RefreshCookieFactory(AuthSecurityProperties properties) {
        this.cookieName = properties.cookie().name();
        this.secure = properties.cookie().secure();
    }

    public ResponseCookie create(String rawToken, Instant expiresAt) {
        long maxAgeSeconds = Math.max(0, Duration.between(Instant.now(), expiresAt).getSeconds());
        return base(rawToken).maxAge(maxAgeSeconds).build();
    }

    public ResponseCookie clearCookie() {
        return base("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(cookieName, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(SAME_SITE)
                .path(PATH);
    }
}
