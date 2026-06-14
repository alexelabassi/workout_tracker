package com.thesis.workout.session.application.exception;

import com.thesis.workout.shared.web.exception.ApiException;
import org.springframework.http.HttpStatus;

public class WorkoutSessionNotFoundException extends ApiException {

    public WorkoutSessionNotFoundException() {
        super(HttpStatus.NOT_FOUND, "WORKOUT_NOT_FOUND", "Workout session not found.");
    }
}
