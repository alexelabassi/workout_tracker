package com.thesis.workout.coaching.application.exception;

import com.thesis.workout.shared.web.exception.ApiException;
import org.springframework.http.HttpStatus;

/** The email a coach tried to invite does not belong to any registered user (no account is created). */
public class InviteTargetNotFoundException extends ApiException {

    public InviteTargetNotFoundException() {
        super(HttpStatus.NOT_FOUND, "INVITE_TARGET_NOT_FOUND", "No registered user with that email.");
    }
}
