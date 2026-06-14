package com.thesis.workout.gym.application.exception;

import com.thesis.workout.shared.web.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a gym cannot be found for the current user. Returning 404 (rather than 403) for a
 * gym owned by someone else avoids leaking its existence (no IDOR signal).
 */
public class GymNotFoundException extends ApiException {

    public GymNotFoundException() {
        super(HttpStatus.NOT_FOUND, "GYM_NOT_FOUND", "Gym not found.");
    }
}
