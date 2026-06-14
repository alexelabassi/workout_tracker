package com.thesis.workout.session.application.exception;

import com.thesis.workout.shared.web.exception.ApiException;
import org.springframework.http.HttpStatus;

public class TemplateDayEmptyException extends ApiException {

    public TemplateDayEmptyException() {
        super(HttpStatus.BAD_REQUEST, "TEMPLATE_DAY_EMPTY", "Cannot start a workout from a day with no exercises.");
    }
}
