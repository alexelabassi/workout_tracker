package com.thesis.workout.gym.application.exception;

import com.thesis.workout.shared.web.exception.ApiException;
import org.springframework.http.HttpStatus;

public class GymNameTakenException extends ApiException {

    public GymNameTakenException() {
        super(HttpStatus.CONFLICT, "GYM_NAME_TAKEN", "You already have a gym with this name.");
    }
}
