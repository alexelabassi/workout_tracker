package com.thesis.workout.gym.application.exception;

import com.thesis.workout.shared.web.exception.ApiException;
import org.springframework.http.HttpStatus;

public class EquipmentNameTakenException extends ApiException {

    public EquipmentNameTakenException() {
        super(HttpStatus.CONFLICT, "EQUIPMENT_NAME_TAKEN", "This gym already has equipment with this name.");
    }
}
