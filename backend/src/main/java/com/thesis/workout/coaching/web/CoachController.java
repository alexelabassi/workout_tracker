package com.thesis.workout.coaching.web;

import com.thesis.workout.analytics.web.dto.AnalyticsOverviewResponse;
import com.thesis.workout.auth.infrastructure.security.UserPrincipal;
import com.thesis.workout.coaching.application.CoachClientService;
import com.thesis.workout.coaching.application.CoachSearchService;
import com.thesis.workout.coaching.application.CoachViewService;
import com.thesis.workout.coaching.web.dto.ClientSummaryResponse;
import com.thesis.workout.coaching.web.dto.InviteRequest;
import com.thesis.workout.history.web.dto.HistoryItemResponse;
import com.thesis.workout.search.application.exception.SearchUnavailableException;
import com.thesis.workout.search.web.dto.SearchResultsResponse;
import com.thesis.workout.search.web.dto.WorkoutSearchItemResponse;
import com.thesis.workout.session.web.dto.WorkoutSessionDetailResponse;
import com.thesis.workout.shared.web.PagedResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Coach-facing, read-only access to a client's data. Locked to ROLE_COACH by the security config
 * ({@code /api/coach/**}); every per-client read additionally passes the ACTIVE-relationship gate
 * (404 otherwise). There are no endpoints that mutate client data.
 */
@RestController
@RequestMapping("/api/coach")
public class CoachController {

    private final CoachClientService coachClientService;
    private final CoachViewService coachViewService;
    private final ObjectProvider<CoachSearchService> coachSearchService;

    public CoachController(CoachClientService coachClientService, CoachViewService coachViewService,
            ObjectProvider<CoachSearchService> coachSearchService) {
        this.coachClientService = coachClientService;
        this.coachViewService = coachViewService;
        this.coachSearchService = coachSearchService;
    }

    @GetMapping("/clients")
    public List<ClientSummaryResponse> clients(@AuthenticationPrincipal UserPrincipal principal) {
        return coachClientService.listActiveClients(principal.id());
    }

    @PostMapping("/clients/invite")
    @ResponseStatus(HttpStatus.CREATED)
    public ClientSummaryResponse invite(@AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody InviteRequest request) {
        return coachClientService.invite(principal.id(), request.clientEmail());
    }

    @DeleteMapping("/clients/{clientId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeClient(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID clientId) {
        coachClientService.revokeClient(principal.id(), clientId);
    }

    @GetMapping("/clients/{clientId}/history")
    public PagedResponse<HistoryItemResponse> clientHistory(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return coachViewService.clientHistory(principal.id(), clientId, page, size);
    }

    @GetMapping("/clients/{clientId}/analytics")
    public AnalyticsOverviewResponse clientAnalytics(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID clientId) {
        return coachViewService.clientAnalytics(principal.id(), clientId);
    }

    @GetMapping("/clients/{clientId}/sessions/{sessionId}")
    public WorkoutSessionDetailResponse clientSession(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID clientId, @PathVariable UUID sessionId) {
        return coachViewService.clientSession(principal.id(), clientId, sessionId);
    }

    /**
     * Full-text search over a client's workout history (relationship-gated). 503 if search is
     * disabled/unavailable so the UI can fall back to the paged history list.
     */
    @GetMapping("/clients/{clientId}/search/workouts")
    public SearchResultsResponse<WorkoutSearchItemResponse> searchClientWorkouts(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID clientId,
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
        CoachSearchService service = coachSearchService.getIfAvailable();
        if (service == null) {
            throw new SearchUnavailableException();
        }
        return service.searchClientWorkouts(principal.id(), clientId, q, status, dateFrom, dateTo, muscleGroup,
                exercise, gym, equipment, minVolume, maxVolume, minDuration, maxDuration, page, size);
    }
}
