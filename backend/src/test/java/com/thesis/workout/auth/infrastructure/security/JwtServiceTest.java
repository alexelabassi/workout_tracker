package com.thesis.workout.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.thesis.workout.auth.domain.model.Role;
import io.jsonwebtoken.JwtException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET =
            "test-secret-which-is-definitely-long-enough-for-hs256-0123456789-abcdef";
    private static final String ISSUER = "workout-thesis";

    private JwtService serviceWith(Duration accessTtl, String secret) {
        AuthSecurityProperties properties = new AuthSecurityProperties(
                new AuthSecurityProperties.Jwt(secret, ISSUER, accessTtl, Duration.ofDays(14)),
                new AuthSecurityProperties.Cookie(false, "refresh_token"),
                new AuthSecurityProperties.Cors(List.of()));
        return new JwtService(properties);
    }

    @Test
    void signsAndParsesRoundTrip() {
        JwtService service = serviceWith(Duration.ofMinutes(15), SECRET);
        UUID userId = UUID.randomUUID();

        String token = service.issueAccessToken(userId, "user@example.com", Role.COACH);
        JwtService.AccessTokenClaims claims = service.parseAccessToken(token);

        assertThat(claims.userId()).isEqualTo(userId);
        assertThat(claims.email()).isEqualTo("user@example.com");
        assertThat(claims.role()).isEqualTo(Role.COACH);
    }

    @Test
    void rejectsExpiredToken() {
        JwtService service = serviceWith(Duration.ofMinutes(-1), SECRET);
        String token = service.issueAccessToken(UUID.randomUUID(), "user@example.com", Role.USER);

        assertThatThrownBy(() -> service.parseAccessToken(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTamperedToken() {
        JwtService service = serviceWith(Duration.ofMinutes(15), SECRET);
        String token = service.issueAccessToken(UUID.randomUUID(), "user@example.com", Role.USER);

        char last = token.charAt(token.length() - 1);
        char replacement = last == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, token.length() - 1) + replacement;

        assertThatThrownBy(() -> service.parseAccessToken(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTokenSignedWithDifferentKey() {
        JwtService signer = serviceWith(Duration.ofMinutes(15), SECRET);
        JwtService verifier = serviceWith(Duration.ofMinutes(15),
                "another-secret-which-is-also-long-enough-for-hs256-9876543210-zyxwvu");
        String token = signer.issueAccessToken(UUID.randomUUID(), "user@example.com", Role.USER);

        assertThatThrownBy(() -> verifier.parseAccessToken(token)).isInstanceOf(JwtException.class);
    }
}
