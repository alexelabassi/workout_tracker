package com.thesis.workout.coaching.application;

import com.thesis.workout.search.application.WorkoutSearchService;
import com.thesis.workout.search.web.dto.SearchResultsResponse;
import com.thesis.workout.search.web.dto.WorkoutSearchItemResponse;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Lets a coach full-text search a client's workout history. This is the one place the workout search
 * runs with an owner id other than the caller's — and it is safe precisely because it first passes
 * the {@link CoachAccess} gate (an ACTIVE relationship, else 404). After the gate it reuses the same
 * {@link WorkoutSearchService} with the <em>client's</em> id, so the search (and its defense-in-depth
 * PostgreSQL re-validation) is scoped to that client. Only present when search is enabled.
 */
@Service
@ConditionalOnProperty(name = "app.search.enabled", havingValue = "true")
public class CoachSearchService {

    private final CoachAccess coachAccess;
    private final WorkoutSearchService workoutSearchService;

    public CoachSearchService(CoachAccess coachAccess, WorkoutSearchService workoutSearchService) {
        this.coachAccess = coachAccess;
        this.workoutSearchService = workoutSearchService;
    }

    public SearchResultsResponse<WorkoutSearchItemResponse> searchClientWorkouts(
            UUID coachUserId, UUID clientUserId, String q, String status, String dateFrom, String dateTo,
            String muscleGroup, String exercise, String gym, String equipment, Double minVolume, Double maxVolume,
            Long minDuration, Long maxDuration, int page, int size) {
        coachAccess.requireActiveClient(coachUserId, clientUserId);
        return workoutSearchService.search(clientUserId, q, status, dateFrom, dateTo, muscleGroup, exercise, gym,
                equipment, minVolume, maxVolume, minDuration, maxDuration, page, size);
    }
}
