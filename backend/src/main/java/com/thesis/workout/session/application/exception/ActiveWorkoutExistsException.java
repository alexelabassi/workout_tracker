package com.thesis.workout.session.application.exception;

import com.thesis.workout.shared.web.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ActiveWorkoutExistsException extends ApiException {

    public ActiveWorkoutExistsException() {
        super(HttpStatus.CONFLICT, "ACTIVE_WORKOUT_EXISTS", "You already have a workout in progress.");
    }
}
