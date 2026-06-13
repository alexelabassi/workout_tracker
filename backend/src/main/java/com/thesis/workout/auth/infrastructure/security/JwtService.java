package com.thesis.workout.auth.infrastructure.security;

import com.thesis.workout.auth.domain.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Issues and verifies short-lived HS256 access tokens. Refresh tokens are intentionally
 * opaque (random + hashed in the database), so they are not minted here.
 */
@Service
public class JwtService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";

    private final SecretKey signingKey;
    private final String issuer;
    private final Duration accessTtl;

    public JwtService(AuthSecurityProperties properties) {
        this.signingKey = Keys.hmacShaKeyFor(properties.jwt().secret().getBytes(StandardCharsets.UTF_8));
        this.issuer = properties.jwt().issuer();
        this.accessTtl = properties.jwt().accessTtl();
    }

    public String issueAccessToken(UUID userId, String email, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Verifies signature, issuer, expiry and token type. Throws {@link JwtException} for
     * any invalid token so callers can treat the request as unauthenticated.
     */
    public AccessTokenClaims parseAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new JwtException("Unexpected token type");
        }

        try {
            UUID userId = UUID.fromString(claims.getSubject());
            String email = claims.get(CLAIM_EMAIL, String.class);
            Role role = Role.valueOf(claims.get(CLAIM_ROLE, String.class));
            return new AccessTokenClaims(userId, email, role);
        } catch (IllegalArgumentException ex) {
            throw new JwtException("Malformed token claims", ex);
        }
    }

    public Duration accessTtl() {
        return accessTtl;
    }

    public record AccessTokenClaims(UUID userId, String email, Role role) {
    }
}
