package com.thesis.workout.auth.application;

import com.thesis.workout.auth.application.exception.EmailAlreadyUsedException;
import com.thesis.workout.auth.application.exception.InvalidCredentialsException;
import com.thesis.workout.auth.application.exception.InvalidRefreshTokenException;
import com.thesis.workout.auth.domain.model.AppUser;
import com.thesis.workout.auth.domain.model.RefreshToken;
import com.thesis.workout.auth.domain.model.Role;
import com.thesis.workout.auth.infrastructure.repository.AppUserRepository;
import com.thesis.workout.auth.infrastructure.repository.RefreshTokenRepository;
import com.thesis.workout.auth.infrastructure.security.AuthSecurityProperties;
import com.thesis.workout.auth.infrastructure.security.JwtService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core authentication use cases. Passwords are stored as BCrypt hashes; refresh tokens are
 * opaque random values stored only as SHA-256 hashes, and rotated on every refresh inside a
 * single transaction so a stolen-and-reused token is detected (the old hash is revoked).
 */
@Service
public class AuthService {

    private static final int REFRESH_TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AppUserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Duration refreshTtl;

    public AuthService(AppUserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthSecurityProperties properties) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTtl = properties.jwt().refreshTtl();
    }

    @Transactional
    public AuthResult register(String email, String rawPassword, String displayName) {
        String normalizedEmail = email.trim();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyUsedException();
        }
        AppUser user = AppUser.create(
                normalizedEmail,
                passwordEncoder.encode(rawPassword),
                normalizeDisplayName(displayName),
                Role.USER);
        userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional
    public AuthResult login(String email, String rawPassword) {
        AppUser user = userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return issueTokens(user);
    }

    /**
     * Validates the presented refresh token, revokes it, and issues a brand-new access +
     * refresh token pair. Rotation and revocation happen in the same transaction.
     */
    @Transactional
    public AuthResult refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        Instant now = Instant.now();
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hashToken(rawRefreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);
        if (!existing.isActive(now)) {
            throw new InvalidRefreshTokenException();
        }
        existing.revoke(now);
        AppUser user = userRepository.findById(existing.getUserId())
                .orElseThrow(InvalidRefreshTokenException::new);
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hashToken(rawRefreshToken))
                .ifPresent(token -> token.revoke(Instant.now()));
    }

    @Transactional(readOnly = true)
    public AppUser currentUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(InvalidCredentialsException::new);
    }

    private AuthResult issueTokens(AppUser user) {
        String accessToken = jwtService.issueAccessToken(user.getId(), user.getEmail(), user.getRole());
        String rawRefreshToken = generateRefreshTokenValue();
        Instant expiresAt = Instant.now().plus(refreshTtl);
        RefreshToken refreshToken = RefreshToken.issue(user.getId(), hashToken(rawRefreshToken), expiresAt);
        refreshTokenRepository.save(refreshToken);
        return new AuthResult(user, accessToken, jwtService.accessTtl(), rawRefreshToken, expiresAt);
    }

    private static String normalizeDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return null;
        }
        return displayName.trim();
    }

    private static String generateRefreshTokenValue() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is required but unavailable", ex);
        }
    }
}
