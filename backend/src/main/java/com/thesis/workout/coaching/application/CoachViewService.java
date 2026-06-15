package com.thesis.workout.coaching.application;

import com.thesis.workout.analytics.application.AnalyticsService;
import com.thesis.workout.analytics.web.dto.AnalyticsOverviewResponse;
import com.thesis.workout.history.application.HistoryService;
import com.thesis.workout.history.web.dto.HistoryItemResponse;
import com.thesis.workout.session.application.WorkoutSessionService;
import com.thesis.workout.session.web.dto.WorkoutSessionDetailResponse;
import com.thesis.workout.shared.web.PagedResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Read-only coach views of a client's data. Every method first passes the {@link CoachAccess} gate
 * (ACTIVE relationship required, else 404) and then delegates to the existing owner-id-scoped
 * services with the <em>client's</em> id — so a coach sees exactly what the client sees, with no new
 * query-security code and no way to mutate. There are deliberately no write methods here.
 */
@Service
public class CoachViewService {

    private final CoachAccess coachAccess;
    private final HistoryService historyService;
    private final AnalyticsService analyticsService;
    private final WorkoutSessionService workoutSessionService;

    public CoachViewService(CoachAccess coachAccess, HistoryService historyService,
            AnalyticsService analyticsService, WorkoutSessionService workoutSessionService) {
        this.coachAccess = coachAccess;
        this.historyService = historyService;
        this.analyticsService = analyticsService;
        this.workoutSessionService = workoutSessionService;
    }

    public PagedResponse<HistoryItemResponse> clientHistory(UUID coachUserId, UUID clientUserId, int page, int size) {
        coachAccess.requireActiveClient(coachUserId, clientUserId);
        return historyService.list(clientUserId, page, size);
    }

    public AnalyticsOverviewResponse clientAnalytics(UUID coachUserId, UUID clientUserId) {
        coachAccess.requireActiveClient(coachUserId, clientUserId);
        return analyticsService.overview(clientUserId);
    }

    public WorkoutSessionDetailResponse clientSession(UUID coachUserId, UUID clientUserId, UUID sessionId) {
        coachAccess.requireActiveClient(coachUserId, clientUserId);
        // requireOwnedSession(clientUserId, sessionId) inside also guarantees the session is the client's.
        return workoutSessionService.get(clientUserId, sessionId);
    }
}
