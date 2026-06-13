package com.thesis.workout.shared.web.exception;

import org.springframework.http.HttpStatus;

/**
 * Base type for domain/application errors that map cleanly onto an HTTP status and a
 * stable machine-readable error code. {@code GlobalExceptionHandler} renders these as the
 * shared {@code ApiError} envelope.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    protected ApiException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
