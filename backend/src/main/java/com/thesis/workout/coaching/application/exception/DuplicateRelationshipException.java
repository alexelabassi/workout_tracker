package com.thesis.workout.coaching.application.exception;

import com.thesis.workout.shared.web.exception.ApiException;
import org.springframework.http.HttpStatus;

/** A pending or active relationship already exists for this coach↔client pair. */
public class DuplicateRelationshipException extends ApiException {

    public DuplicateRelationshipException() {
        super(HttpStatus.CONFLICT, "RELATIONSHIP_EXISTS",
                "A pending or active relationship already exists with this client.");
    }
}
