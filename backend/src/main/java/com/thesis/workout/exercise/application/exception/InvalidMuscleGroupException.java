package com.thesis.workout.exercise.application.exception;

import com.thesis.workout.shared.web.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Raised when an exercise's muscle-group assignments reference an unknown code or repeat the
 * same code. Bean Validation covers shape (non-blank, role present); this guards the values.
 */
public class InvalidMuscleGroupException extends ApiException {

    public InvalidMuscleGroupException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_MUSCLE_GROUP", message);
    }
}
