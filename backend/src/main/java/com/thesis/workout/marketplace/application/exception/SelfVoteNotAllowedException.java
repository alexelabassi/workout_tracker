package com.thesis.workout.marketplace.application.exception;

import com.thesis.workout.shared.web.exception.ApiException;
import org.springframework.http.HttpStatus;

public class SelfVoteNotAllowedException extends ApiException {

    public SelfVoteNotAllowedException() {
        super(HttpStatus.BAD_REQUEST, "CANNOT_VOTE_OWN_TEMPLATE", "You cannot vote on your own template.");
    }
}
