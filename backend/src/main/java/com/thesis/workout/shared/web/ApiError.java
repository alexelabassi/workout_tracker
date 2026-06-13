package com.thesis.workout.shared.web;

import java.time.Instant;

/**
 * Consistent error envelope returned for every failed request, matching the shape
 * documented in docs/API_CONTRACT.md.
 */
public record ApiError(Instant timestamp, int status, String error, String message, String path) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path);
    }
}
