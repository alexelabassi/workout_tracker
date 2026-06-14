package com.thesis.workout.marketplace.application.exception;

import com.thesis.workout.shared.web.exception.ApiException;
import org.springframework.http.HttpStatus;

public class TemplateNotPublishableException extends ApiException {

    public TemplateNotPublishableException() {
        super(HttpStatus.BAD_REQUEST, "TEMPLATE_NOT_PUBLISHABLE",
                "A template needs at least one day with at least one exercise before it can be published.");
    }
}
