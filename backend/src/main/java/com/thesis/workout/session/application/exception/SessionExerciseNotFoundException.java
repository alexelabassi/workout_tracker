package com.thesis.workout.session.application.exception;

import com.thesis.workout.shared.web.exception.ApiException;
import org.springframework.http.HttpStatus;

public class SessionExerciseNotFoundException extends ApiException {

    public SessionExerciseNotFoundException() {
        super(HttpStatus.NOT_FOUND, "SESSION_EXERCISE_NOT_FOUND", "Session exercise not found.");
    }
}
