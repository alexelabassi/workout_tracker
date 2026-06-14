package com.thesis.workout.session.application.exception;

import com.thesis.workout.shared.web.exception.ApiException;
import org.springframework.http.HttpStatus;

/** Raised when a mutation is attempted on a session that is no longer IN_PROGRESS. */
public class SessionNotActiveException extends ApiException {

    public SessionNotActiveException() {
        super(HttpStatus.CONFLICT, "SESSION_NOT_ACTIVE", "This workout is no longer in progress.");
    }
}
