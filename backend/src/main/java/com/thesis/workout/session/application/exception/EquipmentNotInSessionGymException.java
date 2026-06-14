package com.thesis.workout.session.application.exception;

import com.thesis.workout.shared.web.exception.ApiException;
import org.springframework.http.HttpStatus;

/** Raised when a set references equipment the user owns but that lives in a different gym. */
public class EquipmentNotInSessionGymException extends ApiException {

    public EquipmentNotInSessionGymException() {
        super(HttpStatus.BAD_REQUEST, "EQUIPMENT_NOT_IN_SESSION_GYM",
                "That equipment belongs to a different gym than this workout.");
    }
}
