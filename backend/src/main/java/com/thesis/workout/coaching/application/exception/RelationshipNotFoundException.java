package com.thesis.workout.coaching.application.exception;

import com.thesis.workout.shared.web.exception.ApiException;
import org.springframework.http.HttpStatus;

/** A relationship the caller tried to act on does not exist or is not theirs (IDOR-safe 404). */
public class RelationshipNotFoundException extends ApiException {

    public RelationshipNotFoundException() {
        super(HttpStatus.NOT_FOUND, "RELATIONSHIP_NOT_FOUND", "Relationship not found.");
    }
}
