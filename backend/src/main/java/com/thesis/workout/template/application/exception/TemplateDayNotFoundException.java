package com.thesis.workout.template.application.exception;

import com.thesis.workout.shared.web.exception.ApiException;
import org.springframework.http.HttpStatus;

public class TemplateDayNotFoundException extends ApiException {

    public TemplateDayNotFoundException() {
        super(HttpStatus.NOT_FOUND, "TEMPLATE_DAY_NOT_FOUND", "Template day not found.");
    }
}
