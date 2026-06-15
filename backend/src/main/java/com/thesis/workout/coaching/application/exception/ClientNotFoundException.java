package com.thesis.workout.coaching.application.exception;

import com.thesis.workout.shared.web.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Thrown by the coach access gate when the caller has no ACTIVE relationship with the target
 * client. Deliberately a 404 (not 403) so the existence of another user's data is never revealed —
 * consistent with the project's IDOR-safe access pattern.
 */
public class ClientNotFoundException extends ApiException {

    public ClientNotFoundException() {
        super(HttpStatus.NOT_FOUND, "CLIENT_NOT_FOUND", "Client not found.");
    }
}
