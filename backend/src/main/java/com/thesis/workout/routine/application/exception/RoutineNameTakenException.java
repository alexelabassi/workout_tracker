package com.thesis.workout.routine.application.exception;

import com.thesis.workout.shared.web.exception.ApiException;
import org.springframework.http.HttpStatus;

public class RoutineNameTakenException extends ApiException {

    public RoutineNameTakenException() {
        super(HttpStatus.CONFLICT, "ROUTINE_NAME_TAKEN",
                "You already have a routine of this type with this name.");
    }
}
