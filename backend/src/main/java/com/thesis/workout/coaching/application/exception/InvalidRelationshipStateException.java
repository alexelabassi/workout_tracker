package com.thesis.workout.coaching.application.exception;

import com.thesis.workout.shared.web.exception.ApiException;
import org.springframework.http.HttpStatus;

/** A lifecycle action is invalid for the relationship's current status (e.g. accepting a non-pending invite). */
public class InvalidRelationshipStateException extends ApiException {

    public InvalidRelationshipStateException(String message) {
        super(HttpStatus.CONFLICT, "INVALID_RELATIONSHIP_STATE", message);
    }
}
