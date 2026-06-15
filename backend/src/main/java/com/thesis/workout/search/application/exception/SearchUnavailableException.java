package com.thesis.workout.search.application.exception;

import com.thesis.workout.shared.web.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Raised when a search request cannot be served — either the feature is disabled
 * ({@code app.search.enabled=false}) or OpenSearch is unreachable. Maps to 503 so clients can fall
 * back to the authoritative SQL browse/list rather than treating it as a hard error.
 */
public class SearchUnavailableException extends ApiException {

    public SearchUnavailableException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "SEARCH_UNAVAILABLE",
                "Search is temporarily unavailable; showing the standard list instead.");
    }
}
