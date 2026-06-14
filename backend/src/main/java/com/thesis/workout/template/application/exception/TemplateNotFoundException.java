package com.thesis.workout.template.application.exception;

import com.thesis.workout.shared.web.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a template cannot be found for the current user. Returning 404 for a template
 * owned by someone else avoids leaking its existence (no IDOR signal).
 */
public class TemplateNotFoundException extends ApiException {

    public TemplateNotFoundException() {
        super(HttpStatus.NOT_FOUND, "TEMPLATE_NOT_FOUND", "Template not found.");
    }
}
