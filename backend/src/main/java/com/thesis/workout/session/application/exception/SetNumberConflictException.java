package com.thesis.workout.session.application.exception;

import com.thesis.workout.shared.web.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Raised when two concurrent set logs race for the same auto-assigned set number on one session
 * exercise (the DB's UNIQUE (session_exercise_id, set_number) rejects the loser). Surfaced as a
 * 409 so the client can simply retry the log.
 */
public class SetNumberConflictException extends ApiException {

    public SetNumberConflictException() {
        super(HttpStatus.CONFLICT, "SET_NUMBER_CONFLICT", "That set was just logged; please retry.");
    }
}
