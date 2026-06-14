package com.thesis.workout.exercise.application.exception;

import com.thesis.workout.shared.web.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a custom exercise cannot be found for the current user. Returning 404 (rather
 * than 403) for an exercise owned by someone else avoids leaking its existence (no IDOR signal).
 */
public class ExerciseNotFoundException extends ApiException {

    public ExerciseNotFoundException() {
        super(HttpStatus.NOT_FOUND, "EXERCISE_NOT_FOUND", "Exercise not found.");
    }
}
