package com.thesis.workout.analytics.web;

import com.thesis.workout.analytics.application.AnalyticsService;
import com.thesis.workout.analytics.web.dto.AnalyticsOverviewResponse;
import com.thesis.workout.auth.infrastructure.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/overview")
    public AnalyticsOverviewResponse overview(@AuthenticationPrincipal UserPrincipal principal) {
        return analyticsService.overview(principal.id());
    }
}
