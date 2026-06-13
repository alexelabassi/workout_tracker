package com.thesis.workout.auth.infrastructure.security;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed binding for the {@code app.security} configuration tree. The cookie
 * {@code secure} flag is profile-driven (false on local HTTP, true on production HTTPS)
 * and the JWT secret is sourced from {@code JWT_SECRET} with a long dev fallback.
 */
@ConfigurationProperties(prefix = "app.security")
public record AuthSecurityProperties(Jwt jwt, Cookie cookie, Cors cors) {

    public record Jwt(String secret, String issuer, Duration accessTtl, Duration refreshTtl) {
    }

    public record Cookie(boolean secure, String name) {
    }

    public record Cors(List<String> allowedOrigins) {
    }
}
