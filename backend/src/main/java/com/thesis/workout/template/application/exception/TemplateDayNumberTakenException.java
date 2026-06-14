package com.thesis.workout.template.application.exception;

import com.thesis.workout.shared.web.exception.ApiException;
import org.springframework.http.HttpStatus;

public class TemplateDayNumberTakenException extends ApiException {

    public TemplateDayNumberTakenException() {
        super(HttpStatus.CONFLICT, "DAY_NUMBER_TAKEN", "This template already has a day with that number.");
    }
}
