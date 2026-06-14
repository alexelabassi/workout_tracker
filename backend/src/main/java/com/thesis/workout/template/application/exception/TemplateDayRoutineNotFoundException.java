package com.thesis.workout.template.application.exception;

import com.thesis.workout.shared.web.exception.ApiException;
import org.springframework.http.HttpStatus;

public class TemplateDayRoutineNotFoundException extends ApiException {

    public TemplateDayRoutineNotFoundException() {
        super(HttpStatus.NOT_FOUND, "TEMPLATE_DAY_ROUTINE_NOT_FOUND", "Template day routine not found.");
    }
}
