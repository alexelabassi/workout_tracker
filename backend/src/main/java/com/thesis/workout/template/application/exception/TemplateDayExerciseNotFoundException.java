package com.thesis.workout.template.application.exception;

import com.thesis.workout.shared.web.exception.ApiException;
import org.springframework.http.HttpStatus;

public class TemplateDayExerciseNotFoundException extends ApiException {

    public TemplateDayExerciseNotFoundException() {
        super(HttpStatus.NOT_FOUND, "TEMPLATE_DAY_EXERCISE_NOT_FOUND", "Template day exercise not found.");
    }
}
