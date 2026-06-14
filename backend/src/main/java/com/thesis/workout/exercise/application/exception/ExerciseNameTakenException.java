package com.thesis.workout.exercise.application.exception;

import com.thesis.workout.shared.web.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ExerciseNameTakenException extends ApiException {

    public ExerciseNameTakenException() {
        super(HttpStatus.CONFLICT, "EXERCISE_NAME_TAKEN", "You already have an exercise with this name.");
    }
}
