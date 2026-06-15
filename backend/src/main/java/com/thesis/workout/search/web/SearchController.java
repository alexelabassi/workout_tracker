package com.thesis.workout.search.web;

import com.thesis.workout.auth.infrastructure.security.UserPrincipal;
import com.thesis.workout.search.application.TemplateSearchService;
import com.thesis.workout.search.application.WorkoutSearchService;
import com.thesis.workout.search.application.exception.SearchUnavailableException;
import com.thesis.workout.search.web.dto.SearchResultsResponse;
import com.thesis.workout.search.web.dto.TemplateSearchItemResponse;
import com.thesis.workout.search.web.dto.WorkoutSearchItemResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * OpenSearch-powered search endpoints. The services are injected through {@link ObjectProvider} so
 * this controller exists even when {@code app.search.enabled=false}; in that case (or if OpenSearch
 * is down) it returns 503 and the SPA falls back to the SQL browse/list. Both endpoints derive the
 * acting user from the verified JWT — the workout scope is always that user's own history.
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final ObjectProvider<TemplateSearchService> templateSearch;
    private final ObjectProvider<WorkoutSearchService> workoutSearch;

    public SearchController(ObjectProvider<TemplateSearchService> templateSearch,
            ObjectProvider<WorkoutSearchService> workoutSearch) {
        this.templateSearch = templateSearch;
        this.workoutSearch = workoutSearch;
    }

    @GetMapping("/templates")
    public SearchResultsResponse<TemplateSearchItemResponse> templates(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "my") String scope,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String splitType,
            @RequestParam(required = false) Integer daysPerWeek,
            @RequestParam(required = false) String muscleGroup,
            @RequestParam(required = false) String analysisCategory,
            @RequestParam(required = false) Integer minScore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        TemplateSearchService service = templateSearch.getIfAvailable();
        if (service == null) {
            throw new SearchUnavailableException();
        }
        return service.search(principal.id(), scope, q, difficulty, splitType, daysPerWeek, muscleGroup,
                analysisCategory, minScore, page, size);
    }

    @GetMapping("/workouts")
    public SearchResultsResponse<WorkoutSearchItemResponse> workouts(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String muscleGroup,
            @RequestParam(required = false) String exercise,
            @RequestParam(required = false) String gym,
            @RequestParam(required = false) String equipment,
            @RequestParam(required = false) Double minVolume,
            @RequestParam(required = false) Double maxVolume,
            @RequestParam(required = false) Long minDuration,
            @RequestParam(required = false) Long maxDuration,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        WorkoutSearchService service = workoutSearch.getIfAvailable();
        if (service == null) {
            throw new SearchUnavailableException();
        }
        return service.search(principal.id(), q, status, dateFrom, dateTo, muscleGroup, exercise, gym, equipment,
                minVolume, maxVolume, minDuration, maxDuration, page, size);
    }
}
