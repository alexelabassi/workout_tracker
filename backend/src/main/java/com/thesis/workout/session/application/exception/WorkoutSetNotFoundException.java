package com.thesis.workout.session.application.exception;

import com.thesis.workout.shared.web.exception.ApiException;
import org.springframework.http.HttpStatus;

public class WorkoutSetNotFoundException extends ApiException {

    public WorkoutSetNotFoundException() {
        super(HttpStatus.NOT_FOUND, "SET_NOT_FOUND", "Workout set not found.");
    }
}
