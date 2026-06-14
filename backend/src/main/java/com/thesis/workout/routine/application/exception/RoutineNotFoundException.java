package com.thesis.workout.routine.application.exception;

import com.thesis.workout.shared.web.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a routine cannot be found for the current user. Returning 404 (rather than 403)
 * for a routine owned by someone else avoids leaking its existence (no IDOR signal).
 */
public class RoutineNotFoundException extends ApiException {

    public RoutineNotFoundException() {
        super(HttpStatus.NOT_FOUND, "ROUTINE_NOT_FOUND", "Routine not found.");
    }
}
