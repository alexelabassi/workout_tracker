package com.thesis.workout.auth.web.dto;

import com.thesis.workout.auth.application.AuthResult;

/**
 * Login/register/refresh body. The access token is returned here for the SPA to hold in
 * memory; the refresh token is delivered separately as an HttpOnly cookie.
 */
public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {

    public static AuthResponse from(AuthResult result) {
        return new AuthResponse(
                result.accessToken(),
                "Bearer",
                result.accessTtl().getSeconds(),
                UserResponse.from(result.user()));
    }
}
