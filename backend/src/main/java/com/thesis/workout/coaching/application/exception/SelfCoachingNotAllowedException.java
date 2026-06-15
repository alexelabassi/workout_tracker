package com.thesis.workout.coaching.application.exception;

import com.thesis.workout.shared.web.exception.ApiException;
import org.springframework.http.HttpStatus;

/** A coach cannot invite themselves as their own client (also blocked by a DB CHECK). */
public class SelfCoachingNotAllowedException extends ApiException {

    public SelfCoachingNotAllowedException() {
        super(HttpStatus.BAD_REQUEST, "CANNOT_COACH_SELF", "You cannot add yourself as a client.");
    }
}
