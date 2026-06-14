package com.thesis.workout.gym.application.exception;

import com.thesis.workout.shared.web.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when equipment cannot be found for the current user (or its parent gym is gone).
 * Returning 404 for another user's equipment avoids leaking its existence (no IDOR signal).
 */
public class EquipmentNotFoundException extends ApiException {

    public EquipmentNotFoundException() {
        super(HttpStatus.NOT_FOUND, "EQUIPMENT_NOT_FOUND", "Equipment not found.");
    }
}
